package ru.craftysoft.schemaregistry.builder.s3;

import com.sun.source.doctree.SeeTree;
import ru.craftysoft.schemaregistry.dto.intermediate.Schema;
import ru.craftysoft.schemaregistry.dto.intermediate.Version;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
public class PutObjectRequestBuilder {

    public PutObjectRequest build(Version version) {
        var parts = version.link().split("/", 2);
        var bucket = parts[0];
        var fileName = parts[1];
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();
    }

    public PutObjectRequest build(Schema schema) {
        var parts = schema.link().split("/", 2);
        var bucket = parts[0];
        var fileName = parts[1];
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();
    }
}
