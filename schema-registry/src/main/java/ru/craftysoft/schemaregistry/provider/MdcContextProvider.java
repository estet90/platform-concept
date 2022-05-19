package ru.craftysoft.schemaregistry.provider;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.slf4j.MDC;

import java.util.Map;

public class MdcContextProvider implements ThreadContextProvider {
    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        var propagate = MDC.getCopyOfContextMap();
        return () -> {
            var old = MDC.getCopyOfContextMap();
            MDC.setContextMap(propagate);
            return () -> MDC.setContextMap(old);
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            var old = MDC.getCopyOfContextMap();
            MDC.clear();
            return () -> MDC.setContextMap(old);
        };
    }

    @Override
    public String getThreadContextType() {
        return "SLF4J";
    }
}