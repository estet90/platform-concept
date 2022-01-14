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
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClient;
import ru.craftysoft.platform.gateway.service.client.grpc.ServerReflectionClient;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

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
                                var fileDescriptor = extractFileDescriptorProto(descriptorSet);
                                var serviceDescriptor = extractServiceDescriptorProto(fileDescriptor);
                                var methodDescriptor = extractMethodDescriptorProto(environment, serviceDescriptor);
                                var inputTypeNameParts = methodDescriptor.getInputType().split("\\.");
                                var inputTypeName = inputTypeNameParts[inputTypeNameParts.length - 1];
                                var outputTypeNameParts = methodDescriptor.getOutputType().split("\\.");
                                var outputTypeName = outputTypeNameParts[outputTypeNameParts.length - 1];
                                var inputTypeDescriptor = resolveTypeDescriptor(fileDescriptor, inputTypeName);
                                var outputTypeDescriptor = resolveTypeDescriptor(fileDescriptor, outputTypeName);
                                var requestBody = ofNullable(environment.getArgument("request"))
                                        .map(JsonObject::mapFrom)
                                        .map(JsonObject::encode)
                                        .orElseThrow();
                                var builder = resolveBuilder(inputTypeDescriptor, requestBody);
                                var dynamicGrpcClient = dynamicGrpcClients.get("grpc-server");
                                return dynamicGrpcClient.callUnary(builder.build(), CallOptions.DEFAULT, serviceName, methodDescriptor.getName(), inputTypeDescriptor, outputTypeDescriptor)
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

    private Descriptors.Descriptor resolveTypeDescriptor(DescriptorProtos.FileDescriptorProto fileDescriptor, String typeName) {
        try {
            return Descriptors.FileDescriptor.buildFrom(fileDescriptor, new Descriptors.FileDescriptor[0]).findMessageTypeByName(typeName);
        } catch (Descriptors.DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private DescriptorProtos.MethodDescriptorProto extractMethodDescriptorProto(DataFetchingEnvironment environment, DescriptorProtos.ServiceDescriptorProto serviceDescriptor) {
        return serviceDescriptor.getMethodList()
                .stream()
                .filter(methodDescriptorProto -> methodDescriptorProto.getName().equals(environment.getFieldDefinition().getName()))
                .findFirst()
                .orElseThrow();
    }

    private DescriptorProtos.ServiceDescriptorProto extractServiceDescriptorProto(DescriptorProtos.FileDescriptorProto fileDescriptor) {
        return (DescriptorProtos.ServiceDescriptorProto) fileDescriptor.getAllFields().entrySet().stream()
                .filter(entry -> entry.getKey().getFullName().equals("google.protobuf.FileDescriptorProto.service"))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(List.class::cast)
                .map(l -> l.get(0))
                .orElseThrow();
    }

    private DescriptorProtos.FileDescriptorProto extractFileDescriptorProto(DescriptorProtos.FileDescriptorSet descriptorSet) {
        return (DescriptorProtos.FileDescriptorProto) descriptorSet.getAllFields().values().stream()
                .findFirst()
                .map(List.class::cast)
                .orElseThrow()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private DynamicMessage.Builder resolveBuilder(Descriptors.Descriptor inputTypeDescriptor, String requestBody) {
        try {
            var builder = DynamicMessage.newBuilder(inputTypeDescriptor);
            JsonFormat.parser().merge(requestBody, builder);
            return builder;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

}
