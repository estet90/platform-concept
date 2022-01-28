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
public class Resolver {

    private final GrpcClientConfigurationMap configurationMap;
    private final GraphQlServersByMethodsMap graphQlServersByMethods;
    private final ResponseBuilder responseBuilder;
    private final DynamicGrpcClientAdapter dynamicGrpcClientAdapter;
    private final ReflectionGrpcClientAdapter reflectionGrpcClientAdapter;

    public Resolver(GrpcClientConfigurationMap configurationMap,
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
        var future = reflectionGrpcClientAdapter.lookupService(serviceName, serverName)
                .thenCompose(descriptorSet -> dynamicGrpcClientAdapter.processRequest(environment, descriptorSet, serviceName, serverName)
                        .thenApply(responseBuilder::build));
        return Future.fromCompletionStage(future);
    }

}
