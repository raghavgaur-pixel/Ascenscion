package com.ascension.core.config.typed;

import com.ascension.serialization.SerializedObject;

/**
 * Upgrades configuration payloads between schema versions.
 */
@FunctionalInterface
public interface ConfigurationUpgrader {

    /**
     * Upgrades a configuration payload from one version to another.
     *
     * @param fromVersion stored version
     * @param payload raw payload
     * @param targetVersion desired version
     * @return upgraded payload
     */
    SerializedObject upgrade(int fromVersion, SerializedObject payload, int targetVersion);

    /**
     * @return no-op upgrader
     */
    static ConfigurationUpgrader noop() {
        return (fromVersion, payload, targetVersion) -> payload;
    }
}

