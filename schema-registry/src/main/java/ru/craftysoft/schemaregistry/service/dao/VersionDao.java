package ru.craftysoft.schemaregistry.service.dao;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Query;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;
import ru.craftysoft.schemaregistry.util.DbClient;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

import static ru.craftysoft.schemaregistry.model.jooq.tables.Structures.STRUCTURES;
import static ru.craftysoft.schemaregistry.model.jooq.tables.Versions.VERSIONS;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class VersionDao {

    private final DbClient dbClient;
    private final DSLContext dslContext;

    public Uni<Long> create(SqlClient sqlClient, VersionsRecord record) {
        var query = dslContext.insertInto(VERSIONS)
                .set(record)
                .returning(VERSIONS.ID);
        return DbClient.toUni(sqlClient, log, "VersionDao.create", query, row -> row.getLong(VERSIONS.ID.getName()));
    }

    public Uni<String> getLink(SqlClient sqlClient, long id) {
        var query = dslContext.select(VERSIONS.LINK)
                .from(VERSIONS)
                .where(VERSIONS.ID.eq(id));
        return DbClient.toUni(sqlClient, log, "VersionDao.getLink", query, row -> row.getString(VERSIONS.LINK.getName()));
    }

    public Uni<Integer> delete(SqlClient sqlClient, long id) {
        var query = dslContext.deleteFrom(VERSIONS)
                .where(VERSIONS.ID.eq(id));
        return DbClient.execute(sqlClient, log, "VersionDao.delete", query);
    }

    public Uni<Set<VersionsRecord>> getByStructureId(SqlClient sqlClient, long structureId) {
        var query = dslContext
                .select(
                        VERSIONS.LINK,
                        VERSIONS.ID
                )
                .from(VERSIONS)
                .where(VERSIONS.STRUCTURE_ID.eq(structureId));
        return DbClient.toUniOfSet(sqlClient, log, "VersionDao.getByStructureId", query, row -> new VersionsRecord(
                row.getLong(VERSIONS.ID.getName()),
                null,
                null,
                row.getString(VERSIONS.LINK.getName()),
                null
        ));
    }

    public Uni<Set<VersionsRecord>> getByStructureId(long structureId) {
        var query = dslContext
                .selectFrom(VERSIONS)
                .where(VERSIONS.STRUCTURE_ID.eq(structureId));
        return dbClient.toUniOfSet(log, "VersionDao.getByStructureId", query, row -> new VersionsRecord(
                row.getLong(VERSIONS.ID.getName()),
                row.getString(VERSIONS.NAME.getName()),
                null,
                row.getString(VERSIONS.LINK.getName()),
                row.getOffsetDateTime(VERSIONS.CREATED_AT.getName())
        ));
    }

    public Uni<String> getLink(@Nullable Long structureId,
                               @Nullable String structureName,
                               @Nullable Long versionId,
                               @Nullable String versionName) {
        var query = resolveGetLinkQuery(structureId, structureName, versionId, versionName);
        return dbClient.toUni(log, "VersionDao.getLink", query, row -> row.getString(VERSIONS.LINK.getName()));
    }

    private Query resolveGetLinkQuery(@Nullable Long structureId,
                                      @Nullable String structureName,
                                      @Nullable Long versionId,
                                      @Nullable String versionName) {
        var query = dslContext.select(VERSIONS.LINK)
                .from(VERSIONS);
        if (versionId != null) {
            return query.where(VERSIONS.ID.eq(versionId));
        }
        if (structureId != null) {
            return query.where(
                    VERSIONS.NAME.eq(versionName),
                    VERSIONS.STRUCTURE_ID.eq(structureId)
            );
        }
        return query.join(STRUCTURES).on(STRUCTURES.ID.eq(VERSIONS.STRUCTURE_ID))
                .where(
                        VERSIONS.NAME.eq(versionName),
                        STRUCTURES.NAME.eq(structureName)
                );
    }

}
