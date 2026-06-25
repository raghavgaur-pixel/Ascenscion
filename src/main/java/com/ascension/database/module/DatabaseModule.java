package com.ascension.database.module;

import com.ascension.core.config.ConfigurationService;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.database.config.DatabaseSettingsLoader;
import com.ascension.database.connection.DatabaseProvider;
import com.ascension.database.connection.PostgreSqlDatabaseProvider;
import com.ascension.database.connection.SQLiteDatabaseProvider;
import com.ascension.database.migration.DatabaseMigrationService;
import com.ascension.database.model.DatabaseSettings;
import com.ascension.database.model.DatabaseType;
import com.ascension.database.service.DatabaseService;
import com.ascension.database.service.JdbcDatabaseService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.zaxxer.hikari.HikariConfig;
import java.util.Map;

/**
 * Starts the database service, configures the connection pool, and exposes migration services.
 */
public final class DatabaseModule extends AbstractModule {

    private final Map<DatabaseType, DatabaseProvider> providers = Map.of(
        DatabaseType.SQLITE, new SQLiteDatabaseProvider(),
        DatabaseType.POSTGRESQL, new PostgreSqlDatabaseProvider()
    );

    @Override
    public String id() {
        return "database";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("core-infrastructure", "registry");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final DatabaseSettings settings = new DatabaseSettingsLoader(
            services.require(ConfigurationService.class)
        ).load();
        final PluginPlatform platform = services.require(PluginPlatform.class);
        final DatabaseProvider provider = this.providers.get(settings.type());
        if (provider == null) {
            throw new IllegalStateException("Unsupported database type: " + settings.type());
        }

        final HikariConfig configuration = provider.createConfiguration(settings, platform);
        final DatabaseService databaseService = new JdbcDatabaseService(
            services.require(PluginLogger.class),
            settings,
            configuration
        );
        databaseService.start();

        final DatabaseMigrationService migrationService = new DatabaseMigrationService(
            databaseService,
            services.require(RegistryHub.class).require(AscensionRegistries.SCHEMA_MIGRATIONS)
        );

        services.register(DatabaseSettings.class, settings);
        services.register(DatabaseService.class, databaseService);
        services.register(DatabaseMigrationService.class, migrationService);
        services.require(PluginLogger.class).info("Database module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        if (services.has(DatabaseService.class)) {
            services.require(DatabaseService.class).stop();
        }
        services.require(PluginLogger.class).info("Database module stopped.");
    }
}

