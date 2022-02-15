package ru.craftysoft.platform.gateway.resolver;

import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.platform.gateway.builder.ResponseBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServersByMethodsMap;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClientAdapter;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClientAdapter;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
public class MainResolver {

    private final GrpcClientConfigurationMap configurationMap;
    private final GraphQlServersByMethodsMap graphQlServersByMethods;
    private final ResponseBuilder responseBuilder;
    private final DynamicGrpcClientAdapter dynamicGrpcClientAdapter;
    private final ReflectionGrpcClientAdapter reflectionGrpcClientAdapter;

    public Future<Map<String, Object>> resolve(DataFetchingEnvironment environment) {
        var serverName = graphQlServersByMethods.serversByMethods().get(environment.getFieldDefinition().getName());
        var serviceName = configurationMap.servers().get(serverName).serviceName();
        return reflectionGrpcClientAdapter.lookupService(serverName, serviceName)
                .flatMap(fileDescriptor -> dynamicGrpcClientAdapter.processRequest(environment, fileDescriptor, serviceName, serverName)
                        .map(responseBuilder::build));
    }

}
