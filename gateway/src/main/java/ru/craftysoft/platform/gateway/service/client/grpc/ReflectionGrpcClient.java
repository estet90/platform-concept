package ru.craftysoft.platform.gateway.service.client.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.grpc.reflection.v1alpha.ServerReflectionGrpc.getServerReflectionInfoMethod;

public class ReflectionGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(ReflectionGrpcClient.class);

    private final Channel channel;

    public ReflectionGrpcClient(Channel channel) {
        this.channel = channel;
    }

    public Future<ServerReflectionResponse> lookupService(ServerReflectionRequest request) {
        var call = channel.newCall(getServerReflectionInfoMethod(), CallOptions.DEFAULT);
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
