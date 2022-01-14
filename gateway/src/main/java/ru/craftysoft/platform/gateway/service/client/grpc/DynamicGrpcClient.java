package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.craftysoft.platform.gateway.DynamicMessageMarshaller;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

public class DynamicGrpcClient {
    private final Channel channel;

    public DynamicGrpcClient(Channel channel) {
        this.channel = channel;
    }

    public ListenableFuture<Void> callUnary(DynamicMessage request,
                                            StreamObserver<DynamicMessage> responseObserver,
                                            CallOptions callOptions,
                                            String serviceName,
                                            String methodName,
                                            Descriptors.Descriptor inputType,
                                            Descriptors.Descriptor outputType) {
        var call = channel.newCall(createGrpcMethodDescriptor(serviceName, methodName, inputType, outputType), callOptions);
        var doneObserver = new DoneObserver<DynamicMessage>();
        ClientCalls.asyncUnaryCall(
                call,
                request,
                CompositeStreamObserver.of(responseObserver, doneObserver)
        );
        return doneObserver.getCompletionFuture();
    }

    private io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> createGrpcMethodDescriptor(String serviceName,
                                                                                                String methodName,
                                                                                                Descriptors.Descriptor inputType,
                                                                                                Descriptors.Descriptor outputType) {
        var fullMethodName = io.grpc.MethodDescriptor.generateFullMethodName(serviceName, methodName);
        return io.grpc.MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(new DynamicMessageMarshaller(inputType))
                .setResponseMarshaller(new DynamicMessageMarshaller(outputType))
                .build();
    }

    private static class DoneObserver<T> implements StreamObserver<T> {
        private final SettableFuture<Void> doneFuture;

        DoneObserver() {
            this.doneFuture = SettableFuture.create();
        }

        @Override
        public synchronized void onCompleted() {
            doneFuture.set(null);
        }

        @Override
        public synchronized void onError(Throwable t) {
            doneFuture.setException(t);
        }

        @Override
        public void onNext(T next) {
            // Do nothing.
        }

        ListenableFuture<Void> getCompletionFuture() {
            return doneFuture;
        }
    }

    private static class CompositeStreamObserver<T> implements StreamObserver<T> {
        private static final Logger logger = LoggerFactory.getLogger(CompositeStreamObserver.class);
        private final ImmutableList<StreamObserver<T>> observers;

        @SafeVarargs
        public static <T> CompositeStreamObserver<T> of(StreamObserver<T>... observers) {
            return new CompositeStreamObserver<T>(ImmutableList.copyOf(observers));
        }

        private CompositeStreamObserver(ImmutableList<StreamObserver<T>> observers) {
            this.observers = observers;
        }

        @Override
        public void onCompleted() {
            for (StreamObserver<T> observer : observers) {
                try {
                    observer.onCompleted();
                } catch (Throwable t) {
                    logger.error("Exception in composite onComplete, moving on", t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            for (StreamObserver<T> observer : observers) {
                try {
                    observer.onError(t);
                } catch (Throwable s) {
                    logger.error("Exception in composite onError, moving on", s);
                }
            }
        }

        @Override
        public void onNext(T value) {
            for (StreamObserver<T> observer : observers) {
                try {
                    observer.onNext(value);
                } catch (Throwable t) {
                    logger.error("Exception in composite onNext, moving on", t);
                }
            }
        }
    }
}
