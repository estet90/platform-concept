package ru.craftysoft.platform.gateway.handler;

import graphql.GraphQL;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import ru.craftysoft.platform.gateway.configuration.GraphQlFactory;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.platform.gateway.configuration.RouterConfiguration.GRAPHQL_ROUTE_PATH;

@ApplicationScoped
public class MainHandler {

    private final GraphQL graphQl;
    private final Router router;
    private final MainResolver mainResolver;
    private final GraphQLHandlerOptions graphQlHandlerOptions;

    public MainHandler(@Named("graphQl") GraphQL graphQl,
                       Router router,
                       MainResolver mainResolver) {
        this.graphQl = graphQl;
        this.router = router;
        this.mainResolver = mainResolver;
        this.graphQlHandlerOptions = new GraphQLHandlerOptions()
                .setRequestMultipartEnabled(true)
                .setRequestBatchingEnabled(true);
    }

    public void graphqlHandle(RoutingContext routingContext) {
        GraphQLHandler.create(graphQl, graphQlHandlerOptions).handle(routingContext);
    }

    public void refreshHandle(RoutingContext routingContext) {
        var contract = ofNullable(routingContext.getBody())
                .map(Buffer::getBytes)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .orElseThrow();
        var newGraphQl = GraphQlFactory.graphQl(mainResolver::resolve, contract);
        var newGraphQlHandler = GraphQLHandler.create(newGraphQl, graphQlHandlerOptions);
        router.getRoutes().stream()
                .filter(route -> GRAPHQL_ROUTE_PATH.equals(route.getName()))
                .findFirst()
                .orElseThrow()
                .remove();
        router.post(GRAPHQL_ROUTE_PATH)
                .handler(newGraphQlHandler);
        var response = routingContext.response();
        response.end("OK");
    }
}
