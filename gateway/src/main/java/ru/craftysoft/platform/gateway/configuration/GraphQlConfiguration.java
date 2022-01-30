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
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
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
    public GraphQL graphQl(MainResolver mainResolver) {
        var typeRegistry = new SchemaParser().parse(graphql);
        var validationRules = ValidationRules.newValidationRules()
                .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
                .addRule(new SizeConstraint())
                .build();
        var queries = resolveMethods(typeRegistry, "Query");
        var mutations = resolveMethods(typeRegistry, "Mutation");
        var runtimeWiring = newRuntimeWiring()
                .directiveWiring(new ValidationSchemaWiring(validationRules))
                .scalar(GraphQLLong)
                .scalar(DateTime)
                .scalar(Date)
                .type("Query", builder -> resolveBuilder(queries, mainResolver, builder))
                .type("Mutation", builder -> resolveBuilder(mutations, mainResolver, builder))
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

    private TypeRuntimeWiring.Builder resolveBuilder(List<String> methods, MainResolver mainResolver, TypeRuntimeWiring.Builder builder) {
        var dataFetcherMap = methods.stream()
                .collect(Collectors.toMap(name -> name, name -> (DataFetcher) VertxDataFetcher.create(mainResolver::resolve)));
        return builder.dataFetchers(dataFetcherMap);
    }
}
