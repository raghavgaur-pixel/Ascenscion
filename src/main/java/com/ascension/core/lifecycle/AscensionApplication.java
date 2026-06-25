package com.ascension.core.lifecycle;

import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.ModuleManager;
import com.ascension.core.service.ServiceRegistry;

public final class AscensionApplication {

    private final PluginLogger logger;
    private final ServiceRegistry serviceRegistry;
    private final ModuleManager moduleManager;
    private volatile boolean running;

    public AscensionApplication(
        final PluginLogger logger,
        final ServiceRegistry serviceRegistry,
        final ModuleManager moduleManager
    ) {
        this.logger = logger;
        this.serviceRegistry = serviceRegistry;
        this.moduleManager = moduleManager;
    }

    public synchronized void start() {
        if (this.running) {
            throw new IllegalStateException("Application is already running.");
        }

        this.moduleManager.startAll(this.serviceRegistry);
        this.running = true;
        this.logger.info("Ascension application started.");
    }

    public synchronized void stop() {
        if (!this.running) {
            return;
        }

        this.moduleManager.stopAll(this.serviceRegistry);
        this.running = false;
        this.logger.info("Ascension application stopped.");
    }
}

