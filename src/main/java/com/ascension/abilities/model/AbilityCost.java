package com.ascension.abilities.model;

import java.util.Map;
import java.util.Objects;

/**
 * Resource costs required before an ability may execute.
 *
 * <p>Keys are resource identifiers owned by the progression/resource system,
 * allowing future resources beyond mana without changing this contract.</p>
 */
public record AbilityCost(Map<String, Double> resources) {

    public AbilityCost {
        Objects.requireNonNull(resources, "resources");
        resources = Map.copyOf(resources);
        resources.forEach((key, value) -> {
            Objects.requireNonNull(key, "resource key");
            Objects.requireNonNull(value, "resource value");
            if (key.isBlank() || !Double.isFinite(value) || value < 0.0D) {
                throw new IllegalArgumentException("Invalid ability cost for resource: " + key);
            }
        });
    }

    public static AbilityCost none() {
        return new AbilityCost(Map.of());
    }
}
