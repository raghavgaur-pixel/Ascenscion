package com.ascension.abilities.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of reusable, data-driven ability behaviors.
 *
 * <p>Abilities select a behavior through their content payload rather than
 * requiring one executor registration per ability id. Concrete abilities can
 * still be registered in {@link AbilityExecutorRegistry} when they need a
 * bespoke implementation.</p>
 */
public final class AbilityBehaviorRegistry {

    private final Map<String, AbilityExecutor> executors = new ConcurrentHashMap<>();

    public void register(final String behavior, final AbilityExecutor executor) {
        final String normalized = normalize(behavior);
        Objects.requireNonNull(executor, "executor");
        final AbilityExecutor previous = this.executors.putIfAbsent(normalized, executor);
        if (previous != null) {
            throw new IllegalStateException("Ability behavior already registered: " + normalized);
        }
    }

    public Optional<AbilityExecutor> find(final String behavior) {
        if (behavior == null || behavior.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.executors.get(behavior.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public void unregister(final String behavior) {
        this.executors.remove(normalize(behavior));
    }

    public int size() {
        return this.executors.size();
    }

    private static String normalize(final String behavior) {
        Objects.requireNonNull(behavior, "behavior");
        final String normalized = behavior.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("behavior cannot be blank");
        }
        return normalized;
    }
}
