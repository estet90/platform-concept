package ru.craftysoft.schemaregistry.builder.record;

import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StructuresRecordBuilder {

    public StructuresRecord build(String name) {
        var record = new StructuresRecord();
        record.setName(name);
        return record;
    }

}
