package ru.craftysoft.platform.grpcservice;

import io.grpc.stub.StreamObserver;
import ru.craftysoft.platform.grpcservice.proto.*;

@io.quarkus.grpc.GrpcService
public class GrpcService extends GrpcServiceGrpc.GrpcServiceImplBase {

    @Override
    public void filter(FilterRequest request, StreamObserver<FilterResponse> responseObserver) {
        System.out.println("request bliat: " + request);
        var response = FilterResponse.newBuilder()
                .addData(FilterResponseData.newBuilder()
                        .setId(1L)
                        .setName("test")
                        .build())
                .build();
        System.out.println("response nahuy: " + response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void update(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        System.out.println("request bliat: " + request);
        var response = UpdateResponse.newBuilder()
                .setId(1L)
                .setName("test")
                .build();
        System.out.println("response nahuy: " + response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
