package com.ascension.core.module;

import com.ascension.core.config.ConfigurationService;
import com.ascension.core.integration.IntegrationRegistry;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.core.config.typed.DefaultTypedConfigurationService;
import com.ascension.core.config.typed.TypedConfigurationService;
import com.ascension.serialization.BinarySerializedObjectCodec;
import com.ascension.serialization.DefaultSerializedObjectCodecRegistry;
import com.ascension.serialization.JsonSerializedObjectCodec;
import com.ascension.serialization.SerializedObjectCodecRegistry;
import com.ascension.serialization.YamlSerializedObjectCodec;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        final ObjectMapper objectMapper = new ObjectMapper();
        final SerializedObjectCodecRegistry codecRegistry = new DefaultSerializedObjectCodecRegistry();
        codecRegistry.register(new YamlSerializedObjectCodec());
        codecRegistry.register(new JsonSerializedObjectCodec(objectMapper));
        codecRegistry.register(new BinarySerializedObjectCodec(objectMapper));
        final TypedConfigurationService typedConfigurationService = new DefaultTypedConfigurationService(
            platform,
            logger,
            codecRegistry
        );

        platform.ensureDataDirectories();
        configurationService.load("config/config.yml");
        configurationService.load("messages/en_US.yml");
        integrationRegistry.discover();
        services.register(SerializedObjectCodecRegistry.class, codecRegistry);
        services.register(TypedConfigurationService.class, typedConfigurationService);

        logger.info("Core infrastructure module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(PluginLogger.class).info("Core infrastructure module stopped.");
    }
}
