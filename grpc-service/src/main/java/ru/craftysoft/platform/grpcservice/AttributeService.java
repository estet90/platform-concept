package ru.craftysoft.platform.grpcservice;

import com.google.protobuf.NullValue;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import ru.craftysoft.platform.grpcservice.proto.AttributeFilterRequest;
import ru.craftysoft.platform.grpcservice.proto.AttributeFilterResponse;
import ru.craftysoft.platform.grpcservice.proto.AttributeServiceGrpc;
import ru.craftysoft.proto.NullableString;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@GrpcService
public class AttributeService extends AttributeServiceGrpc.AttributeServiceImplBase {

    private static final Set<Attribute> attributes = DocumentService.documents.stream()
            .map(DocumentService.Document::id)
            .flatMap(documentId -> Stream.of(1, 2, 3, 4, 5, 6)
                    .map(value -> documentId * 10 + value)
                    .map(id -> new Attribute(id, "attribute name " + id, "attribute value " + id, documentId))
            )
            .collect(Collectors.toSet());

    @Override
    public void attributeFilter(AttributeFilterRequest request, StreamObserver<AttributeFilterResponse> responseObserver) {
        var response = process(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public AttributeFilterResponse process(AttributeFilterRequest request) {
        var documentId = request.getDocumentId();
        var filteredAttributes = attributes.stream()
                .filter(attribute -> documentId == attribute.documentId)
                .map(attribute -> {
                            var valueBuilder = NullableString.newBuilder();
                            ofNullable(attribute.value).ifPresentOrElse(
                                    valueBuilder::setValue,
                                    () -> valueBuilder.setNullValue(NullValue.NULL_VALUE)
                            );
                            return ru.craftysoft.platform.grpcservice.proto.Attribute.newBuilder()
                                    .setId(attribute.id)
                                    .setName(attribute.name)
                                    .setDocumentId(attribute.documentId)
                                    .setValue(valueBuilder)
                                    .build();
                        }
                )
                .collect(Collectors.toSet());
        return AttributeFilterResponse.newBuilder()
                .addAllAttributes(filteredAttributes)
                .build();
    }

    private record Attribute(long id, String name, @Nullable String value, long documentId) {
    }
}
