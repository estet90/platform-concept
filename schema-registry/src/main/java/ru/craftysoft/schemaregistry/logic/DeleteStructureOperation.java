package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.builder.response.AcceptedResponseDataBuilder;
import ru.craftysoft.schemaregistry.model.rest.AcceptedResponseData;
import ru.craftysoft.schemaregistry.service.dao.SchemaDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.StructureDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.VersionDaoAdapter;
import ru.craftysoft.schemaregistry.service.s3.S3ClientAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class DeleteStructureOperation {

    private final StructureDaoAdapter structureDaoAdapter;
    private final VersionDaoAdapter versionDaoAdapter;
    private final SchemaDaoAdapter schemaDaoAdapter;
    private final S3ClientAdapter s3ClientAdapter;
    private final PgPool pgPool;
    private final AcceptedResponseDataBuilder responseBuilder;

    public Uni<AcceptedResponseData> process(long id) {
        return OperationWrapper.wrap(
                log, "DeleteStructureOperation.process",
                () -> SqlClientHelper.inTransactionUni(pgPool, sqlClient -> versionDaoAdapter.getByStructureId(sqlClient, id)
                                .flatMap(versions -> schemaDaoAdapter.getLinksByVersions(sqlClient, versions)
                                        .flatMap(links -> structureDaoAdapter.delete(sqlClient, id)
                                                .flatMap(v -> s3ClientAdapter.deleteFiles(versions, links))
                                        )
                                )
                        )
                        .map(v -> responseBuilder.build(1, "Структура успешно удалена")),
                () -> "id=" + id,
                response -> "count=" + response.getCount()
        );
    }

}
