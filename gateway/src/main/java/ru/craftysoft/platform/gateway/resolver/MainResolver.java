package ru.craftysoft.platform.gateway.resolver;

import graphql.AssertException;
import graphql.GraphQLContext;
import graphql.language.*;
import graphql.nadel.ServiceExecutionParameters;
import graphql.normalized.NormalizedInputValue;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.DataFetchingFieldSelectionSetImpl;
import graphql.schema.GraphQLSchema;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.craftysoft.platform.gateway.builder.ResponseBuilder;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServicesByMethodsMap;
import ru.craftysoft.platform.gateway.configuration.property.GrpcClientConfigurationMap;
import ru.craftysoft.platform.gateway.service.client.grpc.DynamicGrpcClientAdapter;
import ru.craftysoft.platform.gateway.service.client.grpc.ReflectionGrpcClientAdapter;

import javax.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.collect.ImmutableKit.map;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@ApplicationScoped
@RequiredArgsConstructor
public class MainResolver {

    private final GrpcClientConfigurationMap configurationMap;
    private final GraphQlServicesByMethodsMap graphQlServersByMethods;
    private final ResponseBuilder responseBuilder;
    private final DynamicGrpcClientAdapter dynamicGrpcClientAdapter;
    private final ReflectionGrpcClientAdapter reflectionGrpcClientAdapter;

    public Future<Object> resolve(DataFetchingEnvironment environment) {
        var methodName = environment.getFieldDefinition().getName();
        var request = resolveRequest(environment.getArguments());
        var selectionSet = environment.getSelectionSet();
        var uni = resolve(methodName, request, selectionSet);
        return UniHelper.toFuture(uni);
    }

    public Uni<Object> resolve(ServiceExecutionParameters parameters, String methodName) {
        var executableNormalizedField = parameters.getExecutableNormalizedField();
        Map<String, Object> normalizedArguments = !executableNormalizedField.getResolvedArguments().isEmpty()
                ? executableNormalizedField.getResolvedArguments()
                : executableNormalizedField.getNormalizedArguments().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> normalisedValueToVariableValue(entry.getValue())));
        var request = resolveRequest(normalizedArguments);
        var selectionSet = DataFetchingFieldSelectionSetImpl.newCollector(
                ((GraphQLContext) parameters.getContext()).get(GraphQLSchema.class),
                null,
                parameters::getExecutableNormalizedField
        );
        return resolve(methodName, request, selectionSet);
    }

    private Uni<Object> resolve(String methodName, Map<String, Object> request, DataFetchingFieldSelectionSet selectionSet) {
        var serverName = graphQlServersByMethods.servicesByMethods().get(methodName);
        var serviceName = configurationMap.services().get(serverName).serviceName();
        return reflectionGrpcClientAdapter.serverReflectionInfo(serverName, serviceName)
                .flatMap(fileDescriptor -> dynamicGrpcClientAdapter.processRequest(methodName, request, selectionSet, fileDescriptor, serverName, serviceName))
                .map(responseBuilder::build);
    }

    private Map<String, Object> resolveRequest(Map<String, Object> arguments) {
        var size = arguments.size();
        if (size == 1) {
            return ofNullable(arguments.get("request"))
                    .map(o -> (Map<String, Object>) o)
                    .orElse(arguments);
        } else if (size > 1) {
            return arguments;
        }
        return Map.of();
    }

    @Nullable
    private Object normalisedValueToVariableValue(Object maybeValue) {
        Object variableValue;
        if (maybeValue instanceof NormalizedInputValue) {
            NormalizedInputValue normalizedInputValue = (NormalizedInputValue) maybeValue;
            Object inputValue = normalizedInputValue.getValue();
            if (inputValue instanceof Value) {
                variableValue = toVariableValue((Value<?>) inputValue);
            } else if (inputValue instanceof List) {
                variableValue = normalisedValueToVariableValues((List<Object>) inputValue);
            } else if (inputValue instanceof Map) {
                variableValue = normalisedValueToVariableValues((Map<String, Object>) inputValue);
            } else if (inputValue == null) {
                variableValue = null;
            } else {
                throw new AssertException("Should never happen. Did not expect NormalizedInputValue.getValue() of type: " + maybeClass(inputValue));
            }
        } else if (maybeValue instanceof Value) {
            Value<?> value = (Value<?>) maybeValue;
            variableValue = toVariableValue(value);
        } else if (maybeValue instanceof List) {
            variableValue = normalisedValueToVariableValues((List<Object>) maybeValue);
        } else if (maybeValue instanceof Map) {
            variableValue = normalisedValueToVariableValues((Map<String, Object>) maybeValue);
        } else {
            throw new AssertException("Should never happen. Did not expect type: " + maybeClass(maybeValue));
        }
        return variableValue;
    }

    private List<Object> normalisedValueToVariableValues(List<Object> arrayValues) {
        return map(arrayValues, this::normalisedValueToVariableValue);
    }

    @NotNull
    private Map<String, Object> normalisedValueToVariableValues(Map<String, Object> objectMap) {
        Map<String, Object> output = new LinkedHashMap<>();
        objectMap.forEach((k, v) -> {
            Object value = normalisedValueToVariableValue(v);
            output.put(k, value);
        });
        return output;
    }

    private Map<String, Object> toVariableValue(ObjectValue objectValue) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<ObjectField> objectFields = objectValue.getObjectFields();
        for (ObjectField objectField : objectFields) {
            String objectFieldName = objectField.getName();
            Value<?> objectFieldValue = objectField.getValue();
            map.put(objectFieldName, toVariableValue(objectFieldValue));
        }
        return map;
    }

    @NotNull
    private List<Object> toVariableValues(List<Value> arrayValues) {
        // some values can be null (NullValue) and hence we can use Immutable Lists
        return arrayValues.stream()
                .map(this::toVariableValue)
                .collect(toList());
    }

    @Nullable
    private Object toVariableValue(Value<?> value) {
        if (value instanceof ObjectValue) {
            return toVariableValue((ObjectValue) value);
        } else if (value instanceof ArrayValue) {
            return toVariableValues(((ArrayValue) value).getValues());
        } else if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        } else if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        } else if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        } else if (value instanceof EnumValue) {
            return ((EnumValue) value).getName();
        } else if (value instanceof NullValue) {
            return null;
        }
        throw new AssertException("Should never happen. Cannot handle node of type: " + maybeClass(value));
    }

    private static Object maybeClass(Object maybe) {
        return maybe == null ? "null" : maybe.getClass();
    }

}
