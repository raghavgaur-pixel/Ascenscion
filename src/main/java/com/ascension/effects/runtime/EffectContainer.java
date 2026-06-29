package com.ascension.effects.runtime;

import com.ascension.assets.model.AssetId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe store for active runtime effects.
 */
public final class EffectContainer {

    // Maps effect instance ID to the effect instance
    private final Map<UUID, EffectInstance> instances = new ConcurrentHashMap<>();

    /**
     * Adds an active effect instance.
     *
     * @param instance effect instance
     */
    public void add(final EffectInstance instance) {
        this.instances.put(instance.instanceId(), instance);
    }

    /**
     * Reads an effect instance by ID.
     *
     * @param instanceId instance identifier
     * @return effect if present
     */
    public Optional<EffectInstance> get(final UUID instanceId) {
        return Optional.ofNullable(this.instances.get(instanceId));
    }

    /**
     * Finds active effect instances for a given effect ID.
     *
     * @param effectId effect identifier
     * @return list of active instances for the ID
     */
    public List<EffectInstance> getByEffectId(final AssetId effectId) {
        return this.instances.values().stream()
            .filter(instance -> instance.effectId().equals(effectId))
            .collect(Collectors.toList());
    }

    /**
     * Removes an effect instance.
     *
     * @param instanceId instance identifier
     * @return the removed instance, if any
     */
    public Optional<EffectInstance> remove(final UUID instanceId) {
        return Optional.ofNullable(this.instances.remove(instanceId));
    }

    /**
     * Clears every effect instance.
     */
    public void clear() {
        this.instances.clear();
    }

    /**
     * @return immutable snapshot of all instances
     */
    public Collection<EffectInstance> snapshot() {
        return List.copyOf(this.instances.values());
    }
}
