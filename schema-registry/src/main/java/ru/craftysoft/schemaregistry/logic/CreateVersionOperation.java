package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.builder.response.CreateVersionResponseDataBuilder;
import ru.craftysoft.schemaregistry.model.rest.CreateVersionResponseData;
import ru.craftysoft.schemaregistry.service.dao.SchemaDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.StructureDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.VersionDaoAdapter;
import ru.craftysoft.schemaregistry.service.s3.S3ClientAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;

import static java.util.Optional.ofNullable;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class CreateVersionOperation {

    private final StructureDaoAdapter structureDaoAdapter;
    private final VersionDaoAdapter versionDaoAdapter;
    private final SchemaDaoAdapter schemaDaoAdapter;
    private final S3ClientAdapter s3ClientAdapter;
    private final PgPool pgPool;
    private final CreateVersionResponseDataBuilder responseBuilder;

    public Uni<CreateVersionResponseData> process(String structureName, String versionName, boolean force, File body) {
        return OperationWrapper.wrap(
                log, "CreateVersionOperation.process",
                () -> SqlClientHelper.inTransactionUni(pgPool, sqlClient -> structureDaoAdapter.upsert(sqlClient, structureName)
                        .flatMap(structureId -> {
                            var createVersionUni = versionDaoAdapter.create(sqlClient, structureId, versionName)
                                    .flatMap(version -> schemaDaoAdapter.create(sqlClient, version, body)
                                            .flatMap(schemasWithIds -> s3ClientAdapter.createVersion(version, body, schemasWithIds.getValue())
                                                    .map(ignored -> responseBuilder.build(structureId, version, schemasWithIds.getKey())))
                                    );
                            return force
                                    ? versionDaoAdapter.get(sqlClient, structureId, versionName)
                                    .flatMap(version -> ofNullable(version)
                                            .map(v -> schemaDaoAdapter.getLinksByVersionId(sqlClient, v.getId())
                                                    .flatMap(schemasLinks -> versionDaoAdapter.delete(sqlClient, v.getId())
                                                            .flatMap(ignored -> s3ClientAdapter.deleteFiles(v.getLink(), schemasLinks))
                                                            .flatMap(ignored -> createVersionUni)))
                                            .orElse(createVersionUni))
                                    : createVersionUni;
                        })),
                () -> "structureName='%s' versionName='%s' force='%s'".formatted(structureName, versionName, force),
                response -> "structureId=%s versionId=%s schemasIds=%s"
                        .formatted(response.getStructureId(), response.getVersionId(), response.getSchemaIds())
        );
    }

}
