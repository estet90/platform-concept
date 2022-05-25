package ru.craftysoft.schemaregistry.service.dao;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.sqlclient.SqlClient;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.schemaregistry.builder.intermediate.SchemaBuilder;
import ru.craftysoft.schemaregistry.builder.record.SchemasRecordBuilder;
import ru.craftysoft.schemaregistry.dto.intermediate.Schema;
import ru.craftysoft.schemaregistry.dto.intermediate.Version;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class SchemaDaoAdapter {

    private final SchemaDao dao;
    private final SchemaBuilder schemaBuilder;
    private final SchemasRecordBuilder schemasRecordBuilder;
    private final Vertx vertx;

    public Uni<String> getLink(@Nullable Long schemaId,
                               @Nullable String schemaPath,
                               @Nullable String versionName,
                               @Nullable String structureName) {
        return dao.getLink(schemaId, schemaPath, versionName, structureName)
                .onItem()
                .ifNull()
                .failWith(() -> new RuntimeException("Не найдена схема по schemaId=%s schemaPath=%s versionName=%s structureName=%s".formatted(
                        schemaId,
                        schemaPath,
                        versionName,
                        structureName
                )));
    }

    public Uni<Map.Entry<List<Long>, Set<Schema>>> create(SqlClient sqlClient, Version version, File body) {
        return vertx.executeBlocking(Uni.createFrom().item(() -> schemaBuilder.build(version, body)))
                .flatMap(schemas -> {
                    var records = schemasRecordBuilder.build(schemas);
                    return dao.create(sqlClient, records)
                            .map(ids -> Map.entry(ids, schemas));
                });
    }

    public Uni<Set<String>> getLinksByVersionId(SqlClient sqlClient, long versionId) {
        return dao.getLinksByVersionId(sqlClient, versionId);
    }

    public Uni<Set<String>> getLinksByVersions(SqlClient sqlClient, Set<VersionsRecord> versions) {
        var versionsIds = versions.stream()
                .map(VersionsRecord::getId)
                .collect(Collectors.toSet());
        return dao.getLinksByVersionsIds(sqlClient, versionsIds);
    }

    public Uni<Set<SchemasRecord>> getByVersions(Set<VersionsRecord> versions) {
        var versionsIds = versions.stream()
                .map(VersionsRecord::getId)
                .collect(Collectors.toSet());
        return dao.getByVersionsIds(versionsIds);
    }

}
