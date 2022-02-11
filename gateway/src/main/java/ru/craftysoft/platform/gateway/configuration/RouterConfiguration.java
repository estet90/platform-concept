package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class RouterConfiguration {

    @ApplicationScoped
    public Router router(Vertx vertx,
                         @Named("graphQl") GraphQL graphQl,
                         MainResolver mainResolver) {
        var router = Router.router(vertx);
        var bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        var options = new GraphQLHandlerOptions()
                .setRequestMultipartEnabled(true)
                .setRequestBatchingEnabled(true);
        var graphQlHandler = GraphQLHandler.create(graphQl, options);
        router.post("/graphql")
                .handler(graphQlHandler);
        router.post("/refresh")
                .handler(routingContext -> {
                    var contract = ofNullable(routingContext.getBody())
                            .map(Buffer::getBytes)
                            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                            .orElseThrow();
                    var newGraphQl = GraphQlFactory.graphQl(mainResolver::resolve, contract);
                    var newGraphQlHandler = GraphQLHandler.create(newGraphQl, options);
                    router.getRoutes().stream()
                            .filter(route -> "/graphql".equals(route.getName()))
                            .findFirst()
                            .orElseThrow()
                            .remove();
                    router.post("/graphql")
                            .handler(newGraphQlHandler);
                    var response = routingContext.response();
                    response.end("OK");
                });
        return router;
    }

}
