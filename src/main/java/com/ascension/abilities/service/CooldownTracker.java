package com.ascension.abilities.service;

import com.ascension.assets.model.AssetId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime cooldown store.
 *
 * <p>Cooldown state is deliberately ephemeral and therefore belongs to the
 * session/runtime layer rather than persistent player data.</p>
 */
public final class CooldownTracker {

    private final Map<CooldownKey, Long> expiryMillis = new ConcurrentHashMap<>();

    /**
     * Attempts to acquire a cooldown atomically.
     */
    public synchronized boolean tryAcquire(
        final UUID actorId,
        final AssetId abilityId,
        final long nowMillis,
        final long durationMillis
    ) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(abilityId, "abilityId");
        if (nowMillis < 0L || durationMillis < 0L) {
            throw new IllegalArgumentException("Time values cannot be negative");
        }

        final CooldownKey key = new CooldownKey(actorId, abilityId);
        final long existingExpiry = this.expiryMillis.getOrDefault(key, 0L);
        if (existingExpiry > nowMillis) {
            return false;
        }

        if (durationMillis == 0L) {
            this.expiryMillis.remove(key);
            return true;
        }

        this.expiryMillis.put(key, Math.addExact(nowMillis, durationMillis));
        return true;
    }

    /**
     * Returns remaining cooldown in milliseconds, or zero when ready.
     */
    public long remainingMillis(final UUID actorId, final AssetId abilityId, final long nowMillis) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(abilityId, "abilityId");
        if (nowMillis < 0L) {
            throw new IllegalArgumentException("nowMillis cannot be negative");
        }

        final CooldownKey key = new CooldownKey(actorId, abilityId);
        final long expiry = this.expiryMillis.getOrDefault(key, 0L);
        if (expiry <= nowMillis) {
            this.expiryMillis.remove(key, expiry);
            return 0L;
        }
        return expiry - nowMillis;
    }

    /**
     * Clears all runtime cooldowns for an actor, normally on disconnect.
     */
    public void clear(final UUID actorId) {
        Objects.requireNonNull(actorId, "actorId");
        this.expiryMillis.keySet().removeIf(key -> key.actorId().equals(actorId));
    }

    private record CooldownKey(UUID actorId, AssetId abilityId) {
        private CooldownKey {
            Objects.requireNonNull(actorId, "actorId");
            Objects.requireNonNull(abilityId, "abilityId");
        }
    }
}
