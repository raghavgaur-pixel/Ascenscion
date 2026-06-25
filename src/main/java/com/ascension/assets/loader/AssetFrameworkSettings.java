package com.ascension.assets.loader;

import com.ascension.assets.model.SemanticVersion;
import java.util.Map;
import java.util.Objects;

/**
 * Typed asset framework settings.
 *
 * @param rootDirectory root asset directory relative to the plugin data folder
 * @param defaultNamespace default namespace for future generated assets
 * @param engineVersion current engine data version
 * @param strictValidation whether validation errors should fail reloads
 * @param ownedDirectories declared asset directories by group id
 */
public record AssetFrameworkSettings(
    String rootDirectory,
    String defaultNamespace,
    SemanticVersion engineVersion,
    boolean strictValidation,
    Map<String, String> ownedDirectories
) {

    public AssetFrameworkSettings {
        Objects.requireNonNull(rootDirectory, "rootDirectory");
        Objects.requireNonNull(defaultNamespace, "defaultNamespace");
        Objects.requireNonNull(engineVersion, "engineVersion");
        ownedDirectories = Map.copyOf(ownedDirectories);
    }
}
