package com.ascension.session.model;

import com.ascension.effects.runtime.EffectContainer;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.session.runtime.SessionTaskContainer;
import com.ascension.stats.attribute.AttributeContainer;
import com.ascension.state.ContextVariables;
import com.ascension.state.CooldownContainer;
import com.ascension.state.RuntimeFlagContainer;
import com.ascension.state.StateMachine;
import com.ascension.state.TemporaryMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Disposable runtime session state for an online player.
 */
public final class PlayerSession {

    private final UUID uniqueId;
    private final Instant connectedAt;
    private final StateMachine<PlayerSessionState> lifecycleState;
    private final StateMachine<SessionCombatState> combatState;
    private final AttributeContainer attributes;
    private final ContextVariables runtimeAttributes;
    private final EffectContainer activeEffects;
    private final SessionTaskContainer activeTasks;
    private final CooldownContainer cooldowns;
    private final RuntimeFlagContainer runtimeFlags;
    private final TemporaryMetadata temporaryMetadata;

    private volatile boolean online;
    private volatile PlayerProfile profile;
    private volatile String currentFloorId;
    private volatile String currentWorldName;
    private volatile String currentRegionId;
    private volatile String activeGuiId;
    private volatile String partyId;
    private volatile String guildId;

    public PlayerSession(final UUID uniqueId, final Instant connectedAt, final AttributeContainer attributes) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt");
        this.lifecycleState = new StateMachine<>(PlayerSessionState.CREATED, PlayerSession::validLifecycleTransition);
        this.combatState = new StateMachine<>(SessionCombatState.IDLE, (current, next) -> true);
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.runtimeAttributes = new ContextVariables();
        this.activeEffects = new EffectContainer();
        this.activeTasks = new SessionTaskContainer();
        this.cooldowns = new CooldownContainer();
        this.runtimeFlags = new RuntimeFlagContainer();
        this.temporaryMetadata = new TemporaryMetadata();
        this.online = true;
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public Instant connectedAt() {
        return this.connectedAt;
    }

    public boolean online() {
        return this.online;
    }

    public Optional<PlayerProfile> profile() {
        return Optional.ofNullable(this.profile);
    }

    public PlayerSessionState lifecycleState() {
        return this.lifecycleState.current();
    }

    public SessionCombatState combatState() {
        return this.combatState.current();
    }

    public AttributeContainer attributes() {
        return this.attributes;
    }

    public ContextVariables runtimeAttributes() {
        return this.runtimeAttributes;
    }

    public EffectContainer activeEffects() {
        return this.activeEffects;
    }

    public SessionTaskContainer activeTasks() {
        return this.activeTasks;
    }

    public CooldownContainer cooldowns() {
        return this.cooldowns;
    }

    public RuntimeFlagContainer runtimeFlags() {
        return this.runtimeFlags;
    }

    public TemporaryMetadata temporaryMetadata() {
        return this.temporaryMetadata;
    }

    public Optional<String> currentFloorId() {
        return Optional.ofNullable(this.currentFloorId);
    }

    public Optional<String> currentWorldName() {
        return Optional.ofNullable(this.currentWorldName);
    }

    public Optional<String> currentRegionId() {
        return Optional.ofNullable(this.currentRegionId);
    }

    public Optional<String> activeGuiId() {
        return Optional.ofNullable(this.activeGuiId);
    }

    public Optional<String> partyId() {
        return Optional.ofNullable(this.partyId);
    }

    public Optional<String> guildId() {
        return Optional.ofNullable(this.guildId);
    }

    public boolean transitionLifecycle(final PlayerSessionState nextState) {
        return this.lifecycleState.transitionTo(nextState);
    }

    public boolean transitionCombat(final SessionCombatState nextState) {
        return this.combatState.transitionTo(nextState);
    }

    public void attachProfile(final PlayerProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public void setCurrentFloorId(final String currentFloorId) {
        this.currentFloorId = currentFloorId;
    }

    public void setCurrentWorldName(final String currentWorldName) {
        this.currentWorldName = currentWorldName;
    }

    public void setCurrentRegionId(final String currentRegionId) {
        this.currentRegionId = currentRegionId;
    }

    public void setActiveGuiId(final String activeGuiId) {
        this.activeGuiId = activeGuiId;
    }

    public void setPartyId(final String partyId) {
        this.partyId = partyId;
    }

    public void setGuildId(final String guildId) {
        this.guildId = guildId;
    }

    public void updatePlaytime() {
        final PlayerProfile currentProfile = this.profile;
        if (currentProfile == null) {
            return;
        }

        final long seconds = Duration.between(this.connectedAt, Instant.now()).toSeconds();
        if (seconds > 0L) {
            currentProfile.addPlaytimeSeconds(seconds);
        }
    }

    public void dispose() {
        this.online = false;
        this.activeTasks.cancelAll();
        this.activeEffects.clear();
        this.cooldowns.clear();
        this.runtimeFlags.clear();
        this.temporaryMetadata.clear();
        this.attributes.clear();
        this.runtimeAttributes.clear();
        this.currentFloorId = null;
        this.currentWorldName = null;
        this.currentRegionId = null;
        this.activeGuiId = null;
        this.partyId = null;
        this.guildId = null;
        this.transitionLifecycle(PlayerSessionState.DESTROYED);
    }

    private static boolean validLifecycleTransition(
        final PlayerSessionState current,
        final PlayerSessionState next
    ) {
        return switch (current) {
            case CREATED -> next == PlayerSessionState.LOADING_PROFILE
                || next == PlayerSessionState.LEAVING
                || next == PlayerSessionState.DESTROYED;
            case LOADING_PROFILE -> next == PlayerSessionState.READY
                || next == PlayerSessionState.LEAVING
                || next == PlayerSessionState.DESTROYED;
            case READY -> next == PlayerSessionState.LEAVING || next == PlayerSessionState.DESTROYED;
            case LEAVING -> next == PlayerSessionState.DESTROYED;
            case DESTROYED -> false;
        };
    }
}
