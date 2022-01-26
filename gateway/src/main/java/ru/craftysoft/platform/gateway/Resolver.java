package ru.craftysoft.platform.gateway;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.CallOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
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

    public Resolver(Map<String, DynamicGrpcClient> dynamicGrpcClients, Map<String, ServerReflectionClient> reflectionClients) {
        this.dynamicGrpcClients = dynamicGrpcClients;
        this.reflectionClients = reflectionClients;
        this.ignoredServiceNames = Set.of("grpc.health.v1.Health", "grpc.reflection.v1alpha.ServerReflection");
    }

    public Future<Map<String, Object>> handle(DataFetchingEnvironment environment) {
        var serverReflectionClient = reflectionClients.get("grpc-server");
        var future = serverReflectionClient.listServices()
                .thenCompose(services -> {
                    System.out.println(services);
                    var serviceName = services.stream()
                            .filter(Predicate.not(ignoredServiceNames::contains))
                            .findFirst()
                            .orElseThrow();
                    return serverReflectionClient.lookupService(serviceName)
                            .thenCompose(descriptorSet -> {
                                var fileDescriptorsWithDependencies = descriptorSet.getFileList().stream()
                                        .collect(Collectors.toMap(f -> f, f -> (Set<String>) new HashSet<>(f.getDependencyList())));
                                var wrapper = new FileDescriptorHolder();
                                resolveFullFileDescriptor(fileDescriptorsWithDependencies, new HashMap<>(), new HashSet<>(), wrapper);
                                var fileDescriptorProto = wrapper.fileDescriptor.toProto();
                                var serviceDescriptor = extractServiceDescriptorProto(fileDescriptorProto);
                                var methodDescriptor = extractMethodDescriptorProto(environment, serviceDescriptor);
                                var inputTypeNameParts = methodDescriptor.getInputType().split("\\.");
                                var inputTypeName = inputTypeNameParts[inputTypeNameParts.length - 1];
                                var outputTypeNameParts = methodDescriptor.getOutputType().split("\\.");
                                var outputTypeName = outputTypeNameParts[outputTypeNameParts.length - 1];
                                var inputTypeDescriptor = resolveTypeDescriptor(fileDescriptorProto, wrapper.fileDescriptor, inputTypeName);
                                var outputTypeDescriptor = resolveTypeDescriptor(fileDescriptorProto, wrapper.fileDescriptor, outputTypeName);
                                var builder = DynamicMessage.newBuilder(inputTypeDescriptor);
                                var gqlInputConverter = new GqlInputConverter.Builder().add(inputTypeDescriptor.getFile()).build();
                                var message = (DynamicMessage) gqlInputConverter.createProtoBuf(inputTypeDescriptor, builder, environment.getArgument("request"));
                                var dynamicGrpcClient = dynamicGrpcClients.get("grpc-server");
                                return dynamicGrpcClient.callUnary(message, CallOptions.DEFAULT, serviceName, methodDescriptor.getName(), inputTypeDescriptor, outputTypeDescriptor)
                                        .thenApply(dynamicMessage -> {
                                            String json;
                                            try {
                                                json = JsonFormat.printer().print(dynamicMessage);
                                            } catch (InvalidProtocolBufferException e) {
                                                throw new RuntimeException(e);
                                            }
                                            return new JsonObject(json).getMap();
                                        });
                            });
                });
        return Future.fromCompletionStage(future);
    }

    private Descriptors.Descriptor resolveTypeDescriptor(DescriptorProtos.FileDescriptorProto fileDescriptorProto,
                                                         Descriptors.FileDescriptor fileDescriptor,
                                                         String typeName) {
        try {
            return Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, fileDescriptor.getDependencies().toArray(new Descriptors.FileDescriptor[]{}))
                    .findMessageTypeByName(typeName);
        } catch (Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private void resolveFullFileDescriptor(Map<DescriptorProtos.FileDescriptorProto, Set<String>> fileDescriptorsWithDependencies,
                                           Map<String, Descriptors.FileDescriptor> completedFileDescriptors,
                                           Set<String> dependencies,
                                           FileDescriptorHolder result) {
        if (dependencies.isEmpty()) {
            fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
                if (entry.getValue().isEmpty()) {
                    var descriptorProto = entry.getKey();
                    try {
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
            var completedFileDescriptorsNames = completedFileDescriptors.keySet();
            fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
                if (completedFileDescriptorsNames.containsAll(entry.getValue())) {
                    var descriptorProto = entry.getKey();
                    var currentDependencies = entry.getValue().stream()
                            .map(completedFileDescriptors::get)
                            .toArray(Descriptors.FileDescriptor[]::new);
                    try {
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
                        fileDescriptorsWithDependencies.entrySet().iterator().next().getValue(),
                        result
                );
            }
        } else {
            fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
                if (dependencies.containsAll(entry.getValue())) {
                    var descriptorProto = entry.getKey();
                    var currentDependencies = entry.getValue().stream()
                            .map(completedFileDescriptors::get)
                            .toArray(Descriptors.FileDescriptor[]::new);
                    try {
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
                        fileDescriptorsWithDependencies.entrySet().iterator().next().getValue(),
                        result
                );
            }
        }
    }

    private DescriptorProtos.MethodDescriptorProto extractMethodDescriptorProto(DataFetchingEnvironment environment, DescriptorProtos.ServiceDescriptorProto serviceDescriptor) {
        return serviceDescriptor.getMethodList()
                .stream()
                .filter(methodDescriptorProto -> methodDescriptorProto.getName().equals(environment.getFieldDefinition().getName()))
                .findFirst()
                .orElseThrow();
    }

    private DescriptorProtos.ServiceDescriptorProto extractServiceDescriptorProto(DescriptorProtos.FileDescriptorProto fileDescriptorProto) {
        return (DescriptorProtos.ServiceDescriptorProto) fileDescriptorProto.getAllFields().entrySet().stream()
                .filter(entry -> entry.getKey().getFullName().equals("google.protobuf.FileDescriptorProto.service"))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(List.class::cast)
                .map(l -> l.get(0))
                .orElseThrow();
    }

    private static class FileDescriptorHolder {
        private Descriptors.FileDescriptor fileDescriptor;
    }

}
