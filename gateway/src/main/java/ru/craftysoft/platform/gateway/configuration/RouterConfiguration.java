package ru.craftysoft.platform.gateway.configuration;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import ru.craftysoft.platform.gateway.handler.MainHandler;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RouterConfiguration {

    public static final String GRAPHQL_ROUTE_PATH = "/graphql";

    @ApplicationScoped
    public Router router(Vertx vertx, MainHandler mainHandler) {
        var router = Router.router(vertx);
        var bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        router.post(GRAPHQL_ROUTE_PATH)
                .handler(mainHandler::graphqlHandle);
        router.post("/refresh")
                .handler(mainHandler::refreshHandle);
        return router;
    }

}
