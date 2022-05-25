package ru.craftysoft.schemaregistry.service.s3;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.enterprise.context.ApplicationScoped;

import static ru.craftysoft.schemaregistry.util.UuidUtils.generateDefaultUuid;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class S3Client {

    private final S3AsyncClient s3;

    public Uni<ResponseBytes<GetObjectResponse>> getFile(GetObjectRequest request) {
        var point = "S3Client.getFile";
        var s3RequestId = generateDefaultUuid();
        withS3RequestId(s3RequestId, () -> log.debug("""
                {}.in
                bucket={}
                key={}""", point, request.bucket(), request.key()));
        var future = s3.getObject(request, AsyncResponseTransformer.toBytes());
        return Uni.createFrom().completionStage(future)
                .onItemOrFailure()
                .invoke((response, throwable) -> withS3RequestId(s3RequestId, () -> {
                    if (throwable != null) {
                        log.error("{}.thrown {}", point, throwable.getMessage());
                    } else {
                        log.debug("{}.out length={}", point, response.response().contentLength());
                    }
                }));
    }

    public Uni<PutObjectResponse> uploadFile(PutObjectRequest request, AsyncRequestBody body) {
        var point = "S3Client.uploadFile";
        var s3RequestId = generateDefaultUuid();
        withS3RequestId(s3RequestId, () -> log.debug("""
                {}.in
                bucket={}
                key={}
                length={}""", point, request.bucket(), request.key(), body.contentLength().orElse(0L)));
        var future = s3.putObject(request, body);
        return Uni.createFrom().completionStage(future)
                .onItemOrFailure()
                .invoke((response, throwable) -> withS3RequestId(s3RequestId, () -> {
                    if (throwable != null) {
                        log.error("{}.thrown {}", point, throwable.getMessage());
                    } else {
                        log.debug("{}.out", point);
                    }
                }));
    }

    public Uni<DeleteObjectsResponse> deleteFiles(DeleteObjectsRequest request) {
        var point = "S3Client.deleteFile";
        var s3RequestId = generateDefaultUuid();
        withS3RequestId(s3RequestId, () -> log.debug("""
                {}.in
                bucket={}
                keys={}""", point, request.bucket(), request.delete().objects()));
        var future = s3.deleteObjects(request);
        return Uni.createFrom().completionStage(future)
                .onItemOrFailure()
                .invoke((response, throwable) -> withS3RequestId(s3RequestId, () -> {
                    if (throwable != null) {
                        log.error("{}.thrown {}", point, throwable.getMessage());
                    } else {
                        log.debug("{}.out count={}", point, response.deleted().size());
                    }
                }));
    }

    static void withS3RequestId(String s3RequestId, Runnable callback) {
        try (var ignored = MDC.putCloseable("s3RequestId", s3RequestId)) {
            callback.run();
        }
    }

}
