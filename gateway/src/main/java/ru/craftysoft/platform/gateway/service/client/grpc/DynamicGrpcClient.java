package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import io.grpc.*;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import ru.craftysoft.platform.gateway.interceptor.GrpcClientInterceptor;

import java.util.concurrent.TimeUnit;

public class DynamicGrpcClient {
    private final Channel channel;
    private final long deadline;

    public DynamicGrpcClient(Channel channel, long deadline) {
        this.channel = channel;
        this.deadline = deadline;
    }

    public Future<Message> callUnary(DynamicMessage request, MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor) {
        var call = ClientInterceptors.intercept(channel, new GrpcClientInterceptor(this.getClass()))
                .newCall(
                        methodDescriptor,
                        CallOptions.DEFAULT.withDeadline(Deadline.after(deadline, TimeUnit.MILLISECONDS))
                );
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
