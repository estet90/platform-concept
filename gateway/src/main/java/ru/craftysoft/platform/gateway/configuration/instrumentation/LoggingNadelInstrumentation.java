package ru.craftysoft.platform.gateway.configuration.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.language.AstPrinter;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class LoggingNadelInstrumentation implements NadelInstrumentation {

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.platform.gateway.server.request");
    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.platform.gateway.server.response");

    @Nonnull
    @Override
    public CompletableFuture<InstrumentationContext<ExecutionResult>> beginExecute(@Nonnull NadelInstrumentationExecuteOperationParameters parameters) {
        var operation = parameters.getNormalizedOperation().getOperationName();
        if (!"IntrospectionQuery".equals(operation)) {
            var variables = parameters.getVariables();
            if (requestLogger.isTraceEnabled()) {
                var query = AstPrinter.printAst(parameters.getDocument());
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

        var context = new LoggingSimpleInstrumentationContext(operation, responseLogger);
        return CompletableFuture.completedFuture(context);
    }

}
