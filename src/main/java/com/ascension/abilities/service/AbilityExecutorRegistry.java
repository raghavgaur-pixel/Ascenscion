package com.ascension.abilities.service;

import com.ascension.assets.model.AssetId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for executable ability behaviors.
 *
 * <p>Definitions remain content-driven while behavior can be supplied by the
 * built-in engine or extensions. Registration is explicit and duplicate-safe.</p>
 */
public final class AbilityExecutorRegistry {

    private final Map<AssetId, AbilityExecutor> executors = new ConcurrentHashMap<>();

    public void register(final AssetId abilityId, final AbilityExecutor executor) {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(executor, "executor");
        final AbilityExecutor previous = this.executors.putIfAbsent(abilityId, executor);
        if (previous != null) {
            throw new IllegalStateException("Ability executor already registered: " + abilityId);
        }
    }

    public Optional<AbilityExecutor> find(final AssetId abilityId) {
        return Optional.ofNullable(this.executors.get(Objects.requireNonNull(abilityId, "abilityId")));
    }

    public void unregister(final AssetId abilityId) {
        this.executors.remove(Objects.requireNonNull(abilityId, "abilityId"));
    }

    public int size() {
        return this.executors.size();
    }
}
