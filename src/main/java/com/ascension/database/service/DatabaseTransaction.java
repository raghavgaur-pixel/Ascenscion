package com.ascension.database.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Connection-scoped database operations backed by prepared statements.
 */
public interface DatabaseTransaction {

    /**
     * Executes an update statement.
     *
     * @param sql sql statement
     * @param binder parameter binder
     * @return affected row count
     * @throws SQLException when execution fails
     */
    int update(String sql, StatementBinder binder) throws SQLException;

    /**
     * Reads a single row from the database.
     *
     * @param sql sql statement
     * @param binder parameter binder
     * @param mapper row mapper
     * @param <T> mapped type
     * @return optional mapped value
     * @throws SQLException when execution fails
     */
    <T> Optional<T> queryOne(String sql, StatementBinder binder, ResultSetMapper<T> mapper) throws SQLException;

    /**
     * Reads multiple rows from the database.
     *
     * @param sql sql statement
     * @param binder parameter binder
     * @param mapper row mapper
     * @param <T> mapped type
     * @return mapped list
     * @throws SQLException when execution fails
     */
    <T> List<T> queryList(String sql, StatementBinder binder, ResultSetMapper<T> mapper) throws SQLException;
}

