package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import ru.craftysoft.schemaregistry.configuration.TestDslContext;
import ru.craftysoft.schemaregistry.model.rest.CreateVersionResponseData;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static ru.craftysoft.schemaregistry.model.jooq.Tables.STRUCTURES;

class OperationTest {

    protected static final String UPLOADING_FILE_PATH = "src/test/resources/sample/test.zip";
    protected static final String VERSION_NAME = "v1";
    protected static final String STRUCTURE_NAME = "s1";

    @Inject
    protected S3AsyncClient s3;

    @Inject
    protected TestDslContext testDslContext;

    @ConfigProperty(name = "s3.bucket")
    protected String bucket;

    @Inject
    protected CreateVersionOperation createVersionOperation;

    @BeforeEach
    protected void setUp() {
        var createBucketRequest = CreateBucketRequest.builder()
                .bucket(bucket)
                .build();
        s3.createBucket(createBucketRequest).join();
    }

    @AfterEach
    protected void cleanUp() {
        try {
            Files.delete(Path.of("result.zip"));
        } catch (IOException e) {
            //noop
        }
        try (var query = testDslContext.deleteFrom(STRUCTURES)) {
            query.execute();
        }
        var listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket)
                .build();
        s3.listObjects(listObjectsRequest)
                .thenCompose(listObjectsResponse -> {
                    if (listObjectsResponse.contents().isEmpty()) {
                        return CompletableFuture.completedFuture(DeleteObjectsResponse.builder().build());
                    }
                    var objectIdentifiers = listObjectsResponse.contents().stream()
                            .map(S3Object::key)
                            .map(key -> ObjectIdentifier.builder().key(key).build())
                            .toList();
                    var delete = Delete.builder().objects(objectIdentifiers).build();
                    var deleteObjectsRequest = DeleteObjectsRequest.builder()
                            .delete(delete)
                            .bucket(bucket)
                            .build();
                    return s3.deleteObjects(deleteObjectsRequest);
                })
                .join();
    }

    protected Uni<CreateVersionResponseData> createDefaultVersion() {
        return createVersion(VERSION_NAME);
    }

    protected Uni<CreateVersionResponseData> createVersion(String versionName) {
        return createVersionOperation.process(
                STRUCTURE_NAME,
                versionName,
                false,
                new File(UPLOADING_FILE_PATH)
        );
    }

    @SneakyThrows
    protected static Set<String> paths() {
        try (var zip = new ZipFile(new File(UPLOADING_FILE_PATH))) {
            return Collections.list(zip.entries()).stream()
                    .filter(Predicate.not(ZipEntry::isDirectory))
                    .map(ZipEntry::getName)
                    .collect(Collectors.toSet());
        }
    }

}
