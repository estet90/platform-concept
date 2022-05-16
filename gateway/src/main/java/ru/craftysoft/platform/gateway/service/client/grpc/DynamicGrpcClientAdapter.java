package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.smallrye.mutiny.Uni;
import ru.craftysoft.platform.gateway.builder.dynamic.DescriptorResolver;
import ru.craftysoft.platform.gateway.builder.dynamic.DynamicMessageBuilder;
import ru.craftysoft.platform.gateway.builder.dynamic.DynamicMessageMethodDescriptorBuilder;
import ru.craftysoft.platform.gateway.builder.dynamic.MethodDescriptorResolver;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class DynamicGrpcClientAdapter {

    private final Map<String, DynamicGrpcClient> dynamicGrpcClients;
    private final DynamicMessageBuilder requestBuilder;
    private final DynamicMessageMethodDescriptorBuilder methodDescriptorBuilder;
    private final DescriptorResolver descriptorResolver;
    private final MethodDescriptorResolver methodDescriptorResolver;

    public DynamicGrpcClientAdapter(Map<String, DynamicGrpcClient> dynamicGrpcClients,
                                    DynamicMessageBuilder requestBuilder,
                                    DynamicMessageMethodDescriptorBuilder methodDescriptorBuilder,
                                    DescriptorResolver descriptorResolver,
                                    MethodDescriptorResolver methodDescriptorResolver) {
        this.dynamicGrpcClients = dynamicGrpcClients;
        this.requestBuilder = requestBuilder;
        this.methodDescriptorBuilder = methodDescriptorBuilder;
        this.descriptorResolver = descriptorResolver;
        this.methodDescriptorResolver = methodDescriptorResolver;
    }

    public Uni<DynamicMessage> processRequest(String methodName,
                                              Map<String, Object> request,
                                              DataFetchingFieldSelectionSet selectionSet,
                                              Descriptors.FileDescriptor fileDescriptor,
                                              String serverName,
                                              String serviceName) {
        var method = methodDescriptorResolver.resolve(methodName, fileDescriptor, serviceName);
        var inputTypeDescriptor = descriptorResolver.resolve(method.getInputType(), fileDescriptor);
        var outputTypeDescriptor = descriptorResolver.resolve(method.getOutputType(), fileDescriptor);
        var methodDescriptor = methodDescriptorBuilder.build(serviceName, method.getName(), inputTypeDescriptor, outputTypeDescriptor);
        var message = requestBuilder.build(inputTypeDescriptor, request, selectionSet);
        var dynamicGrpcClient = dynamicGrpcClients.get(serverName);
        return dynamicGrpcClient.callUnary(message, methodDescriptor);
    }
}
