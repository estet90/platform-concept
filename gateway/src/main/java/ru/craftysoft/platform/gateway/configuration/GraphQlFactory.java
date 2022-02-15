package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import graphql.validation.constraints.standard.SizeConstraint;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import io.vertx.core.Future;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static graphql.scalars.ExtendedScalars.*;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class GraphQlFactory {

    public static <T> GraphQL graphQl(Function<DataFetchingEnvironment, Future<T>> dataFetcher, String graphql) {
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
                .type("Query", builder -> resolveBuilder(queries, dataFetcher, builder))
                .type("Mutation", builder -> resolveBuilder(mutations, dataFetcher, builder))
                .build();
        var graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema)
                .build();
    }

    private static List<String> resolveMethods(TypeDefinitionRegistry typeRegistry, String type) {
        return typeRegistry.getType(type)
                .map(ObjectTypeDefinition.class::cast)
                .map(ObjectTypeDefinition::getChildren)
                .stream()
                .flatMap(Collection::stream)
                .map(FieldDefinition.class::cast)
                .map(FieldDefinition::getName)
                .toList();
    }

    private static <T> TypeRuntimeWiring.Builder resolveBuilder(List<String> methods,
                                                                Function<DataFetchingEnvironment, Future<T>> dataFetcher,
                                                                TypeRuntimeWiring.Builder builder) {
        var dataFetcherMap = methods.stream()
                .collect(Collectors.toMap(name -> name, name -> (DataFetcher) VertxDataFetcher.create(dataFetcher)));
        return builder.dataFetchers(dataFetcherMap);
    }

}
