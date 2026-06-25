package com.ascension.validation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility for duplicate detection during validation.
 */
public final class DuplicateDetector<T, K> {

    private final Function<T, K> keyExtractor;

    public DuplicateDetector(final Function<T, K> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Validates uniqueness across a collection.
     *
     * @param values values to inspect
     * @param pathPrefix logical path prefix
     * @return validation report
     */
    public ValidationReport validate(final Iterable<T> values, final String pathPrefix) {
        final ValidationCollector collector = new ValidationCollector();
        final Set<K> seen = new HashSet<>();
        int index = 0;
        for (final T value : values) {
            final K key = this.keyExtractor.apply(value);
            if (!seen.add(key)) {
                collector.error("duplicate", pathPrefix + "[" + index + "]", "Duplicate key detected: " + key);
            }
            index++;
        }
        return collector.report();
    }
}

