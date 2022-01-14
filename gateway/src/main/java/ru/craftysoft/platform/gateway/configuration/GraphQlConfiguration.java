package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import graphql.validation.constraints.standard.SizeConstraint;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import io.vertx.core.Vertx;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import ru.craftysoft.platform.gateway.Resolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
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

    @ApplicationScoped
    @Named("graphQl")
    public GraphQL graphQl(Resolver resolver) {
        var typeRegistry = new SchemaParser().parse(graphql);
        var validationRules = ValidationRules.newValidationRules()
                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
                .addRule(new SizeConstraint())
                .build();
        var queries = resolveMethods(typeRegistry, "Query");
        var mutations = resolveMethods(typeRegistry, "Mutation");
        var runtimeWiring = newRuntimeWiring()
                .directiveWiring(new ValidationSchemaWiring(validationRules))
                .scalar(DateTime)
                .scalar(GraphQLLong)
                .scalar(Date)
                .type("Query", builder -> resolveBuilder(queries, resolver, builder))
                .type("Mutation", builder -> resolveBuilder(mutations, resolver, builder))
                .build();
        var graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema)
                .build();
    }

    private List<String> resolveMethods(TypeDefinitionRegistry typeRegistry, String Query) {
        return typeRegistry.getType(Query)
                .map(ObjectTypeDefinition.class::cast)
                .map(ObjectTypeDefinition::getChildren)
                .stream()
                .flatMap(Collection::stream)
                .map(FieldDefinition.class::cast)
                .map(FieldDefinition::getName)
                .toList();
    }

    private TypeRuntimeWiring.Builder resolveBuilder(List<String> mutations, Resolver resolver, TypeRuntimeWiring.Builder builder) {
        var dataFetcherMap = mutations.stream()
                .map(name -> {
                    var dataFetcher = VertxDataFetcher.create(resolver::handle);
                    return Map.entry(name, (DataFetcher) dataFetcher);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return builder.dataFetchers(dataFetcherMap);
    }

    @ApplicationScoped
    @Named("applicationVertx")
    public Vertx applicationVertx() {
        return Vertx.vertx();
    }
}
