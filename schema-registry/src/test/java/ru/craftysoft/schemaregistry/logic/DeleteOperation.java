package ru.craftysoft.schemaregistry.logic;

import org.jooq.Result;
import org.jooq.SelectConditionStep;
import ru.craftysoft.schemaregistry.builder.s3.GetObjectRequestBuilder;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.SchemasRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.StructuresRecord;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.craftysoft.schemaregistry.model.jooq.Tables.*;

public class DeleteOperation extends OperationTest {

    @Inject
    protected GetObjectRequestBuilder getObjectRequestBuilder;

    @Nonnull
    protected SelectConditionStep<VersionsRecord> getVersionQuery() {
        return testDslContext.selectFrom(VERSIONS)
                .where(VERSIONS.NAME.eq(VERSION_NAME));
    }

    @Nonnull
    protected SelectConditionStep<SchemasRecord> getSchemasQuery(VersionsRecord givenVersion) {
        return testDslContext.selectFrom(SCHEMAS)
                .where(SCHEMAS.VERSION_ID.eq(givenVersion.getId()));
    }

    @Nonnull
    protected SelectConditionStep<StructuresRecord> getStructureQuery() {
        return testDslContext.selectFrom(STRUCTURES)
                .where(STRUCTURES.NAME.eq(STRUCTURE_NAME));
    }

    protected void thenContent(VersionsRecord givenVersion, Result<SchemasRecord> givenSchemas) {
        var schemasLinks = givenSchemas
                .map(SchemasRecord::getLink);
        var links = new ArrayList<>(schemasLinks);
        links.add(givenVersion.getLink());
        for (var link : links) {
            var getObjectRequest = getObjectRequestBuilder.build(link);
            var exception = assertThrows(
                    CompletionException.class,
                    () -> s3.getObject(getObjectRequest, AsyncResponseTransformer.toBytes()).join()
            );
            assertTrue(exception.getCause() instanceof NoSuchKeyException);
        }
    }
}
