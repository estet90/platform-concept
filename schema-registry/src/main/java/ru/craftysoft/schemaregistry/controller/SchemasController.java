package ru.craftysoft.schemaregistry.controller;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.ResponseStatus;
import ru.craftysoft.schemaregistry.logic.GetSchemaOperation;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;

@Path("/schemas")
@ApplicationScoped
@RequiredArgsConstructor
public class SchemasController implements SchemasApi {

    private final GetSchemaOperation getSchemaOperation;

    @ResponseStatus(OK)
    @Override
    public Uni<String> getSchema(Long schemaId, String schemaPath, String versionName, String structureName) {
        if (schemaId == null && schemaPath == null && versionName == null && structureName == null) {
            throw new RuntimeException("Хотя бы один из параметров должен быть заполнен");
        }
        if (schemaId == null && (schemaPath == null || versionName == null || structureName == null)) {
            throw new RuntimeException("Хотя бы один из параметров должен быть заполнен");
        }
        return getSchemaOperation.process(schemaId, schemaPath, versionName, structureName);
    }
}
