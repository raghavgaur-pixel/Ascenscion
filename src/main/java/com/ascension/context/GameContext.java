package com.ascension.context;

import com.ascension.database.service.DatabaseService;
import com.ascension.events.EventBus;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.registry.RegistryHub;
import com.ascension.runtime.tick.GameLoop;
import com.ascension.runtime.tick.TickManager;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.stats.service.AttributeService;
import com.ascension.stats.service.StatService;
import com.ascension.task.RuntimeTaskService;

/**
 * Immutable runtime facade used for explicit constructor injection of core engine services.
 *
 * <p>This is intended for subsystem composition, not ad-hoc global lookups.
 */
public interface GameContext {

    /**
     * @return internal event bus
     */
    EventBus eventBus();

    /**
     * @return runtime task service
     */
    RuntimeTaskService taskService();

    /**
     * @return centralized game loop
     */
    GameLoop gameLoop();

    /**
     * @return tick manager
     */
    TickManager tickManager();

    /**
     * @return profile service
     */
    PlayerProfileService profileService();

    /**
     * @return session manager
     */
    PlayerSessionManager sessionManager();

    /**
     * @return stat lookup service
     */
    StatService statService();

    /**
     * @return attribute runtime service
     */
    AttributeService attributeService();

    /**
     * @return registry hub
     */
    RegistryHub registryHub();

    /**
     * @return database service
     */
    DatabaseService databaseService();
}
