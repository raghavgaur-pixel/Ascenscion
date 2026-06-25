package com.ascension.database.connection;

import com.ascension.core.platform.PluginPlatform;
import com.ascension.database.model.DatabaseSettings;
import com.ascension.database.model.DatabaseType;
import com.zaxxer.hikari.HikariConfig;

/**
 * PostgreSQL connection configuration provider for production deployments.
 */
public final class PostgreSqlDatabaseProvider implements DatabaseProvider {

    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public HikariConfig createConfiguration(final DatabaseSettings settings, final PluginPlatform platform) {
        final HikariConfig configuration = new HikariConfig();
        configuration.setPoolName("Ascension-PostgreSQL");
        configuration.setDriverClassName("org.postgresql.Driver");
        configuration.setJdbcUrl(
            "jdbc:postgresql://"
                + settings.host()
                + ":"
                + settings.port()
                + "/"
                + settings.database()
        );
        configuration.setUsername(settings.username());
        configuration.setPassword(settings.password());
        configuration.setMaximumPoolSize(Math.max(2, settings.maximumPoolSize()));
        configuration.setMinimumIdle(Math.max(1, settings.minimumIdle()));
        configuration.setConnectionTestQuery("SELECT 1");
        return configuration;
    }
}

