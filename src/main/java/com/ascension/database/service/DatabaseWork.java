package com.ascension.database.service;

import java.sql.SQLException;

/**
 * Unit of database work executed on the persistence executor.
 *
 * @param <T> return type
 */
@FunctionalInterface
public interface DatabaseWork<T> {

    /**
     * Executes against a database transaction handle.
     *
     * @param transaction transaction or connection-backed session
     * @return result value
     * @throws SQLException when execution fails
     */
    T execute(DatabaseTransaction transaction) throws SQLException;
}

