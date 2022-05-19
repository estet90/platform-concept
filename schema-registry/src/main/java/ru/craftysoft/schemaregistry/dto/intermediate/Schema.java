package ru.craftysoft.schemaregistry.dto.intermediate;

import javax.annotation.Nonnull;

public record Schema(long versionId,
                     @Nonnull String path,
                     @Nonnull String link,
                     @Nonnull byte[] content) {
}