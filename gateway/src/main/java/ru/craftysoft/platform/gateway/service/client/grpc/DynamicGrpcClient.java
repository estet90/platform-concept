package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.DynamicMessage;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class DynamicGrpcClient {
    private final Channel channel;
    private final long deadline;

    public Uni<DynamicMessage> callUnary(DynamicMessage request, MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor) {
        var call = channel
                .newCall(
                        methodDescriptor,
                        CallOptions.DEFAULT.withDeadline(Deadline.after(deadline, TimeUnit.MILLISECONDS))
                );
        return io.quarkus.grpc.runtime.ClientCalls.oneToOne(
                request,
                (message, streamObserver) -> io.grpc.stub.ClientCalls.asyncUnaryCall(call, message, streamObserver)
        );
    }
}
