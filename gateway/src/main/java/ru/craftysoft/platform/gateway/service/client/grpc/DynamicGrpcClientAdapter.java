package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.vertx.core.Future;
import ru.craftysoft.platform.gateway.builder.dynamic.*;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class DynamicGrpcClientAdapter {

    private final Map<String, DynamicGrpcClient> dynamicGrpcClients;
    private final DynamicMessageBuilder requestBuilder;
    private final DynamicMessageMethodDescriptorBuilder methodDescriptorBuilder;
    private final FileDescriptorResolver fileDescriptorResolver;
    private final DescriptorResolver descriptorResolver;
    private final MethodDescriptorResolver methodDescriptorResolver;

    public DynamicGrpcClientAdapter(Map<String, DynamicGrpcClient> dynamicGrpcClients,
                                    DynamicMessageBuilder requestBuilder,
                                    DynamicMessageMethodDescriptorBuilder methodDescriptorBuilder,
                                    FileDescriptorResolver fileDescriptorResolver,
                                    DescriptorResolver descriptorResolver,
                                    MethodDescriptorResolver methodDescriptorResolver) {
        this.dynamicGrpcClients = dynamicGrpcClients;
        this.requestBuilder = requestBuilder;
        this.methodDescriptorBuilder = methodDescriptorBuilder;
        this.fileDescriptorResolver = fileDescriptorResolver;
        this.descriptorResolver = descriptorResolver;
        this.methodDescriptorResolver = methodDescriptorResolver;
    }

    public Future<Message> processRequest(DataFetchingEnvironment environment,
                                          ServerReflectionResponse serverReflectionResponse,
                                          String serviceName,
                                          String serverName) {
        var fileDescriptor = fileDescriptorResolver.resolve(serverReflectionResponse);
        var method = methodDescriptorResolver.resolve(environment, fileDescriptor, serviceName);
        var inputTypeDescriptor = descriptorResolver.resolve(method.getInputType(), fileDescriptor);
        var outputTypeDescriptor = descriptorResolver.resolve(method.getOutputType(), fileDescriptor);
        var builder = DynamicMessage.newBuilder(inputTypeDescriptor);
        var message = (DynamicMessage) requestBuilder.build(inputTypeDescriptor, builder, environment.getArgument("request"));
        var methodDescriptor = methodDescriptorBuilder.build(serviceName, method.getName(), inputTypeDescriptor, outputTypeDescriptor);
        var dynamicGrpcClient = dynamicGrpcClients.get(serverName);
        return dynamicGrpcClient.callUnary(message, methodDescriptor);
    }
}
