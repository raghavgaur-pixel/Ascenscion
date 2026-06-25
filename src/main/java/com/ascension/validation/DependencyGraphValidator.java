package com.ascension.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Validates dependencies and cycle safety for keyed definitions.
 */
public final class DependencyGraphValidator<T, K> {

    private final Function<T, K> keyExtractor;
    private final Function<T, Collection<K>> dependencyExtractor;

    public DependencyGraphValidator(
        final Function<T, K> keyExtractor,
        final Function<T, Collection<K>> dependencyExtractor
    ) {
        this.keyExtractor = keyExtractor;
        this.dependencyExtractor = dependencyExtractor;
    }

    /**
     * Validates missing references and cycles.
     *
     * @param values values to validate
     * @param pathPrefix logical path prefix
     * @return validation report
     */
    public ValidationReport validate(final Collection<T> values, final String pathPrefix) {
        final ValidationCollector collector = new ValidationCollector();
        final Map<K, T> indexed = new HashMap<>();
        for (final T value : values) {
            indexed.put(this.keyExtractor.apply(value), value);
        }

        for (final T value : values) {
            final K key = this.keyExtractor.apply(value);
            for (final K dependency : this.dependencyExtractor.apply(value)) {
                if (!indexed.containsKey(dependency)) {
                    collector.error(
                        "missing_dependency",
                        pathPrefix + "." + key,
                        "Missing dependency: " + dependency
                    );
                }
            }
        }

        final Set<K> visiting = new HashSet<>();
        final Set<K> visited = new HashSet<>();
        for (final K key : indexed.keySet()) {
            visit(key, indexed, visiting, visited, collector, pathPrefix);
        }

        return collector.report();
    }

    private void visit(
        final K key,
        final Map<K, T> indexed,
        final Set<K> visiting,
        final Set<K> visited,
        final ValidationCollector collector,
        final String pathPrefix
    ) {
        if (visited.contains(key)) {
            return;
        }
        if (!visiting.add(key)) {
            collector.error("cyclic_dependency", pathPrefix + "." + key, "Dependency cycle detected at " + key);
            return;
        }

        final T value = indexed.get(key);
        if (value != null) {
            for (final K dependency : this.dependencyExtractor.apply(value)) {
                if (indexed.containsKey(dependency)) {
                    visit(dependency, indexed, visiting, visited, collector, pathPrefix);
                }
            }
        }

        visiting.remove(key);
        visited.add(key);
    }
}

