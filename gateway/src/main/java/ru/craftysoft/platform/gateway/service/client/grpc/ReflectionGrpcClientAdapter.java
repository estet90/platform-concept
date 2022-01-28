package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.DescriptorProtos;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class ReflectionGrpcClientAdapter {

    private final Map<String, ReflectionGrpcClient> reflectionClients;

    public ReflectionGrpcClientAdapter(Map<String, ReflectionGrpcClient> reflectionClients) {
        this.reflectionClients = reflectionClients;
    }

    public CompletableFuture<DescriptorProtos.FileDescriptorSet> lookupService(String serviceName, String serverName){
        var serverReflectionClient = reflectionClients.get(serverName);
        return serverReflectionClient.lookupService(serviceName);
    }
}
