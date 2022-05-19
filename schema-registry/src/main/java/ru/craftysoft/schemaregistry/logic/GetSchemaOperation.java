package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.service.dao.SchemaDaoAdapter;
import ru.craftysoft.schemaregistry.service.s3.S3ClientAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class GetSchemaOperation {

    private final SchemaDaoAdapter schemaDaoAdapter;
    private final S3ClientAdapter s3ClientAdapter;

    public Uni<String> process(@Nullable Long schemaId,
                               @Nullable String schemaPath,
                               @Nullable String versionName,
                               @Nullable String structureName) {
        return OperationWrapper.wrap(
                log, "GetSchemaByIdOperation.process",
                () -> schemaDaoAdapter.getLink(schemaId, schemaPath, versionName, structureName)
                        .flatMap(s3ClientAdapter::getSchema),
                () -> "schemaId=%s schemaPath=%s versionName=%s structureName=%s".formatted(
                        schemaId,
                        schemaPath,
                        versionName,
                        structureName
                ), null
        );
    }

}
