package ru.craftysoft.schemaregistry.logic;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.craftysoft.schemaregistry.configuration.ApplicationTestProfile;
import ru.craftysoft.schemaregistry.controller.StructuresController;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

import static io.restassured.RestAssured.given;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.OK;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.craftysoft.schemaregistry.model.jooq.Tables.VERSIONS;

@QuarkusTest
@TestProfile(ApplicationTestProfile.class)
@TestHTTPEndpoint(StructuresController.class)
class GetVersionOperationTest extends OperationTest {

    @ParameterizedTest
    @EnumSource(QueryParamsByVersion.class)
    void process(QueryParamsByVersion queryParamsByVersion) throws IOException {
        var givenVersion = givenVersion();

        var response = given()
                .queryParams(queryParamsByVersion.function.apply(givenVersion))
                .get("/versions")
                .then()
                .statusCode(OK)
                .extract()
                .response();

        thenResponseContent(response);
    }

    //quarkus не умеет работать с @MethodSource (https://github.com/quarkusio/quarkus/issues/21031)
    @RequiredArgsConstructor
    private enum QueryParamsByVersion {
        PARAMS1(version -> Map.of("versionId", String.valueOf(version.getId()))),
        PARAMS2(version -> Map.of(
                "structureId", String.valueOf(version.getStructureId()),
                "versionName", version.getName()
        )),
        PARAMS3(version -> Map.of(
                "structureName", STRUCTURE_NAME,
                "versionName", version.getName()
        )),
        ;

        private final Function<VersionsRecord, Map<String, String>> function;
    }

    @Nonnull
    private VersionsRecord givenVersion() {
        return createDefaultVersion()
                .map(createdResponseData -> testDslContext.selectFrom(VERSIONS)
                        .where(VERSIONS.STRUCTURE_ID.eq(createdResponseData.getStructureId()))
                        .fetchOptional()
                        .orElseThrow())
                .subscribeAsCompletionStage()
                .join();
    }

    private void thenResponseContent(Response response) throws IOException {
        var paths = paths();
        try (var inputStream = response.asInputStream();
             var zipInputStream = new ZipInputStream(inputStream)) {
            while (true) {
                var zipEntry = zipInputStream.getNextEntry();
                if (zipEntry == null) {
                    break;
                }
                if (zipEntry.isDirectory()) {
                    continue;
                }
                assertTrue(paths.contains(zipEntry.getName()));
            }
        }
    }

}
