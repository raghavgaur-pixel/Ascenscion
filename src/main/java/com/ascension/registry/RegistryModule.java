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
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registryHub = new DefaultRegistryHub();
        registryHub.getOrCreate(AscensionRegistries.SCHEMA_MIGRATIONS);
        registryHub.getOrCreate(AscensionRegistries.PROFILE_COMPONENTS);

        services.register(RegistryHub.class, registryHub);
        services.require(PluginLogger.class).info("Registry module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(PluginLogger.class).info("Registry module stopped.");
    }
}

