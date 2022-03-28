package ru.craftysoft.platform.gateway.configuration;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;

public class LoggingInstrumentation extends SimpleInstrumentation {

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.platform.gateway.server.request");
    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.platform.gateway.server.response");

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
        var operation = parameters.getOperation();
        if (!"IntrospectionQuery".equals(operation)) {
            var variables = parameters.getVariables();
            if (requestLogger.isTraceEnabled()) {
                var query = parameters.getQuery();
                requestLogger.trace("""
                        operation={}
                        query={}
                        variables={}""", operation, query, variables);
            } else {
                requestLogger.debug("""
                        operation={}
                        variables={}""", operation, variables);
            }
        }

        return new SimpleInstrumentationContext<>() {
            @Override
            public void onCompleted(ExecutionResult executionResult, Throwable t) {
                if (t != null) {
                    responseLogger.error("error", t);
                } else {
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
        };
    }

}
