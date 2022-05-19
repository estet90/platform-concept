package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.builder.response.CreatedResponseDataBuilder;
import ru.craftysoft.schemaregistry.model.rest.CreatedResponseData;
import ru.craftysoft.schemaregistry.service.dao.SchemaDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.StructureDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.VersionDaoAdapter;
import ru.craftysoft.schemaregistry.service.s3.S3ClientAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class CreateVersionOperation {

    private final StructureDaoAdapter structureDaoAdapter;
    private final VersionDaoAdapter versionDaoAdapter;
    private final SchemaDaoAdapter schemaDaoAdapter;
    private final S3ClientAdapter s3ClientAdapter;
    private final PgPool pgPool;
    private final CreatedResponseDataBuilder responseBuilder;

    public Uni<CreatedResponseData> process(String structureName, String versionName, File body) {
        return OperationWrapper.wrap(
                log, "CreateVersionOperation.process",
                () -> SqlClientHelper.inTransactionUni(pgPool, sqlClient -> structureDaoAdapter.upsert(sqlClient, structureName)
                                .flatMap(id -> versionDaoAdapter.create(sqlClient, id, versionName))
                                .flatMap(version -> schemaDaoAdapter.create(sqlClient, version, body)
                                        .flatMap(schemas -> s3ClientAdapter.createVersion(version, body, schemas))
                                        .replaceWith(version))
                        )
                        .map(version -> responseBuilder.build(version.id(), "Схема создана успешно")),
                () -> "structureName='%s' versionName='%s'".formatted(structureName, versionName),
                response -> "id=%s".formatted(response.getId())
        );
    }

}
