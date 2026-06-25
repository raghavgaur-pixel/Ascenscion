package com.ascension.database.service;

import com.ascension.database.model.DatabaseType;
import java.util.concurrent.CompletableFuture;

/**
 * Async database access service and transaction boundary.
 */
public interface DatabaseService {

    /**
     * Starts the connection pool and worker executor.
     */
    void start();

    /**
     * Stops the connection pool and worker executor.
     */
    void stop();

    /**
     * Executes non-transactional database work.
     *
     * @param work unit of work
     * @param <T> return type
     * @return async result
     */
    <T> CompletableFuture<T> execute(DatabaseWork<T> work);

    /**
     * Executes transactional database work.
     *
     * @param work unit of work
     * @param <T> return type
     * @return async result
     */
    <T> CompletableFuture<T> inTransaction(DatabaseWork<T> work);

    /**
     * @return active database engine
     */
    DatabaseType type();
}

