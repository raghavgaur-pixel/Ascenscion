package com.ascension.profiles.model;

import com.ascension.profiles.component.ProfileComponentContainer;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Root player profile aggregate with composable subsystem state.
 */
public final class PlayerProfile {

    private final UUID uniqueId;
    private final Instant firstJoin;
    private final ProfileComponentContainer components;

    private String username;
    private String displayName;
    private Instant lastJoin;
    private long playtimeSeconds;

    public PlayerProfile(
        final UUID uniqueId,
        final String username,
        final String displayName,
        final Instant firstJoin,
        final Instant lastJoin,
        final long playtimeSeconds,
        final ProfileComponentContainer components
    ) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.firstJoin = Objects.requireNonNull(firstJoin, "firstJoin");
        this.lastJoin = Objects.requireNonNull(lastJoin, "lastJoin");
        this.playtimeSeconds = playtimeSeconds;
        this.components = Objects.requireNonNull(components, "components");
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public synchronized String username() {
        return this.username;
    }

    public synchronized String displayName() {
        return this.displayName;
    }

    public Instant firstJoin() {
        return this.firstJoin;
    }

    public synchronized Instant lastJoin() {
        return this.lastJoin;
    }

    public synchronized long playtimeSeconds() {
        return this.playtimeSeconds;
    }

    public ProfileComponentContainer components() {
        return this.components;
    }

    public synchronized void updateIdentity(final String username, final String displayName, final Instant lastJoin) {
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.lastJoin = Objects.requireNonNull(lastJoin, "lastJoin");
    }

    public synchronized void addPlaytimeSeconds(final long additionalSeconds) {
        this.playtimeSeconds += additionalSeconds;
    }
}

