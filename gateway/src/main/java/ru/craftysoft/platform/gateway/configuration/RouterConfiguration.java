package ru.craftysoft.platform.gateway.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import graphql.ExecutionResult;
import graphql.nadel.Nadel;
import graphql.nadel.NadelExecutionInput;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.impl.GraphQLInput;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;
import ru.craftysoft.platform.gateway.handler.MainHandler;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class RouterConfiguration {

    public static final String GRAPHQL_ROUTE_PATH = "/graphql";

    static {
        var objectMapper = DatabindCodec.mapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

        var module = new JavaTimeModule();
        objectMapper.registerModule(module);
    }

    @ApplicationScoped
    public Router router(Vertx vertx, MainHandler mainHandler, Nadel nadel) {
        var router = Router.router(vertx);
        var bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler);
        router.post(GRAPHQL_ROUTE_PATH)
                .handler(mainHandler::graphqlHandle);
        router.post("/refresh")
                .handler(mainHandler::refreshHandle);
        router.post("/nadel")
                .handler(routingContext -> {
                    var query = (GraphQLQuery) GraphQLInput.decode(routingContext.getBody());
                    var nadelExecutionInput = NadelExecutionInput.newNadelExecutionInput()
                            .query(query.getQuery())
                            .operationName(query.getOperationName())
                            .variables(query.getVariables())
                            .context(routingContext)
                            .build();
                    try {
                        var string = nadel.execute(nadelExecutionInput)
                                .thenApply(ExecutionResult::toSpecification)
                                .thenApply(map -> JsonObject.mapFrom(map).encode()).get();
                        routingContext.end(string);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
        return router;
    }

}
