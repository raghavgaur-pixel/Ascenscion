package com.ascension.database.migration;

import com.ascension.database.service.DatabaseTransaction;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * SQL-backed migration implementation.
 */
public final class SqlSchemaMigration implements SchemaMigration {

    private final String id;
    private final String description;
    private final List<String> statements;

    public SqlSchemaMigration(final String id, final String description, final List<String> statements) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.statements = List.copyOf(statements);
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public void migrate(final DatabaseTransaction transaction) throws SQLException {
        for (final String statement : this.statements) {
            transaction.update(statement, preparedStatement -> {
            });
        }
    }
}

