package ru.craftysoft.schemaregistry.service.dao;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.SqlClient;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.schemaregistry.builder.record.StructuresRecordBuilder;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
public class StructureDaoAdapter {

    private final StructureDao dao;
    private final StructuresRecordBuilder structuresRecordBuilder;

    public Uni<Long> upsert(SqlClient sqlClient, String name) {
        var record = structuresRecordBuilder.build(name);
        return dao.upsert(sqlClient, record);
    }

    public Uni<Void> delete(SqlClient sqlClient, long id) {
        return dao.delete(sqlClient, id)
                .onItem()
                .invoke(Unchecked.consumer(count -> {
                    if (count == 0) {
                        throw new RuntimeException("Не удалось удалить структуру по id=" + id);
                    }
                }))
                .replaceWithVoid();
    }

    public Uni<StructuresRecord> getByIdOrName(@Nullable Long id, @Nullable String name) {
        return dao.getByIdOrName(id, name)
                .onItem()
                .ifNull()
                .failWith(() -> new RuntimeException("Не найдена структура по id=%s или name=%s".formatted(id, name)));
    }

    public Uni<Integer> tryDelete(SqlClient sqlClient, long id) {
        return dao.tryDelete(sqlClient, id);
    }

}
