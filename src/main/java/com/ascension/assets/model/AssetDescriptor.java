package com.ascension.assets.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared immutable asset metadata.
 *
 * @param id unique asset id
 * @param owner owning module or asset group
 * @param version asset schema/content version
 * @param displayName display name
 * @param description description text
 * @param metadata metadata map
 * @param compatibility engine compatibility
 * @param dependencies dependency references
 */
public record AssetDescriptor(
    AssetId id,
    String owner,
    SemanticVersion version,
    String displayName,
    String description,
    Map<String, String> metadata,
    AssetCompatibility compatibility,
    Set<AssetReference> dependencies
) {

    public AssetDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        metadata = Map.copyOf(metadata);
        Objects.requireNonNull(compatibility, "compatibility");
        dependencies = Set.copyOf(dependencies);
    }
}

