package com.ascension.effects.runtime;

import java.util.Objects;

/**
 * Stable descriptor of what/who caused an effect.
 *
 * @param owner owning module (e.g., "skills", "items", "environmental")
 * @param sourceId specific identifier within the module (e.g., "fireball_skill")
 */
public record EffectSource(String owner, String sourceId) {
    public EffectSource {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(sourceId, "sourceId");
    }
}
