package com.ascension.profiles.module;

import com.ascension.core.cache.ConcurrentCache;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.database.migration.DatabaseMigrationService;
import com.ascension.database.migration.SchemaMigration;
import com.ascension.database.migration.SqlSchemaMigration;
import com.ascension.profiles.component.AchievementsProfileComponentDefinition;
import com.ascension.profiles.component.CurrencyProfileComponentDefinition;
import com.ascension.profiles.component.ProfileComponentDefinition;
import com.ascension.profiles.component.ProgressionProfileComponentDefinition;
import com.ascension.profiles.component.QuestProgressProfileComponent;
import com.ascension.profiles.component.SettingsProfileComponentDefinition;
import com.ascension.profiles.component.StatisticsProfileComponentDefinition;
import com.ascension.profiles.component.UnlockedFloorsProfileComponentDefinition;
import com.ascension.profiles.repository.JdbcPlayerProfileRepository;
import com.ascension.profiles.repository.PlayerProfileRepository;
import com.ascension.profiles.service.DefaultPlayerProfileService;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.MutableRegistry;
import com.ascension.registry.RegistryHub;
import com.ascension.serialization.YamlSerializedObjectCodec;
import java.util.List;

/** Registers modular profile components, schema migrations, and the player profile service. */
public final class ProfileModule extends AbstractModule {

    @Override public String id() { return "profiles"; }
    @Override public java.util.Set<String> dependencies() { return java.util.Set.of("registry", "database"); }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registryHub = services.require(RegistryHub.class);
        final MutableRegistry<String, ProfileComponentDefinition<?>> componentRegistry =
            registryHub.getOrCreate(AscensionRegistries.PROFILE_COMPONENTS);
        final MutableRegistry<String, SchemaMigration> migrationRegistry =
            registryHub.getOrCreate(AscensionRegistries.SCHEMA_MIGRATIONS);
        registerComponentDefinitions(componentRegistry);
        registerMigrations(migrationRegistry);
        services.require(DatabaseMigrationService.class).migrate().join();

        final PlayerProfileRepository repository = new JdbcPlayerProfileRepository(
            services.require(com.ascension.database.service.DatabaseService.class), new YamlSerializedObjectCodec()
        );
        final PlayerProfileService profileService = new DefaultPlayerProfileService(
            services.require(PluginLogger.class), repository,
            registryHub.require(AscensionRegistries.PROFILE_COMPONENTS), new ConcurrentCache<>()
        );
        services.register(PlayerProfileRepository.class, repository);
        services.register(PlayerProfileService.class, profileService);
        services.require(PluginLogger.class).info("Profile module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        if (services.has(PlayerProfileService.class)) services.require(PlayerProfileService.class).saveAll().join();
        services.require(PluginLogger.class).info("Profile module stopped.");
    }

    private static void registerComponentDefinitions(final MutableRegistry<String, ProfileComponentDefinition<?>> componentRegistry) {
        final List<ProfileComponentDefinition<?>> definitions = List.of(
            new SettingsProfileComponentDefinition(),
            new UnlockedFloorsProfileComponentDefinition(),
            new ProgressionProfileComponentDefinition(),
            new CurrencyProfileComponentDefinition(),
            new StatisticsProfileComponentDefinition(),
            new AchievementsProfileComponentDefinition(),
            new QuestProgressProfileComponent.Definition()
        );
        for (final ProfileComponentDefinition<?> definition : definitions) {
            if (!componentRegistry.contains(definition.id())) componentRegistry.register(definition.id(), definition);
        }
    }

    private static void registerMigrations(final MutableRegistry<String, SchemaMigration> migrationRegistry) {
        final SchemaMigration migration = new SqlSchemaMigration(
            "20260625_profile_tables",
            "Create player profile and profile component persistence tables.",
            List.of(
                """
                CREATE TABLE IF NOT EXISTS player_profiles (
                    player_id VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    display_name VARCHAR(64) NOT NULL,
                    first_join BIGINT NOT NULL,
                    last_join BIGINT NOT NULL,
                    playtime_seconds BIGINT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS profile_component_data (
                    player_id VARCHAR(36) NOT NULL,
                    component_id VARCHAR(128) NOT NULL,
                    payload TEXT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    PRIMARY KEY (player_id, component_id)
                )
                """,
                """
                CREATE INDEX IF NOT EXISTS idx_profile_component_data_player_id
                ON profile_component_data(player_id)
                """
            )
        );
        if (!migrationRegistry.contains(migration.id())) migrationRegistry.register(migration.id(), migration);
    }
}
