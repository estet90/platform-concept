package ru.craftysoft.platform.gateway.configuration.property;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "grpc")
public interface GrpcClientConfigurationMap {
    Map<String, ServerConfiguration> services();

    interface ServerConfiguration {
        String serviceName();

        String host();

        int port();

        long reflectionClientDeadline();

        long dynamicClientDeadline();
    }
}
