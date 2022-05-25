package ru.craftysoft.schemaregistry.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.List;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Provider
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ResponseLoggingFilter implements ContainerResponseFilter {

    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.schemaregistry.server.response");

    private final ObjectMapper objectMapper;

    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (requestContext.getUriInfo().getPath().contains("/swagger")) {
            return;
        }
        var time = System.currentTimeMillis() - (Long) requestContext.getProperty("startMillis");
        requestContext.removeProperty("startMillis");
        if (responseLogger.isDebugEnabled()) {
            int status = responseContext.getStatus();
            var headers = responseContext.getHeaders();
            if (responseLogger.isTraceEnabled()) {
                var body = extractBody(responseContext);
                responseLogger.trace("""
                        status={}
                        headers={}
                        body={}
                        time={}""", status, headers, body, time);
            } else {
                responseLogger.debug("""
                        status={}
                        headers={}
                        time={}""", status, headers, time);
            }
        }
    }

    @Nonnull
    private Object extractBody(ContainerResponseContext responseContext) {
        try {
            return ofNullable(responseContext.getEntity())
                    .map(entity -> responseContext.getHeaders().getOrDefault(CONTENT_TYPE, List.of())
                            .stream()
                            .map(String.class::cast)
                            .filter(contentType -> contentType.contains(APPLICATION_JSON))
                            .findFirst()
                            .map(contentType -> {
                                try {
                                    return objectMapper.writeValueAsString(responseContext.getEntity());
                                } catch (Exception e) {
                                    log.error("thrown", e);
                                    return "thrown %s".formatted(e.getMessage());
                                }
                            })
                            .map(Object.class::cast)
                            .orElse(entity)
                    ).orElse("nobody");
        } catch (Exception e) {
            log.error("thrown", e);
            return "thrown %s".formatted(e.getMessage());
        }
    }
}
