package ru.craftysoft.schemaregistry.service.dao;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlClient;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Query;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;
import ru.craftysoft.schemaregistry.util.DbClient;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.stream.Stream;

import static ru.craftysoft.schemaregistry.model.jooq.tables.Schemas.SCHEMAS;
import static ru.craftysoft.schemaregistry.model.jooq.tables.Structures.STRUCTURES;
import static ru.craftysoft.schemaregistry.model.jooq.tables.Versions.VERSIONS;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class SchemaDao {

    private final DbClient dbClient;
    private final DSLContext dslContext;

    public Uni<String> getLink(@Nullable Long schemaId,
                               @Nullable String schemaPath,
                               @Nullable String versionName,
                               @Nullable String structureName) {
        var query = resolveGetLinkQuery(schemaId, schemaPath, versionName, structureName);
        return dbClient.toUni(log, "SchemaDao.getLink", query, row -> row.getString(SCHEMAS.LINK.getName()));
    }

    private Query resolveGetLinkQuery(@Nullable Long schemaId,
                                      @Nullable String schemaPath,
                                      @Nullable String versionName,
                                      @Nullable String structureName) {
        var query = dslContext.select(SCHEMAS.LINK)
                .from(SCHEMAS);
        if (schemaId != null) {
            return query.where(SCHEMAS.ID.eq(schemaId));
        }
        return query
                .join(VERSIONS).on(VERSIONS.ID.eq(SCHEMAS.VERSION_ID).and(VERSIONS.NAME.eq(versionName)))
                .join(STRUCTURES).on(STRUCTURES.ID.eq(VERSIONS.STRUCTURE_ID).and(STRUCTURES.NAME.eq(structureName)))
                .where(SCHEMAS.PATH.eq(schemaPath));

    }

    public Uni<Set<String>> getLinksByVersionId(SqlClient sqlClient, long versionId) {
        var query = dslContext.select(SCHEMAS.LINK)
                .from(SCHEMAS)
                .where(SCHEMAS.VERSION_ID.eq(versionId));
        return DbClient.toUniOfSet(sqlClient, log, "SchemaDao.getLinksByVersionId", query, row -> row.getString(SCHEMAS.LINK.getName()));
    }

    public Uni<Set<String>> getLinksByVersionsIds(SqlClient sqlClient, Set<Long> versionsIds) {
        var query = dslContext.select(SCHEMAS.LINK)
                .from(SCHEMAS)
                .where(SCHEMAS.VERSION_ID.in(versionsIds));
        return DbClient.toUniOfSet(sqlClient, log, "SchemaDao.getLinksByVersionsIds", query, row -> row.getString(SCHEMAS.LINK.getName()));
    }

    public Uni<Integer> create(SqlClient sqlClient, Stream<SchemasRecord> records) {
        var args = records
                .map(record -> Tuple.of(
                        record.getVersionId(),
                        record.getPath(),
                        record.getLink()
                ))
                .toList();
        var sql = """
                INSERT INTO schema_registry.schemas (version_id, path, link)
                VALUES ($1, $2, $3)""";
        return DbClient.executeBatch(sqlClient, log, "SchemaDao.create", sql, args);
    }

    public Uni<Set<SchemasRecord>> getByVersionsIds(Set<Long> versionsIds) {
        var query = dslContext.selectFrom(SCHEMAS)
                .where(SCHEMAS.VERSION_ID.in(versionsIds));
        return dbClient.toUniOfSet(log, "SchemaDao.getLinksByVersionsIds", query, row -> new SchemasRecord(
                row.getLong(SCHEMAS.ID.getName()),
                row.getString(SCHEMAS.PATH.getName()),
                row.getLong(SCHEMAS.VERSION_ID.getName()),
                row.getString(SCHEMAS.LINK.getName())
        ));
    }
}
