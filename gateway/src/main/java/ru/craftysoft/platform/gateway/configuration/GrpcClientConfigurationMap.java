package ru.craftysoft.platform.gateway.configuration;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "grpc")
public interface GrpcClientConfigurationMap {
    Map<String, ServerConfiguration> servers();

    interface ServerConfiguration {
        String serviceName();

        String host();

        int port();
    }
}
