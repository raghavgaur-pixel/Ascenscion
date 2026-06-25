package com.ascension.session.module;

import com.ascension.context.DefaultGameContext;
import com.ascension.context.GameContext;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.database.service.DatabaseService;
import com.ascension.events.EventBus;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.registry.RegistryHub;
import com.ascension.runtime.tick.GameLoop;
import com.ascension.session.listener.PlayerSessionListener;
import com.ascension.session.service.DefaultPlayerSessionManager;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.task.RuntimeTaskService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Initializes the player session framework and runtime game context.
 */
public final class SessionModule extends AbstractModule {

    @Override
    public String id() {
        return "sessions";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("runtime-engine", "profiles", "database", "registry");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final PlayerSessionManager sessionManager = new DefaultPlayerSessionManager(
            services.require(JavaPlugin.class),
            services.require(PluginLogger.class),
            services.require(EventBus.class),
            services.require(RuntimeTaskService.class),
            services.require(PlayerProfileService.class)
        );

        services.register(PlayerSessionManager.class, sessionManager);
        services.register(GameContext.class, new DefaultGameContext(
            services.require(EventBus.class),
            services.require(RuntimeTaskService.class),
            services.require(GameLoop.class),
            services.require(PlayerProfileService.class),
            sessionManager,
            services.require(RegistryHub.class),
            services.require(DatabaseService.class)
        ));

        services.require(JavaPlugin.class).getServer().getPluginManager().registerEvents(
            new PlayerSessionListener(sessionManager),
            services.require(JavaPlugin.class)
        );
        services.require(PluginLogger.class).info("Session module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        if (services.has(PlayerSessionManager.class)) {
            services.require(PlayerSessionManager.class).shutdown().join();
        }
        services.require(PluginLogger.class).info("Session module stopped.");
    }
}

