package ru.craftysoft.schemaregistry.service.dao;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;
import ru.craftysoft.schemaregistry.util.DbClient;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.schemaregistry.model.jooq.Tables.VERSIONS;
import static ru.craftysoft.schemaregistry.model.jooq.tables.Structures.STRUCTURES;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class StructureDao {

    private final DbClient dbClient;
    private final DSLContext dslContext;

    public Uni<Long> upsert(SqlClient sqlClient, StructuresRecord record) {
        var query = dslContext.insertInto(STRUCTURES)
                .set(record)
                .onConflict(STRUCTURES.NAME)
                .doUpdate()
                .set(STRUCTURES.UPDATED_AT, OffsetDateTime.now())
                .returning(STRUCTURES.ID);
        return DbClient.toUni(sqlClient, log, "StructureDao.upsert", query, row -> row.getLong(STRUCTURES.ID.getName()));
    }

    public Uni<Integer> delete(SqlClient sqlClient, long id) {
        var query = dslContext.deleteFrom(STRUCTURES)
                .where(STRUCTURES.ID.eq(id));
        return DbClient.execute(sqlClient, log, "StructureDao.delete", query);
    }

    public Uni<StructuresRecord> getByIdOrName(@Nullable Long id, @Nullable String name) {
        var condition = ofNullable(id)
                .map(STRUCTURES.ID::eq)
                .orElseGet(() -> STRUCTURES.NAME.eq(name));
        var query = dslContext.selectFrom(STRUCTURES)
                .where(condition);
        return dbClient.toUni(log, "StructureDao.getByIdOrName", query, row -> new StructuresRecord(
                row.getLong(STRUCTURES.ID.getName()),
                row.getString(STRUCTURES.NAME.getName()),
                row.getOffsetDateTime(STRUCTURES.CREATED_AT.getName()),
                row.getOffsetDateTime(STRUCTURES.UPDATED_AT.getName())
        ));
    }

    public Uni<Integer> tryDelete(SqlClient sqlClient, long id) {
        var query = dslContext.deleteFrom(STRUCTURES)
                .where(
                        STRUCTURES.ID.eq(id),
                        dslContext.select(VERSIONS.ID).from(VERSIONS).where(VERSIONS.STRUCTURE_ID.eq(id)).isNull()
                );
        return DbClient.execute(sqlClient, log, "tryDelete", query);
    }
}
