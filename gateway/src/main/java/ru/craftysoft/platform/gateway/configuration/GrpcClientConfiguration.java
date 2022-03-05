package ru.craftysoft.platform.gateway.configuration;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannelBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.interceptor.GrpcClientInterceptor;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClient;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClient;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@ApplicationScoped
public class GrpcClientConfiguration {

    @ApplicationScoped
    public Map<String, DynamicGrpcClient> dynamicGrpcClients(GrpcClientConfigurationMap configurationMap) {
        return clientMap(
                configurationMap,
                DynamicGrpcClient.class,
                (configuration, channel) -> new DynamicGrpcClient(channel, configuration.dynamicClientDeadline())
        );
    }

    @ApplicationScoped
    public Map<String, ReflectionGrpcClient> reflectionClients(GrpcClientConfigurationMap configurationMap) {
        return clientMap(
                configurationMap,
                ReflectionGrpcClient.class,
                (configuration, channel) -> new ReflectionGrpcClient(channel, configuration.reflectionClientDeadline())
        );
    }

    private <T> Map<String, T> clientMap(GrpcClientConfigurationMap configurationMap,
                                         Class<T> clientClass,
                                         BiFunction<GrpcClientConfigurationMap.ServerConfiguration, Channel, T> clientBuilder) {
        return configurationMap.services().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    var channel = ManagedChannelBuilder.forTarget(entry.getValue().host() + ":" + entry.getValue().port())
                            .usePlaintext()
                            .build();
                    return clientBuilder.apply(entry.getValue(), ClientInterceptors.intercept(channel, new GrpcClientInterceptor(clientClass)));
                }));
    }

}
