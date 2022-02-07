package ru.craftysoft.platform.gateway.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClient;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClient;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class GrpcClientConfiguration {

    @ApplicationScoped
    public Map<String, DynamicGrpcClient> dynamicGrpcClients(GrpcClientConfigurationMap configurationMap) {
        return configurationMap.servers().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    var channel = buildManagedChannel(entry);
                    return new DynamicGrpcClient(channel, entry.getValue().dynamicClientDeadline());
                }));
    }

    @ApplicationScoped
    public Map<String, ReflectionGrpcClient> reflectionClients(GrpcClientConfigurationMap configurationMap) {
        return configurationMap.servers().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    var channel = buildManagedChannel(entry);
                    return new ReflectionGrpcClient(channel, entry.getValue().reflectionClientDeadline());
                }));
    }

    private ManagedChannel buildManagedChannel(Map.Entry<String, GrpcClientConfigurationMap.ServerConfiguration> entry) {
        return ManagedChannelBuilder.forTarget(entry.getValue().host() + ":" + entry.getValue().port())
                .usePlaintext()
                .build();
    }

}
