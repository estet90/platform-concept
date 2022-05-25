package ru.craftysoft.schemaregistry.logic;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.craftysoft.schemaregistry.configuration.ApplicationTestProfile;
import ru.craftysoft.schemaregistry.controller.StructuresController;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;
import ru.craftysoft.schemaregistry.model.rest.CreateVersionResponseData;
import ru.craftysoft.schemaregistry.model.rest.GetStructureDescriptorResponseData;
import ru.craftysoft.schemaregistry.model.rest.Schema;
import ru.craftysoft.schemaregistry.model.rest.Version;

import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;
import static org.junit.jupiter.api.Assertions.*;
import static ru.craftysoft.schemaregistry.model.jooq.Tables.*;

@QuarkusTest
@TestProfile(ApplicationTestProfile.class)
@TestHTTPEndpoint(StructuresController.class)
class GetStructureDescriptorOperationTest extends OperationTest {

    @ParameterizedTest
    @EnumSource(QueryParamExtractor.class)
    void process(QueryParamExtractor queryParamExtractor) {
        var givenCreateVersionResponse = createDefaultVersion()
                .subscribeAsCompletionStage()
                .join();
        var structureRecord = testDslContext.selectFrom(STRUCTURES)
                .where(STRUCTURES.NAME.eq(STRUCTURE_NAME))
                .fetchOptional()
                .orElseThrow();
        var versionsRecords = testDslContext.selectFrom(VERSIONS)
                .where(VERSIONS.NAME.eq(VERSION_NAME))
                .fetch();
        var versionsIds = versionsRecords.map(VersionsRecord::getId);
        var schemasRecords = testDslContext.selectFrom(SCHEMAS)
                .where(SCHEMAS.VERSION_ID.in(versionsIds))
                .fetch();
        var queryParams = queryParamExtractor.extractor.apply(givenCreateVersionResponse);

        var response = given()
                .queryParams(queryParams)
                .get("/")
                .then()
                .statusCode(OK)
                .extract()
                .as(GetStructureDescriptorResponseData.class);

        thenStructure(structureRecord, response);
        var versions = response.getVersions();
        thenVersions(versionsRecords, versions);
        thenSchemas(schemasRecords, versions);
    }

    private void thenStructure(StructuresRecord structureRecord, GetStructureDescriptorResponseData response) {
        assertEquals(structureRecord.getId(), response.getId());
        assertEquals(structureRecord.getName(), response.getName());
        assertEquals(structureRecord.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC), response.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC));
        assertEquals(structureRecord.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC), response.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC));
    }

    private void thenVersions(Result<VersionsRecord> versionsRecords, Set<Version> versions) {
        assertFalse(versions.isEmpty());
        var versionsByIds = versions.stream()
                .collect(Collectors.toMap(Version::getId, version -> version));
        versionsRecords.forEach(versionRecord -> {
            var version = versionsByIds.get(versionRecord.getId());
            assertNotNull(version);
            assertEquals(versionRecord.getId(), version.getId());
            assertEquals(versionRecord.getName(), version.getName());
            assertEquals(versionRecord.getLink(), version.getLink());
            assertEquals(versionRecord.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC), version.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC));
            assertFalse(version.getSchemas().isEmpty());
        });
    }

    private void thenSchemas(Result<SchemasRecord> schemasRecords, Set<Version> versions) {
        var schemasByIds = versions.stream()
                .map(Version::getSchemas)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Schema::getId, schema -> schema));
        schemasRecords.forEach(schemaRecord -> {
            var schema = schemasByIds.get(schemaRecord.getId());
            assertNotNull(schema);
            assertEquals(schemaRecord.getId(), schema.getId());
            assertEquals(schemaRecord.getPath(), schema.getPath());
            assertEquals(schemaRecord.getLink(), schema.getLink());
        });
    }

    //quarkus не умеет работать с @MethodSource (https://github.com/quarkusio/quarkus/issues/21031)
    @RequiredArgsConstructor
    private enum QueryParamExtractor {
        PARAMETER1(responseData -> Map.of("id", responseData.getStructureId())),
        PARAMETER2(responseData -> Map.of("name", STRUCTURE_NAME)),
        ;

        private final Function<CreateVersionResponseData, Map<String, Object>> extractor;
    }
}