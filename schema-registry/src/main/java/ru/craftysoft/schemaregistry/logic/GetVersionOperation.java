package ru.craftysoft.schemaregistry.logic;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.craftysoft.schemaregistry.service.dao.VersionDaoAdapter;
import ru.craftysoft.schemaregistry.service.s3.S3ClientAdapter;
import ru.craftysoft.schemaregistry.util.OperationWrapper;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class GetVersionOperation {

    private final VersionDaoAdapter versionDaoAdapter;
    private final S3ClientAdapter s3ClientAdapter;

    public Uni<File> process(@Nullable Long structureId,
                             @Nullable String structureName,
                             @Nullable Long versionId,
                             @Nullable String versionName) {
        return OperationWrapper.wrap(
                log, "GetVersionOperation.process",
                () -> versionDaoAdapter.getLink(structureId, structureName, versionId, versionName)
                        .flatMap(s3ClientAdapter::getVersion),
                () -> "structureId=%s structureName=%s versionId=%s versionName=%s".formatted(structureId, structureName, versionId, versionName),
                null
        );
    }

}
