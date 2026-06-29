package com.ascension.effects.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Contextual parameters for when an effect is applied.
 *
 * @param applierId the entity or player that applied the effect
 * @param targetId the entity or player receiving the effect
 */
public record EffectContext(
    Optional<UUID> applierId,
    UUID targetId
) {
    public EffectContext {
        Objects.requireNonNull(applierId, "applierId");
        Objects.requireNonNull(targetId, "targetId");
    }
}
