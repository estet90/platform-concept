package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named("mainVerticle")
public class MainVerticle extends AbstractVerticle {

    private final Vertx vertx;
    private final GraphQL graphQl;

    private HttpServer server;

    public MainVerticle(Vertx vertx, @Named("graphQl") GraphQL graphQl) {
        this.vertx = vertx;
        this.graphQl = graphQl;
    }

    @Override
    public void start() {
        var router = Router.router(vertx);
        var bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        var options = new GraphQLHandlerOptions()
                .setRequestMultipartEnabled(true)
                .setRequestBatchingEnabled(true);
        var graphQlHandler = GraphQLHandler.create(graphQl, options);
        router.post("/graphql")
                .handler(graphQlHandler);
        server = vertx.createHttpServer()
                .requestHandler(router);
        server.listen(8080)
                .onComplete(httpServerAsyncResult -> {
                    if (httpServerAsyncResult.failed()) {
                        httpServerAsyncResult.cause().printStackTrace();
                    } else {
                        System.out.println("zaebis");
                    }
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        server.close(stopPromise);
    }

}
