package ru.craftysoft.schemaregistry.builder.record;

import ru.craftysoft.schemaregistry.dto.intermediate.Schema;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.stream.Stream;

@ApplicationScoped
public class SchemasRecordBuilder {

    public Stream<SchemasRecord> build(Set<Schema> schema) {
        return schema.stream()
                .map(this::build);
    }

    public SchemasRecord build(Schema schema) {
        var record = new SchemasRecord();
        record.setVersionId(schema.versionId());
        record.setPath(schema.path());
        record.setLink(schema.link());
        return record;
    }
}
