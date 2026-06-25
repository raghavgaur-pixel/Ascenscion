package com.ascension.core.config.typed;

import com.ascension.serialization.SerializedFormat;
import com.ascension.validation.Validator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Descriptor for a typed configuration file.
 *
 * @param <T> configuration type
 */
public record TypedConfigurationDescriptor<T>(
    String owner,
    String id,
    String path,
    int version,
    SerializedFormat format,
    TypedConfigurationCodec<T> codec,
    Validator<T> validator,
    Supplier<T> defaultSupplier,
    ConfigurationUpgrader upgrader,
    List<String> headerComments
) {

    public TypedConfigurationDescriptor {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(defaultSupplier, "defaultSupplier");
        Objects.requireNonNull(upgrader, "upgrader");
        headerComments = List.copyOf(headerComments);
    }
}

