package ru.craftysoft.schemaregistry.service.s3;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniCombine;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.core.Vertx;
import lombok.RequiredArgsConstructor;
import ru.craftysoft.schemaregistry.builder.s3.DeleteObjectsRequestBuilder;
import ru.craftysoft.schemaregistry.builder.s3.GetObjectRequestBuilder;
import ru.craftysoft.schemaregistry.builder.s3.PutObjectRequestBuilder;
import ru.craftysoft.schemaregistry.dto.intermediate.Schema;
import ru.craftysoft.schemaregistry.dto.intermediate.Version;
import ru.craftysoft.schemaregistry.model.jooq.tables.records.VersionsRecord;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class S3ClientAdapter {

    private final S3Client client;
    private final GetObjectRequestBuilder getObjectRequestBuilder;
    private final PutObjectRequestBuilder putObjectRequestBuilder;
    private final DeleteObjectsRequestBuilder deleteObjectsRequestBuilder;
    private final Vertx vertx;

    public Uni<String> getSchema(String link) {
        var request = getObjectRequestBuilder.build(link);
        return client.getFile(request)
                .map(BytesWrapper::asUtf8String);
    }

    public Uni<File> getVersion(String link) {
        var request = getObjectRequestBuilder.build(link);
        return client.getFile(request)
                .flatMap(bytesWrapper -> {
                    var uni = Uni.createFrom()
                            .item(Unchecked.supplier(() -> Files.write(Path.of("result.zip"), bytesWrapper.asByteArray()).toFile()));
                    return vertx.executeBlocking(uni);
                });
    }

    public Uni<Void> createVersion(Version version, File body, Set<Schema> schemas) {
        var versionRequest = putObjectRequestBuilder.build(version);
        var versionBody = AsyncRequestBody.fromFile(body);
        var putVersionUni = client.uploadFile(versionRequest, versionBody);
        var putUnis = schemas.stream()
                .map(schema -> {
                    var schemaRequest = putObjectRequestBuilder.build(schema);
                    var schemaBody = AsyncRequestBody.fromBytes(schema.content());
                    return client.uploadFile(schemaRequest, schemaBody);
                })
                .collect(Collectors.toSet());
        putUnis.add(putVersionUni);
        return UniCombine.INSTANCE.all().unis(putUnis)
                .combinedWith(unis -> null);
    }

    public Uni<Void> deleteFiles(Set<VersionsRecord> versions, Set<String> schemasLinks) {
        var links = new HashSet<>(schemasLinks);
        var versionsLinks = versions.stream()
                .map(VersionsRecord::getLink)
                .toList();
        links.addAll(versionsLinks);
        return deleteFiles(links);
    }

    public Uni<Void> deleteFiles(String versionLink, Set<String> schemasLinks) {
        var links = new HashSet<>(schemasLinks);
        links.add(versionLink);
        return deleteFiles(links);
    }

    private Uni<Void> deleteFiles(Set<String> links) {
        var request = deleteObjectsRequestBuilder.build(links);
        return client.deleteFiles(request)
                .replaceWithVoid();
    }

}
