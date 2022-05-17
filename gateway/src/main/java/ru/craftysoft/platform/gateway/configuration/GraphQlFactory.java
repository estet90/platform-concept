package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import graphql.language.*;
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
import ru.craftysoft.platform.gateway.configuration.instrumentation.LoggingInstrumentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static graphql.scalars.ExtendedScalars.*;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class GraphQlFactory {

    private static final LoggingInstrumentation loggingInstrumentation = new LoggingInstrumentation();

    private static final SchemaParser schemaParser = new SchemaParser();

    public static <T> GraphQL graphQlFromPaths(Function<DataFetchingEnvironment, Future<T>> dataFetcher, Collection<String> paths) {
        var graphqls = parse(paths);
        return graphQlFromContracts(dataFetcher, graphqls);
    }

    public static <T> GraphQL graphQlFromContracts(Function<DataFetchingEnvironment, Future<T>> dataFetcher, Collection<String> graphqls) {
        var typeRegistry = mergeTypeDefinitionRegistry(graphqls);
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
                .instrumentation(loggingInstrumentation)
                .build();
    }

    public static List<String> parse(Collection<String> paths) {
        return paths.stream()
                .map(path -> "/graphql/" + path)
                .map(GraphQlConfiguration.class::getResourceAsStream)
                .map(Objects::requireNonNull)
                .map(inputStream -> {
                    try {
                        return inputStream.readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .toList();
    }

    private static TypeDefinitionRegistry mergeTypeDefinitionRegistry(Collection<String> graphqls) {
        var typeRegistry = new TypeDefinitionRegistry();

        var directives = new HashMap<String, DirectiveDefinition>();
        var scalars = new HashMap<String, ScalarTypeDefinition>();

        var queryDirectives = new ArrayList<Directive>();
        var queryFieldDefinitions = new ArrayList<FieldDefinition>();
        var queryAdditionalData = new HashMap<String, String>();

        var mutationDirectives = new ArrayList<Directive>();
        var mutationFieldDefinitions = new ArrayList<FieldDefinition>();
        var mutationAdditionalData = new HashMap<String, String>();

        for (var graphql : graphqls) {
            var currentTypeDefinitionRegistry = schemaParser.parse(graphql);
            currentTypeDefinitionRegistry.schemaDefinition()
                    .ifPresent(currentTypeDefinitionRegistry::remove);

            for (var currentScalar : currentTypeDefinitionRegistry.scalars().values()) {
                if (scalars.containsKey(currentScalar.getName())) {
                    currentTypeDefinitionRegistry.remove(currentScalar);
                }
            }
            scalars.putAll(currentTypeDefinitionRegistry.scalars());

            for (var currentDirective : currentTypeDefinitionRegistry.getDirectiveDefinitions().values()) {
                if (directives.containsKey(currentDirective.getName())) {
                    currentTypeDefinitionRegistry.remove(currentDirective);
                }
            }
            directives.putAll(currentTypeDefinitionRegistry.getDirectiveDefinitions());

            var currentTypes = currentTypeDefinitionRegistry.types();
            for (var currentType : currentTypes.values()) {
                var currentTypeName = currentType.getName();
                if ("Query".equals(currentTypeName) || "Mutation".equals(currentTypeName)) {
                    var currentObjectTypeDefinition = (ObjectTypeDefinition) currentType;
                    currentTypeDefinitionRegistry.remove(currentObjectTypeDefinition);
                    switch (currentTypeName) {
                        case "Query" -> fillObjectTypeDefinitionParameters(queryDirectives, queryFieldDefinitions, queryAdditionalData, currentObjectTypeDefinition);
                        case "Mutation" -> fillObjectTypeDefinitionParameters(mutationDirectives, mutationFieldDefinitions, mutationAdditionalData, currentObjectTypeDefinition);
                    }
                }
            }
            typeRegistry.merge(currentTypeDefinitionRegistry);
        }
        var operationTypeDefinitions = new ArrayList<OperationTypeDefinition>();
        if (!queryFieldDefinitions.isEmpty()) {
            var query = buildObjectTypeDefinition("Query", queryDirectives, queryFieldDefinitions, queryAdditionalData);
            typeRegistry.add(query);
            var operationTypeDefinition = buildOperationTypeDefinition("query", "Query");
            operationTypeDefinitions.add(operationTypeDefinition);
        }
        if (!mutationFieldDefinitions.isEmpty()) {
            var mutation = buildObjectTypeDefinition("Mutation", mutationDirectives, mutationFieldDefinitions, mutationAdditionalData);
            typeRegistry.add(mutation);
            var operationTypeDefinition = buildOperationTypeDefinition("mutation", "Mutation");
            operationTypeDefinitions.add(operationTypeDefinition);
        }
        var schemaDefinition = SchemaDefinition.newSchemaDefinition()
                .operationTypeDefinitions(operationTypeDefinitions)
                .build();
        typeRegistry.add(schemaDefinition);
        return typeRegistry;
    }

    private static void fillObjectTypeDefinitionParameters(ArrayList<Directive> queryDirectives, ArrayList<FieldDefinition> queryFieldDefinitions, HashMap<String, String> queryAdditionalData, ObjectTypeDefinition currentObjectTypeDefinition) {
        queryDirectives.addAll(currentObjectTypeDefinition.getDirectives());
        queryFieldDefinitions.addAll(currentObjectTypeDefinition.getFieldDefinitions());
        queryAdditionalData.putAll(currentObjectTypeDefinition.getAdditionalData());
    }

    private static OperationTypeDefinition buildOperationTypeDefinition(String operationTypeDefinitionName, String typeNameName) {
        return OperationTypeDefinition.newOperationTypeDefinition()
                .name(operationTypeDefinitionName)
                .typeName(TypeName.newTypeName()
                        .name(typeNameName)
                        .build())
                .build();
    }

    private static ObjectTypeDefinition buildObjectTypeDefinition(String Query, ArrayList<Directive> queryDirectives, ArrayList<FieldDefinition> queryFieldDefinitions, HashMap<String, String> queryAdditionalData) {
        return ObjectTypeDefinition.newObjectTypeDefinition()
                .name(Query)
                .directives(queryDirectives)
                .fieldDefinitions(queryFieldDefinitions)
                .additionalData(queryAdditionalData)
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
