package com.ascension.session.service;

import com.ascension.events.EventBus;
import com.ascension.events.lifecycle.PlayerLeavingEvent;
import com.ascension.events.lifecycle.PlayerReadyEvent;
import com.ascension.events.lifecycle.PlayerSessionCreatedEvent;
import com.ascension.events.lifecycle.PlayerSessionDestroyedEvent;
import com.ascension.events.lifecycle.PlayerSessionLoadedEvent;
import com.ascension.events.lifecycle.PlayerSessionSavedEvent;
import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.session.model.PlayerSession;
import com.ascension.session.model.PlayerSessionState;
import com.ascension.task.RuntimeTaskService;
import com.ascension.core.logging.PluginLogger;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Default player session manager coordinating runtime sessions and persisted profiles.
 */
public final class DefaultPlayerSessionManager implements PlayerSessionManager {

    private final JavaPlugin plugin;
    private final PluginLogger logger;
    private final EventBus eventBus;
    private final RuntimeTaskService taskService;
    private final PlayerProfileService profileService;
    private final ConcurrentHashMap<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public DefaultPlayerSessionManager(
        final JavaPlugin plugin,
        final PluginLogger logger,
        final EventBus eventBus,
        final RuntimeTaskService taskService,
        final PlayerProfileService profileService
    ) {
        this.plugin = plugin;
        this.logger = logger;
        this.eventBus = eventBus;
        this.taskService = taskService;
        this.profileService = profileService;
    }

    @Override
    public CompletableFuture<PlayerSession> handleJoin(final Player player) {
        final PlayerSession existing = this.sessions.get(player.getUniqueId());
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        final PlayerSession session = new PlayerSession(player.getUniqueId(), Instant.now());
        if (!session.transitionLifecycle(PlayerSessionState.LOADING_PROFILE)) {
            throw new IllegalStateException("Failed to transition session into loading state.");
        }
        this.sessions.put(player.getUniqueId(), session);
        this.eventBus.publish(new PlayerSessionCreatedEvent(session));

        final ProfileLoadRequest request = new ProfileLoadRequest(
            player.getUniqueId(),
            player.getName(),
            player.displayName().toString(),
            Instant.now()
        );

        return this.profileService.load(request)
            .thenCompose(profile -> this.taskService.runSync("sessions", "initialize-session-" + player.getUniqueId(), () -> {
                initializeSession(session, player, profile);
                return session;
            }))
            .whenComplete((loadedSession, throwable) -> {
                if (throwable == null) {
                    return;
                }

                this.sessions.remove(player.getUniqueId(), session);
                session.dispose();
                this.logger.error("Failed to initialize session for player " + player.getUniqueId() + ".", throwable);
                this.taskService.runSync("sessions", "kick-session-failure-" + player.getUniqueId(), () -> {
                    if (player.isOnline()) {
                        player.kickPlayer("Failed to initialize your Ascension session.");
                    }
                    this.eventBus.publish(new PlayerSessionDestroyedEvent(session));
                    return null;
                });
            });
    }

    @Override
    public CompletableFuture<Void> handleQuit(final UUID uniqueId) {
        final PlayerSession session = this.sessions.remove(uniqueId);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }

        session.transitionLifecycle(PlayerSessionState.LEAVING);
        this.eventBus.publish(new PlayerLeavingEvent(session));
        session.updatePlaytime();
        session.dispose();

        return this.profileService.unload(uniqueId)
            .handle((ignored, throwable) -> throwable)
            .thenCompose(throwable -> this.taskService.runSync("sessions", "finalize-session-" + uniqueId, () -> {
                if (throwable == null) {
                    this.eventBus.publish(new PlayerSessionSavedEvent(session));
                } else {
                    this.logger.error("Failed to unload session for player " + uniqueId + ".", throwable);
                }
                this.eventBus.publish(new PlayerSessionDestroyedEvent(session));
                return null;
            }));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        final CompletableFuture<?>[] futures = this.sessions.keySet().stream()
            .map(this::handleQuit)
            .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    @Override
    public Optional<PlayerSession> session(final UUID uniqueId) {
        return Optional.ofNullable(this.sessions.get(uniqueId));
    }

    @Override
    public Collection<PlayerSession> sessions() {
        return java.util.List.copyOf(this.sessions.values());
    }

    private void initializeSession(final PlayerSession session, final Player player, final PlayerProfile profile) {
        if (!player.isOnline() || !this.plugin.isEnabled()) {
            this.sessions.remove(player.getUniqueId(), session);
            session.dispose();
            this.profileService.unload(player.getUniqueId())
                .exceptionally(throwable -> {
                    this.logger.error("Failed to unload abandoned session for player " + player.getUniqueId() + ".", throwable);
                    return null;
                });
            this.eventBus.publish(new PlayerSessionDestroyedEvent(session));
            return;
        }

        session.attachProfile(profile);
        session.setCurrentWorldName(player.getWorld().getName());
        session.setCurrentRegionId(player.getLocation().getBlockX() + ":" + player.getLocation().getBlockZ());
        session.transitionLifecycle(PlayerSessionState.READY);

        this.eventBus.publish(new PlayerSessionLoadedEvent(session));
        this.eventBus.publish(new PlayerReadyEvent(session));
    }
}
