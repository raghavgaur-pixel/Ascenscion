package com.ascension.abilities.model;

import java.util.Objects;

/**
 * Platform-neutral world-space targeting point.
 *
 * <p>The world identifier is a stable application-level identifier. Bukkit
 * world objects are resolved only by platform adapters.</p>
 */
public record AbilityTargetPoint(String worldId, double x, double y, double z) {

    public AbilityTargetPoint {
        Objects.requireNonNull(worldId, "worldId");
        if (worldId.isBlank()) {
            throw new IllegalArgumentException("worldId cannot be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Target coordinates must be finite");
        }
    }
}
