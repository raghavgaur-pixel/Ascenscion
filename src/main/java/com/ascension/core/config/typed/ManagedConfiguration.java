package com.ascension.core.config.typed;

/**
 * Loaded typed configuration snapshot.
 *
 * @param descriptor descriptor
 * @param version loaded schema version
 * @param value typed value
 * @param <T> configuration type
 */
public record ManagedConfiguration<T>(
    TypedConfigurationDescriptor<T> descriptor,
    int version,
    T value
) {
}

