package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import graphql.schema.DataFetchingEnvironment;
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
                                          Descriptors.FileDescriptor fileDescriptor,
                                          String serviceName,
                                          String serverName) {
        var method = methodDescriptorResolver.resolve(environment, fileDescriptor, serviceName);
        var inputTypeDescriptor = descriptorResolver.resolve(method.getInputType(), fileDescriptor);
        var outputTypeDescriptor = descriptorResolver.resolve(method.getOutputType(), fileDescriptor);
        var methodDescriptor = methodDescriptorBuilder.build(serviceName, method.getName(), inputTypeDescriptor, outputTypeDescriptor);
        var message = requestBuilder.build(inputTypeDescriptor, environment.getArgument("request"), environment.getSelectionSet());
        var dynamicGrpcClient = dynamicGrpcClients.get(serverName);
        return dynamicGrpcClient.callUnary(message, methodDescriptor);
    }
}
