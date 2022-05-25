package ru.craftysoft.schemaregistry.logic;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.craftysoft.schemaregistry.builder.s3.GetObjectRequestBuilder;
import ru.craftysoft.schemaregistry.configuration.ApplicationTestProfile;
import ru.craftysoft.schemaregistry.controller.StructuresController;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;
import ru.craftysoft.schemaregistry.model.rest.CreateVersionResponseData;
import ru.craftysoft.schemaregistry.model.rest.ErrorResponseData;
import ru.craftysoft.schemaregistry.service.dao.VersionDao;
import ru.craftysoft.schemaregistry.service.s3.S3Client;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.internal.multipart.MultiPartInternal.OCTET_STREAM;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.INTERNAL_SERVER_ERROR;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.craftysoft.schemaregistry.model.jooq.Tables.*;

@QuarkusTest
@TestProfile(ApplicationTestProfile.class)
@TestHTTPEndpoint(StructuresController.class)
class CreateVersionOperationTest extends OperationTest {

    @Inject
    GetObjectRequestBuilder getObjectRequestBuilder;

    @InjectSpy
    S3Client s3Client;

    @InjectSpy
    VersionDao versionDao;

    @Test
    void process() throws IOException {
        var response = httpRequest()
                .then()
                .statusCode(OK)
                .extract()
                .response()
                .as(CreateVersionResponseData.class);

        thenResponse(response);
        verify(s3Client, never()).deleteFiles(any());
        verify(versionDao, never()).delete(any(), anyLong());
    }

    @Test
    void processForce() throws IOException {
        httpRequest();
        var response = given()
                .queryParams(
                        "structureName", STRUCTURE_NAME,
                        "versionName", VERSION_NAME,
                        "force", "true"
                )
                .body(new File(UPLOADING_FILE_PATH))
                .header(new Header(CONTENT_TYPE, OCTET_STREAM))
                .post("/versions")
                .then()
                .statusCode(OK)
                .extract()
                .response()
                .as(CreateVersionResponseData.class);

        thenResponse(response);
        verify(s3Client, times(1)).deleteFiles(any());
        verify(versionDao, times(1)).delete(any(), anyLong());
    }

    @Test
    void processDuplicateVersion() {
        httpRequest();
        var response = httpRequest()
                .then()
                .statusCode(INTERNAL_SERVER_ERROR)
                .extract()
                .response()
                .as(ErrorResponseData.class);

        assertNotNull(response.getMessage());
    }

    private Response httpRequest() {
        return given()
                .queryParams(
                        "structureName", STRUCTURE_NAME,
                        "versionName", VERSION_NAME
                )
                .body(new File(UPLOADING_FILE_PATH))
                .header(new Header(CONTENT_TYPE, OCTET_STREAM))
                .post("/versions");
    }

    private void thenResponse(CreateVersionResponseData response) throws IOException {
        var schemasIds = new HashSet<>(response.getSchemaIds());
        var paths = paths();
        testDslContext.selectFrom(SCHEMAS)
                .where(SCHEMAS.ID.in(schemasIds))
                .fetch()
                .forEach(schema -> thenSchema(response, schemasIds, paths, schema));
        var version = testDslContext.selectFrom(VERSIONS)
                .where(VERSIONS.ID.eq(response.getVersionId()))
                .fetchOptional()
                .orElseThrow();
        thenVersion(response, version);
        var structure = testDslContext.selectFrom(STRUCTURES)
                .where(STRUCTURES.ID.eq(response.getStructureId()))
                .fetchOptional()
                .orElseThrow();
        assertEquals(STRUCTURE_NAME, structure.getName());
    }

    private void thenSchema(CreateVersionResponseData response, HashSet<Long> schemasIds, Set<String> paths, SchemasRecord schema) {
        assertTrue(schemasIds.contains(schema.getId()));
        assertTrue(paths.contains(schema.getPath()));
        assertEquals(response.getVersionId(), schema.getVersionId());
        var getObjectRequest = getObjectRequestBuilder.build(schema.getLink());
        s3.getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenAccept(Assertions::assertNotNull);
    }

    private void thenVersion(CreateVersionResponseData response, VersionsRecord version) {
        assertEquals(VERSION_NAME, version.getName());
        assertEquals(response.getStructureId(), version.getStructureId());
        var getObjectRequest = getObjectRequestBuilder.build(version.getLink());
        s3.getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .thenAccept(Assertions::assertNotNull);
    }
}