package ru.craftysoft.schemaregistry.provider;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import ru.craftysoft.schemaregistry.builder.response.ErrorResponseDataBuilder;
import ru.craftysoft.schemaregistry.model.rest.ErrorResponseData;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Provider
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class HttpExceptionHandler {

    private final ErrorResponseDataBuilder errorResponseDataBuilder;

    @ServerExceptionMapper(Exception.class)
    public Uni<RestResponse<ErrorResponseData>> mapException(Exception exception, ContainerRequestContext requestContext) {
        log.error("HttpExceptionHandler.mapException.thrown", exception);
        var errorPayload = errorResponseDataBuilder.build(exception);
        return Uni.createFrom()
                .item(RestResponse.ResponseBuilder
                        .create(RestResponse.Status.fromStatusCode(500), errorPayload)
                        .header(CONTENT_TYPE, APPLICATION_JSON)
                        .build()
                );
    }

}
