package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.Descriptors;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.platform.gateway.builder.dynamic.FileDescriptorResolver;
import ru.craftysoft.platform.gateway.builder.reflection.ServerReflectionRequestBuilder;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@RequiredArgsConstructor
public class ReflectionGrpcClientAdapter {

    private final Map<String, ReflectionGrpcClient> reflectionClients;
    private final ServerReflectionRequestBuilder requestBuilder;
    private final FileDescriptorResolver fileDescriptorResolver;
    private final LoadingCache<ServiceKey, Future<Descriptors.FileDescriptor>> fileDescriptors = CacheBuilder.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new ReflectionClientCacheLoader());

    public Future<Descriptors.FileDescriptor> lookupService(String serverName, String serviceName) {
        return fileDescriptors.getUnchecked(new ServiceKey(serverName, serviceName));
    }

    private class ReflectionClientCacheLoader extends CacheLoader<ServiceKey, Future<Descriptors.FileDescriptor>> {
        @Override
        public Future<Descriptors.FileDescriptor> load(@Nonnull ServiceKey serviceName) {
            var serverReflectionClient = reflectionClients.get(serviceName.serverName);
            var request = requestBuilder.build(serviceName.serviceName);
            return serverReflectionClient.lookupService(request)
                    .map(fileDescriptorResolver::resolve);
        }
    }

    private record ServiceKey(String serverName, String serviceName) {
    }
}
