package ru.craftysoft.schemaregistry.logic;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import ru.craftysoft.schemaregistry.configuration.ApplicationTestProfile;
import ru.craftysoft.schemaregistry.controller.StructuresController;
import ru.craftysoft.schemaregistry.model.rest.AcceptedResponseData;

import static io.restassured.RestAssured.given;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ApplicationTestProfile.class)
@TestHTTPEndpoint(StructuresController.class)
class DeleteStructureOperationTest extends DeleteOperation {

    @Test
    void process() {
        var givenCreateVersionResponse = createDefaultVersion()
                .subscribeAsCompletionStage()
                .join();
        var givenVersion = getVersionQuery()
                .fetchOptional()
                .orElseThrow();
        var givenSchemas = getSchemasQuery(givenVersion)
                .fetch();

        var response = given()
                .delete("/{id}", String.valueOf(givenCreateVersionResponse.getStructureId()))
                .then()
                .statusCode(OK)
                .extract()
                .response()
                .as(AcceptedResponseData.class);

        assertEquals(1, response.getCount());
        thenContent(givenVersion, givenSchemas);
        var thenStructure = getStructureQuery()
                .fetchOne();
        assertNull(thenStructure);
        var thenVersion = getVersionQuery()
                .fetchOne();
        assertNull(thenVersion);
        var thenSchemas = getSchemasQuery(givenVersion)
                .fetch();
        assertTrue(thenSchemas.isEmpty());
    }
}