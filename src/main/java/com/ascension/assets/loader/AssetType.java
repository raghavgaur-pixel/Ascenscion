package com.ascension.assets.loader;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.registry.RegistryDescriptor;
import com.ascension.validation.Validator;
import java.util.Objects;

/**
 * Typed asset group descriptor.
 *
 * @param <T> asset type
 */
public record AssetType<T extends AssetDefinition>(
    String owner,
    String id,
    String directory,
    RegistryDescriptor<AssetId, T> registryDescriptor,
    AssetSerializer<T> serializer,
    Validator<T> validator
) {

    public AssetType {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(registryDescriptor, "registryDescriptor");
        Objects.requireNonNull(serializer, "serializer");
        Objects.requireNonNull(validator, "validator");
    }
}

