package com.ascension.session.runtime;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Generic runtime effect marker for session-owned temporary effects.
 *
 * @param effectId effect identifier
 * @param sourceId source identifier
 * @param appliedAt application timestamp
 * @param expiresAt optional expiration timestamp
 */
public record ActiveEffect(
    String effectId,
    String sourceId,
    Instant appliedAt,
    Optional<Instant> expiresAt
) {

    public ActiveEffect {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(appliedAt, "appliedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}

