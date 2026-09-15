package com.ascension.registry;

import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;

/**
 * Initializes the global registry hub and foundational registries.
 */
public final class RegistryModule extends AbstractModule {

    @Override
    public String id() {
        return "registry";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("runtime-engine");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registryHub = new DefaultRegistryHub();
        registryHub.getOrCreate(AscensionRegistries.SCHEMA_MIGRATIONS);
        registryHub.getOrCreate(AscensionRegistries.PROFILE_COMPONENTS);
        registryHub.getOrCreateReloadable(AscensionRegistries.ASSET_TYPES);
        registryHub.getOrCreateReloadable(AscensionRegistries.LOCALIZATION_BUNDLES);
        registryHub.getOrCreateReloadable(AscensionRegistries.ITEM_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.ABILITY_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.SKILL_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.BOSS_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.FLOOR_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.QUEST_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.PROFESSION_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.LOOT_TABLE_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.NPC_DEFINITIONS);
        registryHub.getOrCreateReloadable(AscensionRegistries.STAT_DEFINITIONS);
        registryHub.getOrCreate(AscensionRegistries.DERIVED_STAT_CALCULATOR_FACTORIES);

        services.register(RegistryHub.class, registryHub);
        services.require(PluginLogger.class).info("Registry module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(PluginLogger.class).info("Registry module stopped.");
    }
}
