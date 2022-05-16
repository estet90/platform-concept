package ru.craftysoft.platform.gateway.configuration.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class LoggingSimpleInstrumentationContext extends SimpleInstrumentationContext<ExecutionResult> {

    private final String operation;
    private final Logger responseLogger;

    @Override
    public void onCompleted(ExecutionResult executionResult, Throwable t) {
        if (t != null) {
            responseLogger.error("error", t);
        } else {
            if (!"IntrospectionQuery".equals(operation)) {
                if (responseLogger.isTraceEnabled()) {
                    var result = ofNullable(executionResult.toSpecification())
                            .map(JsonObject::mapFrom)
                            .map(JsonObject::toString)
                            .orElse(null);
                    responseLogger.trace("result={}", result);
                } else {
                    responseLogger.debug("result");
                }
            }
        }
    }

}
