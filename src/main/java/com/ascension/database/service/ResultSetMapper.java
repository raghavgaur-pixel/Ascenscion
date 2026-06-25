package com.ascension.database.service;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a result-set row to a domain value.
 *
 * @param <T> mapped type
 */
@FunctionalInterface
public interface ResultSetMapper<T> {

    /**
     * Maps the current row.
     *
     * @param resultSet result set positioned on a row
     * @return mapped value
     * @throws SQLException when mapping fails
     */
    T map(ResultSet resultSet) throws SQLException;
}

