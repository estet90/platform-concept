package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

public class DynamicGrpcClient {
    private final Channel channel;

    public DynamicGrpcClient(Channel channel) {
        this.channel = channel;
    }

    public CompletableFuture<DynamicMessage> callUnary(DynamicMessage request,
                                                       CallOptions callOptions,
                                                       String serviceName,
                                                       String methodName,
                                                       Descriptors.Descriptor inputType,
                                                       Descriptors.Descriptor outputType) {
        var call = channel.newCall(createGrpcMethodDescriptor(serviceName, methodName, inputType, outputType), callOptions);
        var result = new CompletableFuture<DynamicMessage>();
        ClientCalls.asyncUnaryCall(
                call,
                request,
                new StreamObserver<>() {
                    private DynamicMessage dynamicMessage;

                    @Override
                    public void onNext(DynamicMessage dynamicMessage) {
                        this.dynamicMessage = dynamicMessage;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        result.completeExceptionally(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        result.complete(this.dynamicMessage);
                    }
                }
        );
        return result;
    }

    private MethodDescriptor<DynamicMessage, DynamicMessage> createGrpcMethodDescriptor(String serviceName,
                                                                                        String methodName,
                                                                                        Descriptors.Descriptor inputType,
                                                                                        Descriptors.Descriptor outputType) {
        var fullMethodName = MethodDescriptor.generateFullMethodName(serviceName, methodName);
        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(new DynamicMessageMarshaller(inputType))
                .setResponseMarshaller(new DynamicMessageMarshaller(outputType))
                .build();
    }
}
