package ru.craftysoft.schemaregistry.builder.response;

import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;
import ru.craftysoft.schemaregistry.model.rest.GetStructureDescriptorResponseData;
import ru.craftysoft.schemaregistry.model.rest.Schema;
import ru.craftysoft.schemaregistry.model.rest.Version;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetStructureDescriptorResponseDataBuilder {

    public GetStructureDescriptorResponseData build(StructuresRecord structure,
                                                    Set<VersionsRecord> versions,
                                                    Set<SchemasRecord> schemas) {
        return new GetStructureDescriptorResponseData()
                .id(structure.getId())
                .name(structure.getName())
                .createdAt(structure.getCreatedAt())
                .updatedAt(structure.getUpdatedAt())
                .versions(buildVersions(versions, schemas));
    }

    private Set<Version> buildVersions(Set<VersionsRecord> versions, Set<SchemasRecord> schemas) {
        var schemasByVersionId = schemas.stream()
                .collect(Collectors.groupingBy(SchemasRecord::getVersionId));
        return versions.stream()
                .map(version -> buildVersion(schemasByVersionId, version))
                .collect(Collectors.toSet());
    }

    private Version buildVersion(Map<Long, List<SchemasRecord>> schemasByVersionId, VersionsRecord version) {
        return new Version()
                .id(version.getId())
                .name(version.getName())
                .link(version.getLink())
                .createdAt(version.getCreatedAt())
                .schemas(buildSchemas(schemasByVersionId, version));
    }

    private Set<Schema> buildSchemas(Map<Long, List<SchemasRecord>> schemasByVersionId, VersionsRecord version) {
        return schemasByVersionId.get(version.getId()).stream()
                .map(this::buildSchema)
                .collect(Collectors.toSet());
    }

    private Schema buildSchema(SchemasRecord schema) {
        return new Schema()
                .id(schema.getId())
                .path(schema.getPath())
                .link(schema.getLink());
    }

}
