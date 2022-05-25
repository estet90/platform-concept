package ru.craftysoft.schemaregistry.builder.response;

import ru.craftysoft.schemaregistry.dto.intermediate.Version;
import ru.craftysoft.schemaregistry.model.rest.CreateVersionResponseData;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CreateVersionResponseDataBuilder {

    public CreateVersionResponseData build(long structureId, Version version, List<Long> schemasIds) {
        return new CreateVersionResponseData()
                .structureId(structureId)
                .versionId(version.id())
                .schemaIds(schemasIds);
    }

}
