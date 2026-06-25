package com.ascension.database.config;

import com.ascension.core.config.ConfigurationFile;
import com.ascension.core.config.ConfigurationService;
import com.ascension.database.model.DatabaseSettings;
import com.ascension.database.model.DatabaseType;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Loads immutable database settings from plugin configuration.
 */
public final class DatabaseSettingsLoader {

    private final ConfigurationService configurationService;

    public DatabaseSettingsLoader(final ConfigurationService configurationService) {
        this.configurationService = Objects.requireNonNull(configurationService, "configurationService");
    }

    /**
     * Loads database settings from the canonical database config file.
     *
     * @return immutable database settings
     */
    public DatabaseSettings load() {
        final ConfigurationFile configurationFile = this.configurationService.load("config/database.yml");
        final ConfigurationSection root = configurationFile.configuration();
        final String typeValue = root.getString("database.type", DatabaseType.SQLITE.name());
        final DatabaseType type = DatabaseType.valueOf(typeValue.toUpperCase(Locale.ROOT));

        return new DatabaseSettings(
            type,
            root.getString("database.sqlite.file", "data/ascension.db"),
            root.getString("database.postgresql.host", "localhost"),
            root.getInt("database.postgresql.port", 5432),
            root.getString("database.postgresql.database", "ascension"),
            root.getString("database.postgresql.username", "ascension"),
            root.getString("database.postgresql.password", ""),
            root.getInt("database.pool.maximum-size", 10),
            root.getInt("database.pool.minimum-idle", 2),
            root.getInt("database.workers", 4)
        );
    }
}

