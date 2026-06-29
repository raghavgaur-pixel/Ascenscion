package com.ascension.effects.module;

import com.ascension.assets.loader.AssetService;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.effects.definition.EffectDefinition;
import com.ascension.effects.definition.EffectDefinitionSerializer;
import com.ascension.effects.listener.EffectSessionListener;
import com.ascension.effects.service.DefaultEffectService;
import com.ascension.effects.service.EffectService;
import com.ascension.events.EventBus;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.runtime.tick.GameLoop;
import com.ascension.runtime.tick.TickPriority;
import com.ascension.runtime.tick.TickTask;
import com.ascension.session.service.PlayerSessionManager;

import java.util.Set;

/**
 * Initializes the effects engine and its runtime dependencies.
 */
public final class EffectsModule extends AbstractModule {

    @Override
    public String id() {
        return "effects";
    }

    @Override
    public Set<String> dependencies() {
        return Set.of("core", "runtime", "session", "registry", "assets");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registryHub = services.require(RegistryHub.class);
        final EventBus eventBus = services.require(EventBus.class);
        final PlayerSessionManager sessionManager = services.require(PlayerSessionManager.class);
        final GameLoop gameLoop = services.require(GameLoop.class);
        final AssetService assetService = services.require(AssetService.class);

        // Asset serializers and definitions are generally registered in AssetModule directly,
        // but we can register dynamically if AssetService exposes it.
        // Wait, AssetService does not expose `registerSerializer`.
        // I will let AssetModule handle the AssetType creation.
        // Let's remove this block since `assetService` doesn't have `registerSerializer`.
        registryHub.getOrCreateReloadable(AscensionRegistries.EFFECT_DEFINITIONS);

        // Initialize service
        final DefaultEffectService effectService = new DefaultEffectService(registryHub, eventBus, sessionManager);
        services.register(EffectService.class, effectService);

        // Register tickable for effect updates and expirations
        gameLoop.register(new TickTask(
            "effects",
            "effect_lifecycle_tick",
            TickPriority.NORMAL,
            effectService
        ));

        // Initialize listeners
        new EffectSessionListener(eventBus, effectService);
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        if (services.has(GameLoop.class)) {
            services.require(GameLoop.class).unregisterOwner("effects");
        }

        if (services.has(EventBus.class)) {
            services.require(EventBus.class).unsubscribeOwner("effects");
        }
    }
}
