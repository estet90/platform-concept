package ru.craftysoft.platform.gateway.resolver;

import graphql.schema.DataFetchingEnvironment;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.platform.gateway.builder.ResponseBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServicesMethodsMap;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClientAdapter;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClientAdapter;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
public class MainResolver {

    private final GrpcClientConfigurationMap configurationMap;
    private final GraphQlServicesMethodsMap graphQlServersByMethods;
    private final ResponseBuilder responseBuilder;
    private final DynamicGrpcClientAdapter dynamicGrpcClientAdapter;
    private final ReflectionGrpcClientAdapter reflectionGrpcClientAdapter;

    public Future<Map<String, Object>> resolve(DataFetchingEnvironment environment) {
        var serverName = graphQlServersByMethods.servicesByMethods().get(environment.getFieldDefinition().getName());
        var serviceName = configurationMap.services().get(serverName).serviceName();
        var uni = reflectionGrpcClientAdapter.serverReflectionInfo(serverName, serviceName)
                .flatMap(fileDescriptor -> dynamicGrpcClientAdapter.processRequest(environment, fileDescriptor, serverName, serviceName))
                .map(responseBuilder::build);
        return UniHelper.toFuture(uni);
    }

}
