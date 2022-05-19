package ru.craftysoft.schemaregistry.builder.response;

import ru.craftysoft.schemaregistry.model.rest.CreatedResponseData;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreatedResponseDataBuilder {

    public CreatedResponseData build(long id, String message) {
        return new CreatedResponseData()
                .id(id)
                .message(message);
    }

}
