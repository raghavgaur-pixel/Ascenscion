package com.ascension.database.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Generic async repository contract.
 *
 * @param <ID> identifier type
 * @param <T> aggregate type
 */
public interface AsyncRepository<ID, T> {

    /**
     * Finds an aggregate by its identifier.
     *
     * @param id identifier
     * @return optional aggregate
     */
    CompletableFuture<Optional<T>> findById(ID id);

    /**
     * Saves an aggregate.
     *
     * @param value aggregate
     * @return completion future
     */
    CompletableFuture<Void> save(T value);
}
