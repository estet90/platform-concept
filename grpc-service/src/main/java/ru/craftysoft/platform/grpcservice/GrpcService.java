package ru.craftysoft.platform.grpcservice;

import io.grpc.stub.StreamObserver;
import ru.tsc.crm.authorization.api.proto.*;

@io.quarkus.grpc.GrpcService
public class GrpcService extends GrpcServiceGrpc.GrpcServiceImplBase {

    @Override
    public void filter(FilterRequest request, StreamObserver<FilterResponse> responseObserver) {
        responseObserver.onNext(FilterResponse.newBuilder()
                .addData(FilterResponseData.newBuilder()
                        .setId(1)
                        .setName("test")
                        .build())
                .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void update(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        responseObserver.onNext(UpdateResponse.newBuilder()
                .setId(1)
                .setName("test")
                .build());
        responseObserver.onCompleted();
    }
}
