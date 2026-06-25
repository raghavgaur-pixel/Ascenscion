package com.ascension.database.connection;

import com.ascension.core.platform.PluginPlatform;
import com.ascension.database.model.DatabaseSettings;
import com.ascension.database.model.DatabaseType;
import com.zaxxer.hikari.HikariConfig;

/**
 * Factory for persistence-engine-specific connection pool configuration.
 */
public interface DatabaseProvider {

    /**
     * @return supported engine type
     */
    DatabaseType type();

    /**
     * Builds a hikari configuration for the given settings.
     *
     * @param settings database settings
     * @param platform plugin platform
     * @return hikari configuration
     */
    HikariConfig createConfiguration(DatabaseSettings settings, PluginPlatform platform);
}

