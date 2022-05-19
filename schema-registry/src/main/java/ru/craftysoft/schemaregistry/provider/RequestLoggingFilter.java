package ru.craftysoft.schemaregistry.provider;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.schemaregistry.util.MdcUtils.withMdcRun;

//@Priority(Integer.MIN_VALUE + 50)
@Provider
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RequestLoggingFilter implements ContainerRequestFilter {

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.schemaregistry.server.request");

    private final Vertx vertx;

    @SneakyThrows
    public void filter(ContainerRequestContext requestContext) {
        var startMillis = System.currentTimeMillis();
        requestContext.setProperty("startMillis", startMillis);
        var containerRequestContext = (ResteasyReactiveContainerRequestContext) requestContext;
        containerRequestContext.suspend();
        if (requestContext.getUriInfo().getPath().contains("/swagger")) {
            containerRequestContext.resume();
            return;
        }
        if (requestLogger.isDebugEnabled()) {
            var headers = ofNullable(containerRequestContext.getHeaders())
                    .orElseGet(MultivaluedHashMap::new);
            var url = resolveUrl(containerRequestContext);
            if (requestLogger.isTraceEnabled()) {
                var mdc = MDC.getCopyOfContextMap();
                vertx.executeBlocking((Handler<Promise<byte[]>>) event -> withMdcRun(mdc, () -> {
                    try (var bodyStream = containerRequestContext.getEntityStream()) {
                        var bytes = bodyStream.readAllBytes();
                        var hasBody = bytes != null && bytes.length > 0;
                        var body = hasBody
                                ? new String(bytes, StandardCharsets.UTF_8)
                                : "nobody";
                        requestLogger.trace("""
                                method={}
                                url={}
                                headers={}
                                body={}""", requestContext.getMethod(), url, headers, body);
                        if (hasBody) {
                            containerRequestContext.setEntityStream(new ByteArrayInputStream(bytes));
                        }
                        containerRequestContext.resume();
                        event.complete();
                    } catch (IOException e) {
                        event.fail(e);
                    }
                })).result();
            } else {
                requestLogger.debug("""
                        method={}
                        url={}
                        headers={}""", requestContext.getMethod(), url, headers);
                containerRequestContext.resume();
            }
        }
    }

    private String resolveUrl(ContainerRequestContext containerRequestContext) {
        var url = containerRequestContext.getUriInfo().getAbsolutePath().toString();
        var queryParameters = containerRequestContext.getUriInfo().getQueryParameters().entrySet().stream()
                .map(entry -> {
                    if (entry.getValue().size() == 1) {
                        return entry.getKey() + "=" + entry.getValue().get(0);
                    }
                    return entry.getValue().stream()
                            .map(value -> entry.getKey() + "=" + value)
                            .collect(Collectors.joining("&"));
                })
                .collect(Collectors.joining("&"));
        if (!queryParameters.isEmpty()) {
            return url + "?" + queryParameters;
        }
        return url;
    }
}
