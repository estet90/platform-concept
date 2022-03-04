package ru.craftysoft.platform.gateway.service.client.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

import static io.grpc.reflection.v1alpha.ServerReflectionGrpc.getServerReflectionInfoMethod;

@RequiredArgsConstructor
public class ReflectionGrpcClient {
    private final Channel channel;
    private final long deadline;

    public Uni<ServerReflectionResponse> serverReflectionInfo(ServerReflectionRequest request) {
        var call = channel
                .newCall(
                        getServerReflectionInfoMethod(),
                        CallOptions.DEFAULT.withDeadline(Deadline.after(deadline, TimeUnit.MILLISECONDS))
                );
        return io.quarkus.grpc.runtime.ClientCalls.oneToOne(
                request,
                (message, streamObserver) -> io.grpc.stub.ClientCalls.asyncUnaryCall(call, message, streamObserver)
        );
    }
}
