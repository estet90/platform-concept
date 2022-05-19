package ru.craftysoft.schemaregistry.builder.s3;

import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
public class DeleteObjectsRequestBuilder {

    public DeleteObjectsRequest build(Set<String> links) {
        var bucket = links.iterator().next().split("/")[0];
        var objects = links.stream()
                .map(link -> link.split("/", 2)[1])
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
        return DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder()
                        .objects(objects)
                        .build())
                .build();
    }

}
