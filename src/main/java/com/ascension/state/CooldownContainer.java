package com.ascension.state;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cooldown store keyed by runtime identifiers.
 */
public final class CooldownContainer {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Applies a cooldown.
     *
     * @param key cooldown key
     * @param duration cooldown duration
     */
    public void put(final String key, final Duration duration) {
        this.cooldowns.put(key, System.currentTimeMillis() + duration.toMillis());
    }

    /**
     * Checks whether a cooldown is still active.
     *
     * @param key cooldown key
     * @return {@code true} if active
     */
    public boolean active(final String key) {
        return this.remaining(key).map(remaining -> remaining.toMillis() > 0L).orElse(false);
    }

    /**
     * Reads the remaining cooldown duration.
     *
     * @param key cooldown key
     * @return remaining duration if active
     */
    public Optional<Duration> remaining(final String key) {
        final Long expiresAt = this.cooldowns.get(key);
        if (expiresAt == null) {
            return Optional.empty();
        }

        final long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            this.cooldowns.remove(key);
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(remaining));
    }

    /**
     * Removes a cooldown.
     *
     * @param key cooldown key
     */
    public void clear(final String key) {
        this.cooldowns.remove(key);
    }

    /**
     * Clears all expired cooldowns.
     */
    public void purgeExpired() {
        final long now = System.currentTimeMillis();
        this.cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    /**
     * Clears all cooldowns.
     */
    public void clear() {
        this.cooldowns.clear();
    }
}
