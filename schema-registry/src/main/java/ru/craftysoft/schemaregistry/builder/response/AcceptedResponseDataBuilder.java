package ru.craftysoft.schemaregistry.builder.response;

import ru.craftysoft.schemaregistry.model.rest.AcceptedResponseData;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AcceptedResponseDataBuilder {

    public AcceptedResponseData build(int count, String message) {
        return new AcceptedResponseData()
                .count(count)
                .message(message);
    }

}
