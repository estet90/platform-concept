package ru.craftysoft.schemaregistry.builder.response;

import ru.craftysoft.schemaregistry.model.rest.ErrorResponseData;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ErrorResponseDataBuilder {

    public ErrorResponseData build(Exception e) {
        return new ErrorResponseData()
                .message(e.getMessage());
    }

}
