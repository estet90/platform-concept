package ru.craftysoft.platform.gateway.configuration;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClient;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class GrpcClientConfiguration {


    @ApplicationScoped
    @Named("channels")
    public Map<String, Channel> channels(GrpcClientConfigurationMap configurationMap) {
        return configurationMap.servers().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> ManagedChannelBuilder.forTarget(entry.getValue().host() + ":" + entry.getValue().port())
                        .usePlaintext()
                        .build())
                );
    }

    @ApplicationScoped
    public Map<String, DynamicGrpcClient> dynamicGrpcClients(@Named("channels") Map<String, Channel> channels) {
        return channels.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new DynamicGrpcClient(entry.getValue())));
    }

    @ApplicationScoped
    public Map<String, ReflectionGrpcClient> reflectionClients(@Named("channels") Map<String, Channel> channels) {
        return channels.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ReflectionGrpcClient(entry.getValue())));
    }

}
