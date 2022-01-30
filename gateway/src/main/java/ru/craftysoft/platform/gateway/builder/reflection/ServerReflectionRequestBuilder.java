package ru.craftysoft.platform.gateway.builder.reflection;

import io.grpc.reflection.v1alpha.ServerReflectionRequest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ServerReflectionRequestBuilder {

    public ServerReflectionRequest build(String serviceName) {
        return ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(serviceName)
                .build();
    }

}
