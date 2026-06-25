package com.ascension.database.migration;

import com.ascension.database.service.DatabaseTransaction;
import java.sql.SQLException;

/**
 * Ordered schema migration.
 */
public interface SchemaMigration {

    /**
     * @return stable unique migration identifier
     */
    String id();

    /**
     * @return human-readable migration description
     */
    String description();

    /**
     * Applies the migration.
     *
     * @param transaction transaction handle
     * @throws SQLException when migration fails
     */
    void migrate(DatabaseTransaction transaction) throws SQLException;
}

