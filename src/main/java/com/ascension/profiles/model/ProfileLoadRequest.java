package com.ascension.profiles.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable input required to load or create a player profile.
 *
 * @param uniqueId player uuid
 * @param username latest username
 * @param displayName latest display name
 * @param joinedAt current join instant
 */
public record ProfileLoadRequest(
    UUID uniqueId,
    String username,
    String displayName,
    Instant joinedAt
) {

    public ProfileLoadRequest {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(joinedAt, "joinedAt");
    }
}

