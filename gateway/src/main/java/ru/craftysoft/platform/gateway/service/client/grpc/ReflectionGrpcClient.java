package ru.craftysoft.platform.gateway.service.client.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.util.concurrent.TimeUnit;

import static io.grpc.reflection.v1alpha.ServerReflectionGrpc.getServerReflectionInfoMethod;

public class ReflectionGrpcClient {
    private final Channel channel;
    private final long deadline;

    public ReflectionGrpcClient(Channel channel, long deadline) {
        this.channel = channel;
        this.deadline = deadline;
    }

    public Future<ServerReflectionResponse> lookupService(ServerReflectionRequest request) {
        var call = channel.newCall(
                getServerReflectionInfoMethod(),
                CallOptions.DEFAULT.withDeadline(Deadline.after(deadline, TimeUnit.MILLISECONDS))
        );
        var promise = Promise.<ServerReflectionResponse>promise();
        ClientCalls.asyncUnaryCall(
                call,
                request,
                new StreamObserver<>() {
                    private ServerReflectionResponse dynamicMessage;

                    @Override
                    public void onNext(ServerReflectionResponse dynamicMessage) {
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
