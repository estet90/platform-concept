package ru.craftysoft.platform.gateway;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.CallOptions;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClient;
import ru.craftysoft.platform.gateway.service.client.grpc.ServerReflectionClient;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class Resolver {

    private final Map<String, DynamicGrpcClient> dynamicGrpcClients;
    private final Map<String, ServerReflectionClient> reflectionClients;

    public Resolver(Map<String, DynamicGrpcClient> dynamicGrpcClients, Map<String, ServerReflectionClient> reflectionClients) {
        this.dynamicGrpcClients = dynamicGrpcClients;
        this.reflectionClients = reflectionClients;
    }

    public Future<String> handle(DataFetchingEnvironment environment) {
        var serverReflectionClient = reflectionClients.get("grpc-server");
        String result = null;
        try {
            var serviceName = serverReflectionClient.listServices().get().stream()
                    .filter(s -> !"grpc.reflection.v1alpha.ServerReflection".equals(s))
                    .findFirst()
                    .orElseThrow();
            var descriptorSet = serverReflectionClient.lookupService(serviceName).get();
            var fileDescriptor = (DescriptorProtos.FileDescriptorProto) descriptorSet.getAllFields().values().stream()
                    .findFirst()
                    .map(List.class::cast)
                    .orElseThrow()
                    .stream()
                    .findFirst()
                    .map(DescriptorProtos.FileDescriptorProto.class::cast)
                    .orElseThrow();
            var serviceDescriptor = fileDescriptor.getAllFields().entrySet().stream()
                    .filter(entry -> entry.getKey().getFullName().equals("google.protobuf.FileDescriptorProto.service"))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .map(List.class::cast)
                    .map(l -> l.get(0))
                    .map(DescriptorProtos.ServiceDescriptorProto.class::cast)
                    .orElseThrow();
            var methodDescriptor = serviceDescriptor.getMethodList()
                    .stream()
                    .filter(methodDescriptorProto -> methodDescriptorProto.getName().equals(environment.getFieldDefinition().getName()))
                    .findFirst()
                    .orElseThrow();
            var inputTypeNameParts = methodDescriptor.getInputType().split("\\.");
            var inputTypeName = inputTypeNameParts[inputTypeNameParts.length - 1];
            var outputTypeNameParts = methodDescriptor.getOutputType().split("\\.");
            var outputTypeName = outputTypeNameParts[outputTypeNameParts.length - 1];
            var inputTypeDescriptor = (DescriptorProtos.DescriptorProto) fileDescriptor.getAllFields().entrySet().stream()
                    .filter(entry -> entry.getKey().getFullName().equals("google.protobuf.FileDescriptorProto.message_type"))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .map(List.class::cast)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(DescriptorProtos.DescriptorProto.class::cast)
                    .filter(descriptor -> ((DescriptorProtos.DescriptorProto) descriptor).getName().equals(inputTypeName))
                    .findFirst()
                    .orElseThrow();
            var outputTypeDescriptor = (DescriptorProtos.DescriptorProto) fileDescriptor.getAllFields().entrySet().stream()
                    .filter(entry -> entry.getKey().getFullName().equals("google.protobuf.FileDescriptorProto.message_type"))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .map(List.class::cast)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(DescriptorProtos.DescriptorProto.class::cast)
                    .filter(descriptor -> ((DescriptorProtos.DescriptorProto) descriptor).getName().equals(inputTypeName))
                    .findFirst()
                    .orElseThrow();
            var dynamicGrpcClient = dynamicGrpcClients.get("grpc-server");
            var requestBody = ofNullable(environment.getArgument("request"))
                    .map(JsonObject::mapFrom)
                    .map(JsonObject::encode)
                    .orElseThrow();
            var builder = DynamicMessage.newBuilder(Descriptors.FileDescriptor.buildFrom(fileDescriptor, new Descriptors.FileDescriptor[0]).findMessageTypeByName(inputTypeName));
            JsonFormat.parser().merge(requestBody, builder);
            var wrapper = new Object() {
                DynamicMessage dynamicMessage;
            };
            dynamicGrpcClient.callUnary(builder.build(), new StreamObserver<>() {
                private DynamicMessage dynamicMessage;

                @Override
                public void onNext(DynamicMessage dynamicMessage) {
                    System.out.println("message bliat: " + dynamicMessage);
                    this.dynamicMessage = dynamicMessage;
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    throw new RuntimeException(throwable);
                }

                @Override
                public void onCompleted() {
                    wrapper.dynamicMessage = dynamicMessage;
                }
            }, CallOptions.DEFAULT, serviceName, methodDescriptor.getName(), inputTypeDescriptor.getDescriptorForType(), outputTypeDescriptor.getDescriptorForType()).get();
            System.out.println("message nahuy: " + wrapper.dynamicMessage);
            result = JsonFormat.printer().print(wrapper.dynamicMessage);
            System.out.println(result);
        } catch (InterruptedException | ExecutionException | InvalidProtocolBufferException | Descriptors.DescriptorValidationException e) {
            e.printStackTrace();
        }
        return Future.succeededFuture(result);
    }

}
