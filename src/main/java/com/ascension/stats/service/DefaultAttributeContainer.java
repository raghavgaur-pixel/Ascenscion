package com.ascension.stats.service;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.attribute.AttributeContainer;
import com.ascension.stats.attribute.AttributeSnapshot;
import com.ascension.stats.attribute.AttributeValueSnapshot;
import com.ascension.stats.definition.StatDefinition;
import com.ascension.stats.modifier.AttributeModifier;
import com.ascension.stats.modifier.ModifierStackingBehavior;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default session-owned attribute container with caching and dirty tracking.
 */
final class DefaultAttributeContainer implements AttributeContainer {

    private final StatService statService;
    private final DefaultAttributeCalculationService calculationService;
    private final Map<AssetId, Entry> entries = new LinkedHashMap<>();
    private long revision;

    DefaultAttributeContainer(
        final StatService statService,
        final DefaultAttributeCalculationService calculationService
    ) {
        this.statService = statService;
        this.calculationService = calculationService;
        for (final AssetId statId : statService.statIds()) {
            this.register(statId);
        }
    }

    @Override
    public synchronized Optional<StatDefinition> register(final AssetId statId) {
        final StatDefinition definition = this.statService.find(statId).orElse(null);
        if (definition == null) {
            return Optional.empty();
        }
        this.entries.computeIfAbsent(statId, ignored -> new Entry(definition.defaultBaseValue()));
        return Optional.of(definition);
    }

    @Override
    public synchronized Optional<StatDefinition> stat(final AssetId statId) {
        return this.statService.find(statId);
    }

    @Override
    public synchronized double finalValue(final AssetId statId) {
        final StatDefinition definition = this.statService.require(statId);
        final Entry entry = this.entries.computeIfAbsent(statId, ignored -> new Entry(definition.defaultBaseValue()));
        purgeExpiredModifiers(entry);
        if (entry.dirty) {
            entry.cachedFinalValue = this.calculationService.calculate(this, statId);
            entry.dirty = false;
        }
        return entry.cachedFinalValue;
    }

    @Override
    public synchronized double baseValue(final AssetId statId) {
        return this.entries.computeIfAbsent(
            statId,
            ignored -> new Entry(this.statService.require(statId).defaultBaseValue())
        ).baseValue;
    }

    @Override
    public synchronized Collection<AssetId> statIds() {
        return Set.copyOf(this.entries.keySet());
    }

    @Override
    public synchronized void setBaseValue(final AssetId statId, final double value) {
        final StatDefinition definition = this.statService.require(statId);
        final Entry entry = this.entries.computeIfAbsent(statId, ignored -> new Entry(definition.defaultBaseValue()));
        entry.baseValue = clampBase(definition, value);
        markDirty(statId);
    }

    @Override
    public synchronized void addModifier(final AssetId statId, final AttributeModifier modifier) {
        this.statService.require(statId);
        final Entry entry = this.entries.computeIfAbsent(
            statId,
            ignored -> new Entry(this.statService.require(statId).defaultBaseValue())
        );
        applyStacking(entry.modifiers, modifier);
        markDirty(statId);
    }

    @Override
    public synchronized boolean removeModifier(final AssetId statId, final String modifierId) {
        final Entry entry = this.entries.get(statId);
        if (entry == null) {
            return false;
        }
        final boolean removed = entry.modifiers.removeIf(modifier -> modifier.id().equals(modifierId));
        if (removed) {
            markDirty(statId);
        }
        return removed;
    }

    @Override
    public synchronized void removeSource(final String sourceId) {
        for (final Map.Entry<AssetId, Entry> entry : this.entries.entrySet()) {
            final boolean removed = entry.getValue().modifiers.removeIf(modifier -> modifier.source().id().equals(sourceId));
            if (removed) {
                markDirty(entry.getKey());
            }
        }
    }

    @Override
    public synchronized Collection<AttributeModifier> modifiers(final AssetId statId) {
        final Entry entry = this.entries.get(statId);
        if (entry == null) {
            return List.of();
        }
        purgeExpiredModifiers(entry);
        return List.copyOf(entry.modifiers);
    }

    @Override
    public synchronized boolean dirty(final AssetId statId) {
        return this.entries.getOrDefault(statId, new Entry(0.0D)).dirty;
    }

    @Override
    public synchronized AttributeSnapshot snapshot() {
        final Map<AssetId, AttributeValueSnapshot> snapshot = new LinkedHashMap<>();
        for (final AssetId statId : this.entries.keySet()) {
            final Entry entry = this.entries.get(statId);
            final double finalValue = this.finalValue(statId);
            snapshot.put(
                statId,
                new AttributeValueSnapshot(
                    statId,
                    entry.baseValue,
                    finalValue,
                    entry.dirty,
                    List.copyOf(entry.modifiers)
                )
            );
        }
        return new AttributeSnapshot(snapshot, this.revision);
    }

    @Override
    public synchronized void clear() {
        this.entries.clear();
        this.revision++;
    }

    synchronized double rawBaseValue(final AssetId statId) {
        final StatDefinition definition = this.statService.require(statId);
        return this.entries.computeIfAbsent(statId, ignored -> new Entry(definition.defaultBaseValue())).baseValue;
    }

    synchronized Collection<AttributeModifier> activeModifiers(final AssetId statId, final Instant now) {
        final Entry entry = this.entries.get(statId);
        if (entry == null) {
            return List.of();
        }
        final boolean changed = purgeExpiredModifiers(entry, now);
        if (changed) {
            markDirty(statId);
        }
        return List.copyOf(entry.modifiers);
    }

    private void applyStacking(final List<AttributeModifier> modifiers, final AttributeModifier modifier) {
        switch (modifier.stackingBehavior()) {
            case STACK -> modifiers.add(modifier);
            case REPLACE_BY_ID -> {
                modifiers.removeIf(existing -> existing.id().equals(modifier.id()));
                modifiers.add(modifier);
            }
            case UNIQUE_PER_SOURCE -> {
                modifiers.removeIf(existing ->
                    existing.source().equals(modifier.source()) && existing.operation() == modifier.operation()
                );
                modifiers.add(modifier);
            }
            case HIGHEST_VALUE -> {
                final Optional<AttributeModifier> strongest = modifiers.stream()
                    .filter(existing ->
                        existing.source().equals(modifier.source()) && existing.operation() == modifier.operation()
                    )
                    .max(java.util.Comparator.comparingDouble(AttributeModifier::value));
                if (strongest.isEmpty() || strongest.orElseThrow().value() <= modifier.value()) {
                    strongest.ifPresent(modifiers::remove);
                    modifiers.add(modifier);
                }
            }
            case REFRESH_DURATION -> {
                modifiers.removeIf(existing ->
                    existing.source().equals(modifier.source()) && existing.operation() == modifier.operation()
                );
                modifiers.add(modifier);
            }
        }
    }

    private void markDirty(final AssetId statId) {
        final Set<AssetId> queue = new LinkedHashSet<>();
        queue.add(statId);
        while (!queue.isEmpty()) {
            final AssetId current = queue.iterator().next();
            queue.remove(current);
            final Entry currentEntry = this.entries.computeIfAbsent(
                current,
                ignored -> new Entry(this.statService.require(current).defaultBaseValue())
            );
            currentEntry.dirty = true;
            queue.addAll(this.calculationService.dependentsOf(current));
        }
        this.revision++;
    }

    private boolean purgeExpiredModifiers(final Entry entry) {
        return purgeExpiredModifiers(entry, Instant.now());
    }

    private boolean purgeExpiredModifiers(final Entry entry, final Instant now) {
        return entry.modifiers.removeIf(modifier -> modifier.expired(now));
    }

    private static double clampBase(final StatDefinition definition, final double value) {
        double clamped = value;
        if (definition.minimumValue().isPresent()) {
            clamped = Math.max(clamped, definition.minimumValue().orElseThrow());
        }
        if (definition.maximumValue().isPresent()) {
            clamped = Math.min(clamped, definition.maximumValue().orElseThrow());
        }
        if (Double.isNaN(clamped)) {
            return 0.0D;
        }
        if (Double.isInfinite(clamped)) {
            return clamped > 0.0D ? Double.MAX_VALUE : -Double.MAX_VALUE;
        }
        return clamped;
    }

    private static final class Entry {

        private double baseValue;
        private double cachedFinalValue;
        private boolean dirty = true;
        private final List<AttributeModifier> modifiers = new ArrayList<>();

        private Entry(final double baseValue) {
            this.baseValue = baseValue;
            this.cachedFinalValue = baseValue;
        }
    }
}
