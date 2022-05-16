package ru.craftysoft.platform.gateway.configuration;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.nadel.*;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.WiringFactory;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.craftysoft.platform.gateway.configuration.instrumentation.LoggingNadelInstrumentation;
import ru.craftysoft.platform.gateway.configuration.property.NadelContractsByServicesMap;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.lang.Object;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.scalars.ExtendedScalars.*;
import static java.util.Objects.requireNonNull;

@ApplicationScoped
public class NadelConfiguration {

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.platform.gateway.nadel.request");
    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.platform.gateway.nadel.response");

    @Produces
    Nadel nadel(NadelContractsByServicesMap nadelContractsByServicesMap, MainResolver mainResolver) {
        var overallSchemasString = nadelContractsByServicesMap.nadelContractsByServices().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> loadResourceAsString("/graphql/" + entry.getValue())));
        var underlyingTypeDefs = nadelContractsByServicesMap.graphqlContractsByServices().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    var schema = loadResourceAsString("/graphql/" + entry.getValue());
                    return new SchemaParser().parse(schema);
                }));
        var scalarMap = Stream.of(GraphQLLong, DateTime, Date)
                .collect(Collectors.toMap(GraphQLScalarType::getName, scalar -> scalar));
        var wiringFactory = new WiringFactory() {
            @Override
            public boolean providesScalar(ScalarWiringEnvironment environment) {
                return scalarMap.containsKey(environment.getScalarTypeDefinition().getName());
            }

            @Override
            public GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
                return scalarMap.get(environment.getScalarTypeDefinition().getName());
            }
        };
        return Nadel.newNadel()
                .instrumentation(new LoggingNadelInstrumentation())
                .engineFactory(new NadelExecutionEngineFactory() {
                    @Nonnull
                    @Override
                    public NadelExecutionEngine create(@Nonnull Nadel nadel) {

                        final NadelExecutionEngine delegate = new NextgenEngine(nadel);

                        return new NadelExecutionEngine() {
                            @Nonnull
                            @Override
                            public CompletableFuture<ExecutionResult> execute(@Nonnull ExecutionInput executionInput,
                                                                              @Nonnull Document queryDocument,
                                                                              @Nullable InstrumentationState instrumentationState,
                                                                              @Nonnull NadelExecutionParams nadelExecutionParams) {
                                var newExecutionInput = ExecutionInput.newExecutionInput()
                                        .executionId(executionInput.getExecutionId())
                                        .cacheControl(executionInput.getCacheControl())
                                        .dataLoaderRegistry(executionInput.getDataLoaderRegistry())
                                        .operationName(executionInput.getOperationName())
                                        .query(executionInput.getQuery())
                                        .root(executionInput.getRoot())
                                        .variables(executionInput.getVariables())
                                        .graphQLContext(Map.of(
                                                ExecutionInput.class, executionInput,
                                                Document.class, queryDocument,
                                                GraphQLSchema.class, nadel.getQuerySchema()
                                        ))
                                        .build();
                                return delegate.execute(newExecutionInput, queryDocument, instrumentationState, nadelExecutionParams);
                            }
                        };
                    }
                })
                .overallSchemasString(overallSchemasString)
                .overallWiringFactory(wiringFactory)
                .underlyingTypeDefs(underlyingTypeDefs)
                .underlyingWiringFactory(wiringFactory)
                .serviceExecutionFactory(new ServiceExecutionFactory() {
                    @Nonnull
                    @Override
                    public ServiceExecution getServiceExecution(@Nonnull String serviceName) {
                        return new ServiceExecution() {

                            @Nonnull
                            @Override
                            public CompletableFuture<ServiceExecutionResult> execute(@Nonnull ServiceExecutionParameters parameters) {
                                var methodName = parameters.getExecutableNormalizedField().getFieldName();
                                if (requestLogger.isTraceEnabled()) {
                                    var query = parameters.getQuery();
                                    requestLogger.trace("query={}", AstPrinter.printAst(query));
                                }
                                return mainResolver.resolve(parameters, methodName)
                                        .map(object -> {
                                            if ("document-service".equals(serviceName)) {
                                                var documents = (List<Map<String, Object>>) object;
                                                for (var document : documents) {
                                                    document.put("batch_hydration__attributes__id", document.get("id"));
                                                    document.put("__typename__batch_hydration__attributes", "Document");
                                                }
                                            }
                                            var data = Map.of(methodName, object);
                                            if (responseLogger.isTraceEnabled()) {
                                                var result = JsonObject.mapFrom(data).encode();
                                                responseLogger.trace("result={}", result);
                                            }
                                            return new ServiceExecutionResult(data);
                                        })
                                        .subscribeAsCompletionStage();
                            }
                        };
                    }
                })
                .build();
    }

    @SneakyThrows
    private String loadResourceAsString(String path) {
        try (var inputStream = requireNonNull(getClass().getResourceAsStream(path))) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
