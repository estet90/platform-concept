package ru.craftysoft.schemaregistry.builder.s3;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GetObjectRequestBuilder {

    public GetObjectRequest build(String link) {
        var parts = link.split("/", 2);
        var bucket = parts[0];
        var fileName = parts[1];
        return GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();
    }

}
