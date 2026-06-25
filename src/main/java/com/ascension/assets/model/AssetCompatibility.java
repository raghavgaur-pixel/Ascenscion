package com.ascension.assets.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Version compatibility window for an asset.
 *
 * @param minimumEngineVersion optional minimum engine version
 * @param maximumEngineVersion optional maximum engine version
 */
public record AssetCompatibility(
    Optional<SemanticVersion> minimumEngineVersion,
    Optional<SemanticVersion> maximumEngineVersion
) {

    public AssetCompatibility {
        Objects.requireNonNull(minimumEngineVersion, "minimumEngineVersion");
        Objects.requireNonNull(maximumEngineVersion, "maximumEngineVersion");
    }

    /**
     * @return compatibility with no explicit bounds
     */
    public static AssetCompatibility open() {
        return new AssetCompatibility(Optional.empty(), Optional.empty());
    }

    /**
     * Checks whether an engine version is supported.
     *
     * @param engineVersion engine version
     * @return {@code true} if supported
     */
    public boolean supports(final SemanticVersion engineVersion) {
        if (this.minimumEngineVersion.isPresent() && engineVersion.compareTo(this.minimumEngineVersion.get()) < 0) {
            return false;
        }
        if (this.maximumEngineVersion.isPresent() && engineVersion.compareTo(this.maximumEngineVersion.get()) > 0) {
            return false;
        }
        return true;
    }
}

