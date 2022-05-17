package ru.craftysoft.platform.gateway.resolver;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.platform.gateway.builder.ResponseBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServicesByMethodsMap;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClientAdapter;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClientAdapter;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

import static java.util.Optional.ofNullable;

@ApplicationScoped
@RequiredArgsConstructor
public class MainResolver {

    private final GrpcClientConfigurationMap configurationMap;
    private final GraphQlServicesByMethodsMap graphQlServersByMethods;
    private final ResponseBuilder responseBuilder;
    private final DynamicGrpcClientAdapter dynamicGrpcClientAdapter;
    private final ReflectionGrpcClientAdapter reflectionGrpcClientAdapter;

    public Future<Object> resolve(DataFetchingEnvironment environment) {
        var methodName = environment.getFieldDefinition().getName();
        var request = resolveRequest(environment.getArguments());
        var selectionSet = environment.getSelectionSet();
        var uni = resolve(methodName, request, selectionSet);
        return UniHelper.toFuture(uni);
    }

    private Uni<Object> resolve(String methodName, Map<String, Object> request, DataFetchingFieldSelectionSet selectionSet) {
        var serverName = graphQlServersByMethods.servicesByMethods().get(methodName);
        var serviceName = configurationMap.services().get(serverName).serviceName();
        return reflectionGrpcClientAdapter.serverReflectionInfo(serverName, serviceName)
                .flatMap(fileDescriptor -> dynamicGrpcClientAdapter.processRequest(methodName, request, selectionSet, fileDescriptor, serverName, serviceName))
                .map(responseBuilder::build);
    }

    private Map<String, Object> resolveRequest(Map<String, Object> arguments) {
        var size = arguments.size();
        if (size == 1) {
            return ofNullable(arguments.get("request"))
                    .map(o -> (Map<String, Object>) o)
                    .orElse(arguments);
        } else if (size > 1) {
            return arguments;
        }
        return Map.of();
    }

}
