package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Channel;
import io.grpc.reflection.v1alpha.ListServiceResponse;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServerReflectionResponse.MessageResponseCase;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerReflectionClient {
    private static final Logger logger = LoggerFactory.getLogger(ServerReflectionClient.class);
    private static final long LIST_RPC_DEADLINE_MS = 1_000;
    private static final long LOOKUP_RPC_DEADLINE_MS = 10_000;
    private static final ServerReflectionRequest LIST_SERVICES_REQUEST = ServerReflectionRequest.newBuilder()
                    .setListServices("")  // Not sure what this is for, appears to be ignored.
                    .build();

    private final Channel channel;

    public ServerReflectionClient(Channel channel) {
        this.channel = channel;
    }

    public ListenableFuture<ImmutableList<String>> listServices() {
        var rpcHandler = new ListServicesHandler();
        var requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LIST_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    public ListenableFuture<FileDescriptorSet> lookupService(String serviceName) {
        var rpcHandler = new LookupServiceHandler(serviceName);
        var requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LOOKUP_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    private static class ListServicesHandler implements StreamObserver<ServerReflectionResponse> {
        private final SettableFuture<ImmutableList<String>> resultFuture;
        private StreamObserver<ServerReflectionRequest> requestStream;

        private ListServicesHandler() {
            resultFuture = SettableFuture.create();
        }

        ListenableFuture<ImmutableList<String>> start(StreamObserver<ServerReflectionRequest> requestStream) {
            this.requestStream = requestStream;
            requestStream.onNext(LIST_SERVICES_REQUEST);
            return resultFuture;
        }

        @Override
        public void onNext(ServerReflectionResponse serverReflectionResponse) {
            MessageResponseCase responseCase = serverReflectionResponse.getMessageResponseCase();
            switch (responseCase) {
                case LIST_SERVICES_RESPONSE -> handleListServiceResponse(serverReflectionResponse.getListServicesResponse());
                default -> logger.warn("Got unknown reflection response type: " + responseCase);
            }
        }

        @Override
        public void onError(Throwable t) {
            resultFuture.setException(new RuntimeException("Error in server reflection rpc while listing services", t));
        }

        @Override
        public void onCompleted() {
            if (!resultFuture.isDone()) {
                logger.error("Unexpected completion of server reflection rpc while listing services");
                resultFuture.setException(new RuntimeException("Unexpected end of rpc"));
            }
        }

        private void handleListServiceResponse(ListServiceResponse response) {
            ImmutableList.Builder<String> servicesBuilder = ImmutableList.builder();
            response.getServiceList().forEach(service -> servicesBuilder.add(service.getName()));
            resultFuture.set(servicesBuilder.build());
            requestStream.onCompleted();
        }
    }

    private static class LookupServiceHandler implements StreamObserver<ServerReflectionResponse> {
        private final SettableFuture<FileDescriptorSet> resultFuture;
        private final String serviceName;
        private final HashSet<String> requestedDescriptors;
        private final HashMap<String, FileDescriptorProto> resolvedDescriptors;
        private StreamObserver<ServerReflectionRequest> requestStream;

        private int outstandingRequests;

        private LookupServiceHandler(String serviceName) {
            this.serviceName = serviceName;
            this.resultFuture = SettableFuture.create();
            this.resolvedDescriptors = new HashMap<>();
            this.requestedDescriptors = new HashSet<>();
            this.outstandingRequests = 0;
        }

        ListenableFuture<FileDescriptorSet> start(
                StreamObserver<ServerReflectionRequest> requestStream) {
            this.requestStream = requestStream;
            requestStream.onNext(requestForSymbol(serviceName));
            ++outstandingRequests;
            return resultFuture;
        }

        @Override
        public void onNext(ServerReflectionResponse response) {
            MessageResponseCase responseCase = response.getMessageResponseCase();
            switch (responseCase) {
                case FILE_DESCRIPTOR_RESPONSE -> {
                    ImmutableSet<FileDescriptorProto> descriptors =
                            parseDescriptors(response.getFileDescriptorResponse().getFileDescriptorProtoList());
                    descriptors.forEach(d -> resolvedDescriptors.put(d.getName(), d));
                    descriptors.forEach(this::processDependencies);
                }
                default -> logger.warn("Got unknown reflection response type: " + responseCase);
            }
        }

        @Override
        public void onError(Throwable t) {
            resultFuture.setException(new RuntimeException("Reflection lookup rpc failed for: " + serviceName, t));
        }

        @Override
        public void onCompleted() {
            if (!resultFuture.isDone()) {
                logger.error("Unexpected completion of the server reflection rpc");
                resultFuture.setException(new RuntimeException("Unexpected end of rpc"));
            }
        }

        private ImmutableSet<FileDescriptorProto> parseDescriptors(List<ByteString> descriptorBytes) {
            ImmutableSet.Builder<FileDescriptorProto> resultBuilder = ImmutableSet.builder();
            for (ByteString fileDescriptorBytes : descriptorBytes) {
                try {
                    resultBuilder.add(FileDescriptorProto.parseFrom(fileDescriptorBytes));
                } catch (InvalidProtocolBufferException e) {
                    logger.warn("Failed to parse bytes as file descriptor proto");
                }
            }
            return resultBuilder.build();
        }

        private void processDependencies(FileDescriptorProto fileDescriptor) {
            logger.debug("Processing deps of descriptor: " + fileDescriptor.getName());
            fileDescriptor.getDependencyList().forEach(dep -> {
                if (!resolvedDescriptors.containsKey(dep) && !requestedDescriptors.contains(dep)) {
                    requestedDescriptors.add(dep);
                    ++outstandingRequests;
                    requestStream.onNext(requestForDescriptor(dep));
                }
            });

            --outstandingRequests;
            if (outstandingRequests == 0) {
                logger.debug("Retrieved service definition for [{}] by reflection", serviceName);
                resultFuture.set(FileDescriptorSet.newBuilder()
                        .addAllFile(resolvedDescriptors.values())
                        .build());
                requestStream.onCompleted();
            }
        }

        private static ServerReflectionRequest requestForDescriptor(String name) {
            return ServerReflectionRequest.newBuilder()
                    .setFileByFilename(name)
                    .build();
        }

        private static ServerReflectionRequest requestForSymbol(String symbol) {
            return ServerReflectionRequest.newBuilder()
                    .setFileContainingSymbol(symbol)
                    .build();
        }
    }
}
