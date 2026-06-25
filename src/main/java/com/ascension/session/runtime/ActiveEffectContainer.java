package com.ascension.session.runtime;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe store for active runtime effects.
 */
public final class ActiveEffectContainer {

    private final Map<String, ActiveEffect> effects = new ConcurrentHashMap<>();

    /**
     * Applies or replaces an active effect.
     *
     * @param effect effect descriptor
     */
    public void put(final ActiveEffect effect) {
        this.effects.put(effect.effectId(), effect);
    }

    /**
     * Reads an effect by identifier.
     *
     * @param effectId effect identifier
     * @return effect if present
     */
    public Optional<ActiveEffect> get(final String effectId) {
        return Optional.ofNullable(this.effects.get(effectId));
    }

    /**
     * Removes an effect.
     *
     * @param effectId effect identifier
     */
    public void remove(final String effectId) {
        this.effects.remove(effectId);
    }

    /**
     * Clears every effect.
     */
    public void clear() {
        this.effects.clear();
    }

    /**
     * @return immutable snapshot
     */
    public Collection<ActiveEffect> snapshot() {
        return java.util.List.copyOf(this.effects.values());
    }
}

