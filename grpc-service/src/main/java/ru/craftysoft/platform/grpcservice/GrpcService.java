package ru.craftysoft.platform.grpcservice;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.platform.grpcservice.proto.*;

@io.quarkus.grpc.GrpcService
public class GrpcService extends GrpcServiceGrpc.GrpcServiceImplBase {

    @Override
    public void filter(FilterRequest request, StreamObserver<FilterResponse> responseObserver) {
        var response = FilterResponse.newBuilder()
                .addData(FilterResponseData.newBuilder()
                        .setId(1L)
                        .setName("test")
                        .build())
                .addData(FilterResponseData.newBuilder()
                        .setId(2L)
                        .setName("test2")
                        .build())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getById(GetByIdRequest request, StreamObserver<GetByIdResponse> responseObserver) {
        var response = GetByIdResponse.newBuilder()
                .setType(request.getType())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void update(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        var response = UpdateResponse.newBuilder()
                .setId(1L)
                .setName("test")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
