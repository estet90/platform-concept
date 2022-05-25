package ru.craftysoft.schemaregistry.controller;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.ResponseStatus;
import ru.craftysoft.schemaregistry.logic.*;
import ru.craftysoft.schemaregistry.model.rest.AcceptedResponseData;
import ru.craftysoft.schemaregistry.model.rest.CreateVersionResponseData;
import ru.craftysoft.schemaregistry.model.rest.GetStructureDescriptorResponseData;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;
import java.io.File;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.*;

@Path("/structures")
@ApplicationScoped
@RequiredArgsConstructor
public class StructuresController implements StructuresApi {

    private final CreateVersionOperation createVersionOperation;
    private final DeleteVersionOperation deleteVersionOperation;
    private final DeleteStructureOperation deleteStructureOperation;
    private final GetStructureDescriptorOperation getStructureDescriptorOperation;
    private final GetVersionOperation getVersionOperation;

    @ResponseStatus(CREATED)
    @Override
    public Uni<CreateVersionResponseData> createVersion(String structureName,
                                                        String versionName,
                                                        Boolean force,
                                                        File body) {
        return createVersionOperation.process(structureName, versionName, force, body);
    }

    @ResponseStatus(ACCEPTED)
    @Override
    public Uni<AcceptedResponseData> deleteStructure(Long id) {
        return deleteStructureOperation.process(id);
    }

    @ResponseStatus(ACCEPTED)
    @Override
    public Uni<AcceptedResponseData> deleteVersion(Long id) {
        return deleteVersionOperation.process(id);
    }

    @ResponseStatus(OK)
    @Override
    public Uni<GetStructureDescriptorResponseData> getStructureDescriptor(Long id, String name) {
        if (id == null && name == null) {
            throw new RuntimeException("Хотя бы один из параметров должен быть заполнен");
        }
        return getStructureDescriptorOperation.process(id, name);
    }

    @ResponseStatus(OK)
    @ResponseHeader(name = CONTENT_DISPOSITION, value = "attachment;filename=result.zip")
    @Override
    public Uni<File> getVersion(Long structureId,
                                String structureName,
                                Long versionId,
                                String versionName) {
        if (structureId == null && structureName == null && versionId == null && versionName == null) {
            throw new RuntimeException("Хотя бы один из параметров должен быть заполнен");
        }
        if (versionId == null) {
            if ((structureId != null || structureName != null) && versionName == null || (structureId == null && structureName == null)) {
                throw new RuntimeException("Хотя бы один из параметров должен быть заполнен");
            }
        }
        return getVersionOperation.process(structureId, structureName, versionId, versionName);
    }
}
