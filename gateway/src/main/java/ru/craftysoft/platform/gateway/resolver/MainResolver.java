package ru.craftysoft.platform.gateway.resolver;

import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Future;
import ru.craftysoft.platform.gateway.builder.ResponseBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServersByMethodsMap;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClientAdapter;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClientAdapter;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class MainResolver {

    private final GrpcClientConfigurationMap configurationMap;
    private final GraphQlServersByMethodsMap graphQlServersByMethods;
    private final ResponseBuilder responseBuilder;
    private final DynamicGrpcClientAdapter dynamicGrpcClientAdapter;
    private final ReflectionGrpcClientAdapter reflectionGrpcClientAdapter;

    public MainResolver(GrpcClientConfigurationMap configurationMap,
                        GraphQlServersByMethodsMap graphQlServersByMethods,
                        ResponseBuilder responseBuilder,
                        DynamicGrpcClientAdapter dynamicGrpcClientAdapter,
                        ReflectionGrpcClientAdapter reflectionGrpcClientAdapter) {
        this.configurationMap = configurationMap;
        this.graphQlServersByMethods = graphQlServersByMethods;
        this.responseBuilder = responseBuilder;
        this.dynamicGrpcClientAdapter = dynamicGrpcClientAdapter;
        this.reflectionGrpcClientAdapter = reflectionGrpcClientAdapter;
    }

    public Future<Map<String, Object>> resolve(DataFetchingEnvironment environment) {
        var serverName = graphQlServersByMethods.serversByMethods().get(environment.getFieldDefinition().getName());
        var serviceName = configurationMap.servers().get(serverName).serviceName();
        return reflectionGrpcClientAdapter.lookupService(serviceName, serverName)
                .flatMap(descriptorSet -> dynamicGrpcClientAdapter.processRequest(environment, descriptorSet, serviceName, serverName)
                        .map(responseBuilder::build));
    }

}
