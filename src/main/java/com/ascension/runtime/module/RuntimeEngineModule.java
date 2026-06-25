package com.ascension.runtime.module;

import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.events.DefaultEventBus;
import com.ascension.events.EventBus;
import com.ascension.runtime.tick.DefaultGameLoop;
import com.ascension.runtime.tick.GameLoop;
import com.ascension.runtime.tick.TickManager;
import com.ascension.task.BukkitRuntimeTaskService;
import com.ascension.task.RuntimeTaskService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Initializes the internal runtime engine foundation: tasks, event bus, and game loop.
 */
public final class RuntimeEngineModule extends AbstractModule {

    @Override
    public String id() {
        return "runtime-engine";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("core-infrastructure");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RuntimeTaskService taskService = new BukkitRuntimeTaskService(
            services.require(JavaPlugin.class),
            services.require(PluginLogger.class)
        );
        final EventBus eventBus = new DefaultEventBus(services.require(PluginLogger.class), taskService);
        final GameLoop gameLoop = new DefaultGameLoop(services.require(PluginLogger.class), taskService);
        gameLoop.start();

        services.register(RuntimeTaskService.class, taskService);
        services.register(EventBus.class, eventBus);
        services.register(GameLoop.class, gameLoop);
        services.register(TickManager.class, gameLoop);
        services.require(PluginLogger.class).info("Runtime engine module started.");
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        if (services.has(GameLoop.class)) {
            services.require(GameLoop.class).stop();
        }
        if (services.has(RuntimeTaskService.class)) {
            services.require(RuntimeTaskService.class).cancelOwner("runtime-engine");
        }
        services.require(PluginLogger.class).info("Runtime engine module stopped.");
    }
}

