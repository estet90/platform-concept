package ru.craftysoft.platform.gateway.service.client.grpc;

import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.vertx.core.Future;
import ru.craftysoft.platform.gateway.builder.reflection.ServerReflectionRequestBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class ReflectionGrpcClientAdapter {

    private final Map<String, ReflectionGrpcClient> reflectionClients;
    private final ServerReflectionRequestBuilder requestBuilder;

    public ReflectionGrpcClientAdapter(Map<String, ReflectionGrpcClient> reflectionClients, ServerReflectionRequestBuilder requestBuilder) {
        this.reflectionClients = reflectionClients;
        this.requestBuilder = requestBuilder;
    }

    public Future<ServerReflectionResponse> lookupService(String serviceName, String serverName) {
        var serverReflectionClient = reflectionClients.get(serverName);
        var request = requestBuilder.build(serviceName);
        return serverReflectionClient.lookupService(request);
    }
}
