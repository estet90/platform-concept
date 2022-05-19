package ru.craftysoft.schemaregistry.dto.intermediate;

import javax.annotation.Nonnull;

public record Version(long id, @Nonnull String link) {
}
