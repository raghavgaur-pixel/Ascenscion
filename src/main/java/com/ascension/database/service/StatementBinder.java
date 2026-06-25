package com.ascension.database.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Binds values to a prepared statement.
 */
@FunctionalInterface
public interface StatementBinder {

    /**
     * Populates a prepared statement before execution.
     *
     * @param statement prepared statement
     * @throws SQLException when binding fails
     */
    void bind(PreparedStatement statement) throws SQLException;

    /**
     * @return binder that performs no work
     */
    static StatementBinder noop() {
        return statement -> {
        };
    }
}

