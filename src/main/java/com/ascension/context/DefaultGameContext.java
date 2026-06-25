package com.ascension.context;

import com.ascension.database.service.DatabaseService;
import com.ascension.events.EventBus;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.registry.RegistryHub;
import com.ascension.runtime.tick.GameLoop;
import com.ascension.runtime.tick.TickManager;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.task.RuntimeTaskService;
import java.util.Objects;

/**
 * Default immutable game context implementation.
 */
public final class DefaultGameContext implements GameContext {

    private final EventBus eventBus;
    private final RuntimeTaskService taskService;
    private final GameLoop gameLoop;
    private final PlayerProfileService profileService;
    private final PlayerSessionManager sessionManager;
    private final RegistryHub registryHub;
    private final DatabaseService databaseService;

    public DefaultGameContext(
        final EventBus eventBus,
        final RuntimeTaskService taskService,
        final GameLoop gameLoop,
        final PlayerProfileService profileService,
        final PlayerSessionManager sessionManager,
        final RegistryHub registryHub,
        final DatabaseService databaseService
    ) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.taskService = Objects.requireNonNull(taskService, "taskService");
        this.gameLoop = Objects.requireNonNull(gameLoop, "gameLoop");
        this.profileService = Objects.requireNonNull(profileService, "profileService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.registryHub = Objects.requireNonNull(registryHub, "registryHub");
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
    }

    @Override
    public EventBus eventBus() {
        return this.eventBus;
    }

    @Override
    public RuntimeTaskService taskService() {
        return this.taskService;
    }

    @Override
    public GameLoop gameLoop() {
        return this.gameLoop;
    }

    @Override
    public TickManager tickManager() {
        return this.gameLoop;
    }

    @Override
    public PlayerProfileService profileService() {
        return this.profileService;
    }

    @Override
    public PlayerSessionManager sessionManager() {
        return this.sessionManager;
    }

    @Override
    public RegistryHub registryHub() {
        return this.registryHub;
    }

    @Override
    public DatabaseService databaseService() {
        return this.databaseService;
    }
}

