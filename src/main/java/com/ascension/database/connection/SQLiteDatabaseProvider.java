package com.ascension.database.connection;

import com.ascension.core.platform.PluginPlatform;
import com.ascension.database.model.DatabaseSettings;
import com.ascension.database.model.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import java.io.File;

/**
 * SQLite connection configuration provider for development and single-node deployments.
 */
public final class SQLiteDatabaseProvider implements DatabaseProvider {

    @Override
    public DatabaseType type() {
        return DatabaseType.SQLITE;
    }

    @Override
    public HikariConfig createConfiguration(final DatabaseSettings settings, final PluginPlatform platform) {
        final HikariConfig configuration = new HikariConfig();
        final File databaseFile = new File(platform.dataFolder(), settings.sqliteFile());
        final File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create SQLite parent directory: " + parent.getAbsolutePath());
        }

        configuration.setPoolName("Ascension-SQLite");
        configuration.setDriverClassName("org.sqlite.JDBC");
        configuration.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        configuration.setMaximumPoolSize(Math.max(1, settings.maximumPoolSize()));
        configuration.setMinimumIdle(Math.max(1, settings.minimumIdle()));
        configuration.setConnectionTestQuery("SELECT 1");
        configuration.addDataSourceProperty("foreign_keys", "true");
        configuration.addDataSourceProperty("busy_timeout", "5000");
        return configuration;
    }
}

