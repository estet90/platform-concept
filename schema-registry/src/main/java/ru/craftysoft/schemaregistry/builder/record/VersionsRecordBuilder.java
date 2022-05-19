package ru.craftysoft.schemaregistry.builder.record;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class VersionsRecordBuilder {

    private final String bucket;

    public VersionsRecordBuilder(@ConfigProperty(name = "s3.bucket") String bucket) {
        this.bucket = bucket;
    }

    public VersionsRecord build(long structureId, String name) {
        var record = new VersionsRecord();
        record.setStructureId(structureId);
        record.setName(name);
        record.setLink(bucket + "/" + "version_" + UUID.randomUUID());
        return record;
    }

}
