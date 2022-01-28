package ru.craftysoft.platform.gateway;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.CallOptions;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.craftysoft.platform.gateway.builder.ResponseBuilder;
import ru.craftysoft.platform.gateway.configuration.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.rejoiner.GqlInputConverter;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClient;
import ru.craftysoft.platform.gateway.service.client.grpc.ServerReflectionClient;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class Resolver {

    private final Map<String, DynamicGrpcClient> dynamicGrpcClients;
    private final Map<String, ServerReflectionClient> reflectionClients;
    private final Set<String> ignoredServiceNames;
    private final GrpcClientConfigurationMap configurationMap;
    private final ResponseBuilder responseBuilder;

    private static final Logger logger = LoggerFactory.getLogger(Resolver.class);

    public Resolver(Map<String, DynamicGrpcClient> dynamicGrpcClients,
                    Map<String, ServerReflectionClient> reflectionClients,
                    GrpcClientConfigurationMap configurationMap,
                    ResponseBuilder responseBuilder) {
        this.dynamicGrpcClients = dynamicGrpcClients;
        this.reflectionClients = reflectionClients;
        this.configurationMap = configurationMap;
        this.responseBuilder = responseBuilder;
        this.ignoredServiceNames = Set.of("grpc.health.v1.Health", "grpc.reflection.v1alpha.ServerReflection");
    }

    public Future<Map<String, Object>> handle(DataFetchingEnvironment environment) {
        var serverReflectionClient = reflectionClients.get("grpc-server");
        var future = serverReflectionClient.listServices()
                .thenCompose(services -> {
                    logger.info("services: {}", services);
                    var serviceName = services.stream()
                            .filter(Predicate.not(ignoredServiceNames::contains))
                            .findFirst()
                            .orElseThrow();
                    return serverReflectionClient.lookupService(serviceName)
                            .thenCompose(descriptorSet -> {
                                var fileDescriptorsWithDependencies = descriptorSet.getFileList().stream()
                                        .collect(Collectors.toMap(f -> f, f -> (Set<String>) new HashSet<>(f.getDependencyList())));
                                var wrapper = new FileDescriptorHolder();
                                resolveFullFileDescriptor(fileDescriptorsWithDependencies, new HashMap<>(), wrapper);
                                var fileDescriptor = wrapper.fileDescriptor;
                                var method = resolveMethod(environment, fileDescriptor);
                                var inputTypeNameParts = method.getInputType().getName().split("\\.");
                                var inputTypeName = inputTypeNameParts[inputTypeNameParts.length - 1];
                                var outputTypeNameParts = method.getOutputType().getName().split("\\.");
                                var outputTypeName = outputTypeNameParts[outputTypeNameParts.length - 1];
                                var inputTypeDescriptor = fileDescriptor.findMessageTypeByName(inputTypeName);
                                var outputTypeDescriptor = fileDescriptor.findMessageTypeByName(outputTypeName);
                                var builder = DynamicMessage.newBuilder(inputTypeDescriptor);
                                var gqlInputConverter = new GqlInputConverter.Builder()
                                        .add(inputTypeDescriptor.getFile())
                                        .build();
                                var message = (DynamicMessage) gqlInputConverter.createProtoBuf(inputTypeDescriptor, builder, environment.getArgument("request"));
                                var dynamicGrpcClient = dynamicGrpcClients.get("grpc-server");
                                return dynamicGrpcClient.callUnary(message, CallOptions.DEFAULT, serviceName, method.getName(), inputTypeDescriptor, outputTypeDescriptor)
                                        .thenApply(responseBuilder::build);
                            });
                });
        return Future.fromCompletionStage(future);
    }

    private Descriptors.MethodDescriptor resolveMethod(DataFetchingEnvironment environment, Descriptors.FileDescriptor fileDescriptor) {
        return fileDescriptor.getServices().stream()
                .filter(service -> configurationMap.servers().get("grpc-server").serviceName().equals(service.getFullName()))
                .findFirst()
                .map(Descriptors.ServiceDescriptor::getMethods)
                .stream()
                .flatMap(Collection::stream)
                .filter(m -> environment.getFieldDefinition().getName().equals(m.getName()))
                .findFirst()
                .orElseThrow();
    }

    private void resolveFullFileDescriptor(Map<DescriptorProtos.FileDescriptorProto, Set<String>> fileDescriptorsWithDependencies,
                                           Map<String, Descriptors.FileDescriptor> completedFileDescriptors,
                                           FileDescriptorHolder result) {
        if (completedFileDescriptors.isEmpty()) {
            fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
                if (entry.getValue().isEmpty()) {
                    var descriptorProto = entry.getKey();
                    try {
                        logger.info("added descriptor '{}' without dependencies", descriptorProto.getName());
                        var addedFileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorProto, new Descriptors.FileDescriptor[]{});
                        completedFileDescriptors.put(descriptorProto.getName(), addedFileDescriptor);
                        result.fileDescriptor = addedFileDescriptor;
                        return true;
                    } catch (Descriptors.DescriptorValidationException e) {
                        throw new RuntimeException(e);
                    }
                }
                return false;
            });
        }
        var completedFileDescriptorsNames = completedFileDescriptors.keySet();
        fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
            if (completedFileDescriptorsNames.containsAll(entry.getValue())) {
                var descriptorProto = entry.getKey();
                var currentDependencies = entry.getValue().stream()
                        .map(completedFileDescriptors::get)
                        .toArray(Descriptors.FileDescriptor[]::new);
                try {
                    logger.info("added descriptor '{}' with dependencies '{}'", descriptorProto.getName(), entry.getValue());
                    var addedFileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorProto, currentDependencies);
                    completedFileDescriptors.put(descriptorProto.getName(), addedFileDescriptor);
                    result.fileDescriptor = addedFileDescriptor;
                    return true;
                } catch (Descriptors.DescriptorValidationException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        });
        if (!fileDescriptorsWithDependencies.isEmpty()) {
            resolveFullFileDescriptor(
                    fileDescriptorsWithDependencies,
                    completedFileDescriptors,
                    result
            );
        }
    }

    private static class FileDescriptorHolder {
        private Descriptors.FileDescriptor fileDescriptor;
    }

}
