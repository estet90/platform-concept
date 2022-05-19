package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.builder.response.AcceptedResponseDataBuilder;
import ru.craftysoft.schemaregistry.model.rest.AcceptedResponseData;
import ru.craftysoft.schemaregistry.service.dao.SchemaDaoAdapter;
import ru.craftysoft.schemaregistry.service.dao.VersionDaoAdapter;
import ru.craftysoft.schemaregistry.service.s3.S3ClientAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class DeleteVersionOperation {

    private final VersionDaoAdapter versionDaoAdapter;
    private final SchemaDaoAdapter schemaDaoAdapter;
    private final S3ClientAdapter s3ClientAdapter;
    private final PgPool pgPool;
    private final AcceptedResponseDataBuilder responseBuilder;

    public Uni<AcceptedResponseData> process(long id) {
        return OperationWrapper.wrap(
                log, "DeleteVersionOperation.process",
                () -> SqlClientHelper.inTransactionUni(pgPool, sqlClient -> Uni.combine().all()
                        .unis(
                                versionDaoAdapter.getLink(sqlClient, id),
                                schemaDaoAdapter.getLinksByVersionId(sqlClient, id)
                        )
                        .combinedWith((versionLink, schemasLinks) -> versionDaoAdapter.delete(sqlClient, id)
                                .flatMap(v -> s3ClientAdapter.deleteFiles(versionLink, schemasLinks))
                                .map(v -> responseBuilder.build(1, "Версия успешно удалена"))
                        )
                        .flatMap(u -> u)
                ),
                () -> "id=" + id,
                response -> "count=" + response.getCount()
        );
    }

}
