package ru.craftysoft.schemaregistry.logic;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.craftysoft.schemaregistry.configuration.ApplicationTestProfile;
import ru.craftysoft.schemaregistry.controller.SchemasController;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;
import static ru.craftysoft.schemaregistry.model.jooq.Tables.SCHEMAS;

@QuarkusTest
@TestProfile(ApplicationTestProfile.class)
@TestHTTPEndpoint(SchemasController.class)
class GetSchemaOperationTest extends OperationTest {

    private static final String SCHEMA_PATH = paths().iterator().next();

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void process(boolean findById) {
        var givenCreateVersionResponse = createDefaultVersion()
                .subscribeAsCompletionStage()
                .join();
        var queryParams = new HashMap<String, String>();
        if (findById) {
            var schemaId = testDslContext.select(SCHEMAS.ID)
                    .from(SCHEMAS)
                    .where(
                            SCHEMAS.PATH.eq(SCHEMA_PATH),
                            SCHEMAS.VERSION_ID.eq(givenCreateVersionResponse.getVersionId())
                    )
                    .fetchOptional()
                    .orElseThrow()
                    .get(SCHEMAS.ID);
            queryParams.put("schemaId", String.valueOf(schemaId));
        } else {
            queryParams.put("schemaPath", SCHEMA_PATH);
            queryParams.put("structureName", STRUCTURE_NAME);
            queryParams.put("versionName", VERSION_NAME);
        }

        given()
                .queryParams(queryParams)
                .get("/")
                .then()
                .statusCode(OK);
    }
}