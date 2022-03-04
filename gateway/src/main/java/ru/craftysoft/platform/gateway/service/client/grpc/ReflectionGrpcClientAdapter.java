package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.Descriptors;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.platform.gateway.builder.dynamic.FileDescriptorResolver;
import ru.craftysoft.platform.gateway.builder.reflection.ServerReflectionRequestBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
public class ReflectionGrpcClientAdapter {

    private final Map<String, ReflectionGrpcClient> reflectionClients;
    private final ServerReflectionRequestBuilder requestBuilder;
    private final FileDescriptorResolver fileDescriptorResolver;

    public Uni<Descriptors.FileDescriptor> serverReflectionInfo(String serverName, String serviceName) {
        var serviceKey = new ServiceKey(serverName, serviceName);
        return serverReflectionInfo(serviceKey);
    }

    @CacheResult(cacheName = "server-reflection-info")
    Uni<Descriptors.FileDescriptor> serverReflectionInfo(@CacheKey ServiceKey serviceKey) {
        var serverReflectionClient = reflectionClients.get(serviceKey.serverName);
        var request = requestBuilder.build(serviceKey.serviceName);
        return serverReflectionClient.serverReflectionInfo(request)
                .map(fileDescriptorResolver::resolve);
    }

    private record ServiceKey(String serverName, String serviceName) {
    }
}
