package com.ascension.core.config.typed;

import java.util.Collection;

/**
 * Runtime service for owned, typed, validated configuration files.
 */
public interface TypedConfigurationService {

    /**
     * Registers and loads a typed configuration.
     *
     * @param descriptor configuration descriptor
     * @param <T> configuration type
     * @return loaded configuration
     */
    <T> ManagedConfiguration<T> registerAndLoad(TypedConfigurationDescriptor<T> descriptor);

    /**
     * Reloads a configuration and swaps it only if validation succeeds.
     *
     * @param descriptor configuration descriptor
     * @param <T> configuration type
     * @return reloaded configuration
     */
    <T> ManagedConfiguration<T> reload(TypedConfigurationDescriptor<T> descriptor);

    /**
     * Resolves a previously loaded configuration.
     *
     * @param descriptor configuration descriptor
     * @param <T> configuration type
     * @return loaded configuration
     */
    <T> ManagedConfiguration<T> require(TypedConfigurationDescriptor<T> descriptor);

    /**
     * Reloads every configuration owned by a module.
     *
     * @param owner owner identifier
     */
    void reloadOwner(String owner);

    /**
     * @return immutable snapshot of loaded configurations
     */
    Collection<ManagedConfiguration<?>> loaded();
}

