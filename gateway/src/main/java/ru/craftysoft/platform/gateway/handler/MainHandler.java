package ru.craftysoft.platform.gateway.handler;

import graphql.GraphQL;
import io.quarkus.cache.CacheInvalidateAll;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.platform.gateway.configuration.GraphQlFactory;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServicesByMethodsMap;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.platform.gateway.configuration.RouterConfiguration.GRAPHQL_ROUTE_PATH;

@ApplicationScoped
@RequiredArgsConstructor
public class MainHandler {

    private final GraphQL graphQl;
    private final Router router;
    private final MainResolver mainResolver;
    private final GraphQlServicesByMethodsMap graphQlServersByMethods;

    private final GraphQLHandlerOptions graphQlHandlerOptions = new GraphQLHandlerOptions()
            .setRequestMultipartEnabled(true)
            .setRequestBatchingEnabled(true);

    public void graphqlHandle(RoutingContext routingContext) {
        GraphQLHandler.create(graphQl, graphQlHandlerOptions).handle(routingContext);
    }

    public void refreshHandle(RoutingContext routingContext) {
        var serviceName = routingContext.queryParams().get("service");
        if (serviceName == null) {
            throw new IllegalArgumentException("Не передан query-параметр 'serviceName'");
        }
        var isExists = graphQlServersByMethods.contractsByServices().containsKey(serviceName);
        if (!isExists) {
            throw new IllegalArgumentException("Контракт с serviceName='%s' не существует".formatted(serviceName));
        }
        var paths = graphQlServersByMethods.contractsByServices().entrySet().stream()
                .filter(entry -> !serviceName.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        var graphqls = new ArrayList<>(GraphQlFactory.parse(paths));
        var contract = ofNullable(routingContext.getBody())
                .map(Buffer::getBytes)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .orElseThrow();
        graphqls.add(contract);
        var newGraphQl = GraphQlFactory.graphQlFromContracts(mainResolver::resolve, graphqls);
        var newGraphQlHandler = GraphQLHandler.create(newGraphQl, graphQlHandlerOptions);
        invalidateCache().subscribe().asCompletionStage();
        router.getRoutes().stream()
                .filter(route -> GRAPHQL_ROUTE_PATH.equals(route.getName()))
                .findFirst()
                .orElseThrow()
                .remove();
        router.post(GRAPHQL_ROUTE_PATH)
                .handler(newGraphQlHandler);
        routingContext.response().end("OK");
    }

    @CacheInvalidateAll(cacheName = "server-reflection-info")
    Uni<Void> invalidateCache() {
        return Uni.createFrom().voidItem();
    }
}
