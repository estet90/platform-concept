package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class DynamicGrpcClient {
    private final Channel channel;

    public DynamicGrpcClient(Channel channel) {
        this.channel = channel;
    }

    public Future<Message> callUnary(DynamicMessage request, MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor) {
        var call = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
        var promise = Promise.<Message>promise();
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
                        promise.fail(throwable);
                    }

                    @Override
                    public void onCompleted() {
                        promise.complete(this.dynamicMessage);
                    }
                }
        );
        return promise.future();
    }
}
