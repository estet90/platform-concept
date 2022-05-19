package ru.craftysoft.schemaregistry.builder.intermediate;

import io.smallrye.mutiny.unchecked.Unchecked;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.craftysoft.schemaregistry.dto.intermediate.Schema;
import ru.craftysoft.schemaregistry.dto.intermediate.Version;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@ApplicationScoped
public class SchemaBuilder {

    private final String bucket;

    public SchemaBuilder(@ConfigProperty(name = "s3.bucket") String bucket) {
        this.bucket = bucket;
    }

    public Set<Schema> build(Version version, File body) {
        try (var zip = new ZipFile(body)) {
            return Collections.list(zip.entries()).stream()
                    .filter(Predicate.not(ZipEntry::isDirectory))
                    .map(Unchecked.function(zipEntry -> {
                        try (var fileInputStream = zip.getInputStream(zipEntry)) {
                            return new Schema(
                                    version.id(),
                                    zipEntry.getName(),
                                    bucket + "/" + "schema_" + UUID.randomUUID(),
                                    fileInputStream.readAllBytes()
                            );
                        }
                    }))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
