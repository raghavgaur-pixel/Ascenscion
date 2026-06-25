package com.ascension.stats.service;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.attribute.AttributeContainer;
import com.ascension.stats.calculation.AttributeCalculationService;
import com.ascension.stats.calculation.DerivedStatCalculator;
import com.ascension.stats.calculation.DerivedStatContext;
import com.ascension.stats.definition.StatDefinition;
import com.ascension.stats.modifier.AttributeModifier;
import com.ascension.stats.modifier.ModifierOperation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default central calculation engine for stats and modifiers.
 */
public final class DefaultAttributeCalculationService implements AttributeCalculationService {

    private final StatService statService;

    public DefaultAttributeCalculationService(final StatService statService) {
        this.statService = Objects.requireNonNull(statService, "statService");
    }

    @Override
    public double calculate(final AttributeContainer container, final AssetId statId) {
        if (!(container instanceof DefaultAttributeContainer internal)) {
            throw new IllegalArgumentException("Unsupported attribute container implementation: " + container.getClass().getName());
        }
        return this.calculateInternal(internal, statId, new LinkedHashSet<>());
    }

    @Override
    public Set<AssetId> dependentsOf(final AssetId statId) {
        final Set<AssetId> dependents = new LinkedHashSet<>();
        for (final StatDefinition definition : this.statService.definitions()) {
            this.statService.calculator(definition.id())
                .filter(calculator -> calculator.dependencies().contains(statId))
                .ifPresent(calculator -> dependents.add(calculator.targetStatId()));
        }
        return Set.copyOf(dependents);
    }

    double calculateInternal(
        final DefaultAttributeContainer container,
        final AssetId statId,
        final Set<AssetId> visiting
    ) {
        if (!visiting.add(statId)) {
            throw new IllegalStateException("Derived stat cycle detected while calculating " + statId);
        }

        final StatDefinition definition = this.statService.require(statId);
        final double resolvedBase = this.statService.calculator(statId)
            .<Double>map(calculator -> calculator.calculate(new InternalDerivedStatContext(container, visiting)))
            .orElse(container.rawBaseValue(statId));
        final Collection<AttributeModifier> modifiers = container.activeModifiers(statId, Instant.now());
        final double calculated = applyModifiers(definition, resolvedBase, modifiers);
        visiting.remove(statId);
        return calculated;
    }

    private static double applyModifiers(
        final StatDefinition definition,
        final double baseValue,
        final Collection<AttributeModifier> modifiers
    ) {
        double current = finite(baseValue);

        current = applyBaseOverride(current, modifiers);
        current = applyFlat(current, modifiers);
        current = applyPercent(current, modifiers);
        current = applyMultipliers(current, modifiers);
        current = clamp(definition, current);

        if (definition.decimalPlaces() >= 0) {
            current = BigDecimal.valueOf(current)
                .setScale(definition.decimalPlaces(), RoundingMode.HALF_UP)
                .doubleValue();
        }
        return current;
    }

    private static double applyBaseOverride(final double current, final Collection<AttributeModifier> modifiers) {
        double value = current;
        for (final AttributeModifier modifier : sorted(modifiers, ModifierOperation.BASE_OVERRIDE)) {
            value = finite(modifier.value());
        }
        return value;
    }

    private static double applyFlat(final double current, final Collection<AttributeModifier> modifiers) {
        double value = current;
        for (final AttributeModifier modifier : sorted(modifiers, ModifierOperation.FLAT)) {
            value = finite(value + modifier.value());
        }
        return value;
    }

    private static double applyPercent(final double current, final Collection<AttributeModifier> modifiers) {
        double additivePercent = 0.0D;
        for (final AttributeModifier modifier : sorted(modifiers, ModifierOperation.PERCENT_ADD)) {
            additivePercent += modifier.value();
        }
        return finite(current * (1.0D + additivePercent));
    }

    private static double applyMultipliers(final double current, final Collection<AttributeModifier> modifiers) {
        double value = current;
        for (final AttributeModifier modifier : sorted(modifiers, ModifierOperation.MULTIPLY)) {
            value = finite(value * (1.0D + modifier.value()));
        }
        return value;
    }

    private static Collection<AttributeModifier> sorted(
        final Collection<AttributeModifier> modifiers,
        final ModifierOperation operation
    ) {
        return modifiers.stream()
            .filter(modifier -> modifier.operation() == operation)
            .sorted(java.util.Comparator.comparingInt(AttributeModifier::priority).thenComparing(AttributeModifier::id))
            .toList();
    }

    private static double clamp(final StatDefinition definition, final double value) {
        double clamped = value;
        if (definition.minimumValue().isPresent()) {
            clamped = Math.max(clamped, definition.minimumValue().orElseThrow());
        }
        if (definition.maximumValue().isPresent()) {
            clamped = Math.min(clamped, definition.maximumValue().orElseThrow());
        }
        return finite(clamped);
    }

    private static double finite(final double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        if (Double.isInfinite(value)) {
            return value > 0.0D ? Double.MAX_VALUE : -Double.MAX_VALUE;
        }
        return value;
    }

    private final class InternalDerivedStatContext implements DerivedStatContext {

        private final DefaultAttributeContainer container;
        private final Set<AssetId> visiting;

        private InternalDerivedStatContext(final DefaultAttributeContainer container, final Set<AssetId> visiting) {
            this.container = container;
            this.visiting = visiting;
        }

        @Override
        public double finalValue(final AssetId statId) {
            return DefaultAttributeCalculationService.this.calculateInternal(
                this.container,
                statId,
                new LinkedHashSet<>(this.visiting)
            );
        }
    }
}
