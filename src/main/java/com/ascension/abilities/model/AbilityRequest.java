package com.ascension.abilities.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Runtime request to execute a registered ability.
 *
 * <p>It contains player intent only. Validation, costs, cooldowns and
 * execution remain owned by the ability service.</p>
 */
public record AbilityRequest(
    UUID actorId,
    String abilityId,
    UUID targetId,
    AbilityTargetPoint targetPoint
) {

    /**
     * Backwards-compatible entity-target constructor for callers that only
     * need an actor and an optional entity target.
     */
    public AbilityRequest(final UUID actorId, final String abilityId, final UUID targetId) {
        this(actorId, abilityId, targetId, null);
    }

    public AbilityRequest {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(abilityId, "abilityId");
        if (abilityId.isBlank()) {
            throw new IllegalArgumentException("abilityId cannot be blank");
        }
        if (targetId != null && targetId.equals(actorId)) {
            // Self-targets are valid, but represented consistently through the
            // entity id rather than silently changing request semantics.
        }
    }
}
