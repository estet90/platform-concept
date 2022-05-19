package ru.craftysoft.schemaregistry.service.dao;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.sqlclient.SqlClient;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.schemaregistry.builder.record.VersionsRecordBuilder;
import ru.craftysoft.schemaregistry.dto.intermediate.Version;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
@RequiredArgsConstructor
public class VersionDaoAdapter {

    private final VersionDao dao;
    private final VersionsRecordBuilder versionsRecordBuilder;

    public Uni<Version> create(SqlClient sqlClient, long structureId, String name) {
        var record = versionsRecordBuilder.build(structureId, name);
        return dao.create(sqlClient, record)
                .map(id -> new Version(id, record.getLink()));
    }

    public Uni<String> getLink(SqlClient sqlClient, long id) {
        return dao.getLink(sqlClient, id)
                .onItem()
                .ifNull()
                .failWith(() -> new RuntimeException("Не найдена версия по id=" + id));
    }

    public Uni<Void> delete(SqlClient sqlClient, long id) {
        return dao.delete(sqlClient, id)
                .onItem()
                .invoke(Unchecked.consumer(count -> {
                    if (count == 0) {
                        throw new RuntimeException("Не удалось удалить версию с id=" + id);
                    }
                }))
                .replaceWithVoid();
    }

    public Uni<Set<VersionsRecord>> getByStructureId(SqlClient sqlClient, long structureId) {
        return dao.getByStructureId(sqlClient, structureId);
    }

    public Uni<Set<VersionsRecord>> getByStructure(StructuresRecord structure) {
        return dao.getByStructureId(structure.getId());
    }

    public Uni<String> getLink(@Nullable Long structureId,
                               @Nullable String structureName,
                               @Nullable Long versionId,
                               @Nullable String versionName) {
        return dao.getLink(structureId, structureName, versionId, versionName)
                .onItem()
                .ifNull()
                .failWith(() -> new RuntimeException("Не найдена версия по structureId=%s structureName=%s versionId=%s versionName=%s"
                        .formatted(structureId, structureName, versionId, versionName)));
    }

}
