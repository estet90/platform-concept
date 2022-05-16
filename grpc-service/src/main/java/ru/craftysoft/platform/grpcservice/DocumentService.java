package ru.craftysoft.platform.grpcservice;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import ru.craftysoft.platform.grpcservice.proto.DocumentFilterRequest;
import ru.craftysoft.platform.grpcservice.proto.DocumentFilterResponse;
import ru.craftysoft.platform.grpcservice.proto.DocumentServiceGrpc;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@GrpcService
public class DocumentService extends DocumentServiceGrpc.DocumentServiceImplBase {

    public static final Set<Document> documents = Stream.of(1, 2, 3, 4, 5, 6)
            .map(i -> new Document(i, "document " + i, OffsetDateTime.of(2022, i, i, i, i, i, i, ZoneOffset.UTC)))
            .collect(Collectors.toSet());

    @Override
    public void documentFilter(DocumentFilterRequest request, StreamObserver<DocumentFilterResponse> responseObserver) {
        var response = process(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private DocumentFilterResponse process(DocumentFilterRequest request) {
        var ids = Set.copyOf(request.getIdsList());
        var names = Set.copyOf(request.getNamesList());
        var createdAtFrom = request.hasCreatedAtFrom()
                ? Instant
                .ofEpochSecond(
                        request.getCreatedAtFrom().getSeconds(),
                        request.getCreatedAtFrom().getNanos()
                ).atOffset(ZoneOffset.UTC)
                : null;
        var createdAtTo = request.hasCreatedAtTo()
                ? Instant
                .ofEpochSecond(
                        request.getCreatedAtTo().getSeconds(),
                        request.getCreatedAtTo().getNanos()
                ).atOffset(ZoneOffset.UTC)
                : null;
        var filteredDocuments = documents.stream()
                .filter(document -> ids.size() == 0 || ids.contains(document.id))
                .filter(document -> names.size() == 0 || names.contains(document.name))
                .filter(document -> createdAtFrom == null || document.createdAt.equals(createdAtFrom) || document.createdAt.isAfter(createdAtFrom))
                .filter(document -> createdAtTo == null || document.createdAt.equals(createdAtTo) || document.createdAt.isBefore(createdAtTo))
                .map(document -> ru.craftysoft.platform.grpcservice.proto.Document.newBuilder()
                        .setId(document.id)
                        .setName(document.name)
                        .setCreatedAt(Timestamp.newBuilder()
                                .setSeconds(document.createdAt.toEpochSecond())
                                .setNanos(document.createdAt.getNano()))
                        .build()
                )
                .toList();
        return DocumentFilterResponse.newBuilder()
                .addAllDocuments(filteredDocuments)
                .build();
    }

    protected record Document(long id, @Nonnull String name, @Nonnull OffsetDateTime createdAt) {
    }
}
