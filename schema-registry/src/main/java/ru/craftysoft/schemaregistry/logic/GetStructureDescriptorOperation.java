package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.builder.response.GetStructureDescriptorResponseDataBuilder;
import ru.craftysoft.schemaregistry.model.rest.GetStructureDescriptorResponseData;
import ru.craftysoft.schemaregistry.service.dao.SchemaDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.StructureDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.VersionDaoAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class GetStructureDescriptorOperation {

    private final StructureDaoAdapter structureDaoAdapter;
    private final VersionDaoAdapter versionDaoAdapter;
    private final SchemaDaoAdapter schemaDaoAdapter;
    private final GetStructureDescriptorResponseDataBuilder responseBuilder;

    public Uni<GetStructureDescriptorResponseData> process(@Nullable Long id, @Nullable String name) {
        return OperationWrapper.wrap(
                log, "GetStructureDescriptorOperation.process",
                () -> structureDaoAdapter.getByIdOrName(id, name)
                        .flatMap(structuresRecord -> versionDaoAdapter.getByStructure(structuresRecord)
                                .flatMap(versionsRecords -> schemaDaoAdapter.getByVersions(versionsRecords)
                                        .map(schemasRecords -> responseBuilder.build(structuresRecord, versionsRecords, schemasRecords))
                                )
                        )
                ,
                () -> "id=%s name=%s".formatted(id, name),
                null
        );
    }

}
