package com.ascension.abilities.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Runtime request to execute a registered ability.
 *
 * <p>It contains intent only. Validation, costs, cooldowns and execution are
 * owned by the ability service.</p>
 */
public record AbilityRequest(
    UUID actorId,
    String abilityId,
    UUID targetId
) {

    public AbilityRequest {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(abilityId, "abilityId");
        if (abilityId.isBlank()) {
            throw new IllegalArgumentException("abilityId cannot be blank");
        }
    }
}
