package com.ascension.core.module;

import com.ascension.core.config.ConfigurationService;
import com.ascension.core.integration.IntegrationRegistry;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.core.service.ServiceRegistry;

public final class CoreInfrastructureModule extends AbstractModule {

    @Override
    public String id() {
        return "core-infrastructure";
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final PluginLogger logger = services.require(PluginLogger.class);
        final PluginPlatform platform = services.require(PluginPlatform.class);
        final ConfigurationService configurationService = services.require(ConfigurationService.class);
        final IntegrationRegistry integrationRegistry = services.require(IntegrationRegistry.class);

        platform.ensureDataDirectories();
        configurationService.load("config/config.yml");
        configurationService.load("messages/en_US.yml");
        integrationRegistry.discover();

        logger.info("Core infrastructure module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(PluginLogger.class).info("Core infrastructure module stopped.");
    }
}

