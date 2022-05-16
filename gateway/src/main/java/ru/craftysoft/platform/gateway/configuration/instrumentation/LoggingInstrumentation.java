package ru.craftysoft.platform.gateway.configuration.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        return new LoggingSimpleInstrumentationContext(operation, responseLogger);
    }

}
