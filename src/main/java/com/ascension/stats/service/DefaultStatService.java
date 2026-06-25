package com.ascension.stats.service;

import com.ascension.assets.model.AssetId;
import com.ascension.registry.Registry;
import com.ascension.stats.calculation.DerivedStatCalculator;
import com.ascension.stats.calculation.DerivedStatCalculatorFactory;
import com.ascension.stats.definition.StatDefinition;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry-backed stat service with lazy derived calculator construction.
 */
public final class DefaultStatService implements StatService {

    private final Registry<AssetId, StatDefinition> definitions;
    private final Registry<String, DerivedStatCalculatorFactory> calculatorFactories;
    private final Map<AssetId, DerivedStatCalculator> calculators = new ConcurrentHashMap<>();

    public DefaultStatService(
        final Registry<AssetId, StatDefinition> definitions,
        final Registry<String, DerivedStatCalculatorFactory> calculatorFactories
    ) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.calculatorFactories = Objects.requireNonNull(calculatorFactories, "calculatorFactories");
    }

    @Override
    public Optional<StatDefinition> find(final AssetId statId) {
        return this.definitions.find(statId);
    }

    @Override
    public StatDefinition require(final AssetId statId) {
        return this.definitions.require(statId);
    }

    @Override
    public Optional<DerivedStatCalculator> calculator(final AssetId statId) {
        final StatDefinition definition = this.find(statId).orElse(null);
        if (definition == null || definition.formula().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.calculators.computeIfAbsent(statId, ignored -> {
            final String type = definition.formula().orElseThrow().type();
            final DerivedStatCalculatorFactory factory = this.calculatorFactories.require(type);
            return factory.create(definition);
        }));
    }

    @Override
    public Collection<StatDefinition> definitions() {
        return this.definitions.values();
    }

    @Override
    public Set<AssetId> statIds() {
        return Set.copyOf(this.definitions.keys());
    }
}
