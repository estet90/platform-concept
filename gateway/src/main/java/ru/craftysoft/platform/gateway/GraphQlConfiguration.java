package ru.craftysoft.platform.gateway;

import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.validation.constraints.standard.SizeConstraint;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static graphql.scalars.ExtendedScalars.*;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

@ApplicationScoped
public class GraphQlConfiguration {

    private static final String graphql;

    static {
        try {
            var graphqlAsBytes = Objects.requireNonNull(GraphQlConfiguration.class.getResourceAsStream("/gateway.graphqls"))
                    .readAllBytes();
            graphql = new String(graphqlAsBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Produces
    @ApplicationScoped
    @Named("graphQl")
    public GraphQL graphQl() {
        var typeRegistry = new SchemaParser().parse(graphql);
        var validationRules = ValidationRules.newValidationRules()
                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
                .addRule(new SizeConstraint())
                .build();
        var queries = typeRegistry.getType("Query")
                .map(ObjectTypeDefinition.class::cast)
                .map(ObjectTypeDefinition::getChildren)
                .stream()
                .flatMap(Collection::stream)
                .map(FieldDefinition.class::cast)
                .map(FieldDefinition::getName)
                .toList();
        var mutations = typeRegistry.getType("Mutation")
                .map(ObjectTypeDefinition.class::cast)
                .map(ObjectTypeDefinition::getChildren)
                .stream()
                .flatMap(Collection::stream)
                .map(FieldDefinition.class::cast)
                .map(FieldDefinition::getName)
                .toList();
        var runtimeWiring = newRuntimeWiring()
                .directiveWiring(new ValidationSchemaWiring(validationRules))
                .scalar(DateTime)
                .scalar(GraphQLLong)
                .scalar(Date)
                .type("Query", builder -> {
                    var dataFetcherMap = queries.stream()
                            .map(name -> {
                                var dataFetcher = VertxDataFetcher.create((dataFetchingEnvironment -> Future.succeededFuture("")));
                                return Map.entry(name, (DataFetcher) dataFetcher);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    return builder.dataFetchers(dataFetcherMap);
                })
                .type("Mutation", builder -> {
                    var dataFetcherMap = mutations.stream()
                            .map(name -> {
                                var dataFetcher = VertxDataFetcher.create((dataFetchingEnvironment -> Future.succeededFuture("")));
                                return Map.entry(name, (DataFetcher) dataFetcher);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    return builder.dataFetchers(dataFetcherMap);
                })
                .build();
        var graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema)
                .build();
    }

    @Produces
    @ApplicationScoped
    @Named("mainVerticle")
    public AbstractVerticle mainVerticle(@Named("graphQl") GraphQL graphQl) {
        return new AbstractVerticle() {

            private HttpServer server;

            @Override
            public void start() {
                var vertx = Vertx.vertx();
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
        };
    }
}
