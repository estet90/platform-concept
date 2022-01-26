package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Channel;
import io.grpc.reflection.v1alpha.*;
import io.grpc.reflection.v1alpha.ServerReflectionResponse.MessageResponseCase;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ServerReflectionClient {
    private static final Logger logger = LoggerFactory.getLogger(ServerReflectionClient.class);
    private static final ServerReflectionRequest LIST_SERVICES_REQUEST = ServerReflectionRequest.newBuilder()
            .setListServices("")  // Not sure what this is for, appears to be ignored.
            .build();

    private final Channel channel;

    public ServerReflectionClient(Channel channel) {
        this.channel = channel;
    }

    public CompletableFuture<List<String>> listServices() {
        var rpcHandler = new ListServicesHandler();
        var requestStream = ServerReflectionGrpc.newStub(channel)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    public CompletableFuture<FileDescriptorSet> lookupService(String serviceName) {
        var rpcHandler = new LookupServiceHandler(serviceName);
        var requestStream = ServerReflectionGrpc.newStub(channel)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    private static class ListServicesHandler implements StreamObserver<ServerReflectionResponse> {
        private final CompletableFuture<List<String>> resultFuture;
        private StreamObserver<ServerReflectionRequest> requestStream;

        private ListServicesHandler() {
            resultFuture = new CompletableFuture<>();
        }

        CompletableFuture<List<String>> start(StreamObserver<ServerReflectionRequest> requestStream) {
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
            resultFuture.completeExceptionally(t);
        }

        @Override
        public void onCompleted() {
            if (!resultFuture.isDone()) {
                logger.error("Unexpected completion of server reflection rpc while listing services");
                resultFuture.completeExceptionally(new RuntimeException("Unexpected end of rpc"));
            }
        }

        private void handleListServiceResponse(ListServiceResponse response) {
            var result = response.getServiceList().stream()
                    .map(ServiceResponse::getName)
                    .toList();
            resultFuture.complete(result);
            requestStream.onCompleted();
        }
    }

    private static class LookupServiceHandler implements StreamObserver<ServerReflectionResponse> {
        private final CompletableFuture<FileDescriptorSet> resultFuture;
        private final String serviceName;
        private final Set<String> requestedDescriptors;
        private final Map<String, FileDescriptorProto> resolvedDescriptors;
        private StreamObserver<ServerReflectionRequest> requestStream;

        private LookupServiceHandler(String serviceName) {
            this.serviceName = serviceName;
            this.resultFuture = new CompletableFuture<>();
            this.resolvedDescriptors = new HashMap<>();
            this.requestedDescriptors = new HashSet<>();
        }

        CompletableFuture<FileDescriptorSet> start(StreamObserver<ServerReflectionRequest> requestStream) {
            this.requestStream = requestStream;
            requestStream.onNext(requestForSymbol(serviceName));
            return resultFuture;
        }

        @Override
        public void onNext(ServerReflectionResponse response) {
            MessageResponseCase responseCase = response.getMessageResponseCase();
            switch (responseCase) {
                case FILE_DESCRIPTOR_RESPONSE -> {
                    var descriptors = parseDescriptors(response.getFileDescriptorResponse().getFileDescriptorProtoList());
                    descriptors.forEach(d -> {
                        resolvedDescriptors.put(d.getName(), d);
                        processDependencies(d);
                    });
                    resultFuture.complete(FileDescriptorSet.newBuilder()
                            .addAllFile(resolvedDescriptors.values())
                            .build());
                    requestStream.onCompleted();
                }
                default -> logger.warn("Got unknown reflection response type: " + responseCase);
            }
        }

        @Override
        public void onError(Throwable t) {
            resultFuture.completeExceptionally(new RuntimeException("Reflection lookup rpc failed for: " + serviceName, t));
        }

        @Override
        public void onCompleted() {
            if (!resultFuture.isDone()) {
                logger.error("Unexpected completion of the server reflection rpc");
                resultFuture.completeExceptionally(new RuntimeException("Unexpected end of rpc"));
            }
        }

        private Set<FileDescriptorProto> parseDescriptors(List<ByteString> descriptorBytes) {
            return descriptorBytes.stream()
                    .map(fileDescriptorBytes -> {
                        try {
                            return FileDescriptorProto.parseFrom(fileDescriptorBytes);
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toSet());
        }

        private void processDependencies(FileDescriptorProto fileDescriptor) {
            fileDescriptor.getDependencyList().forEach(dep -> {
                if (!resolvedDescriptors.containsKey(dep) && !requestedDescriptors.contains(dep)) {
                    requestedDescriptors.add(dep);
                    requestStream.onNext(requestForDescriptor(dep));
                }
            });
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
