package com.ascension.database.migration;

import com.ascension.database.service.DatabaseService;
import com.ascension.registry.Registry;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Applies pending schema migrations and records their execution.
 */
public final class DatabaseMigrationService {

    private final DatabaseService databaseService;
    private final Registry<String, SchemaMigration> migrations;

    public DatabaseMigrationService(
        final DatabaseService databaseService,
        final Registry<String, SchemaMigration> migrations
    ) {
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
        this.migrations = Objects.requireNonNull(migrations, "migrations");
    }

    /**
     * Applies all pending schema migrations in deterministic order.
     *
     * @return completion future
     */
    public CompletableFuture<Void> migrate() {
        return this.databaseService.inTransaction(transaction -> {
            transaction.update(
                """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    id VARCHAR(128) PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at BIGINT NOT NULL
                )
                """,
                statement -> {
                }
            );

            final Set<String> applied = Set.copyOf(
                transaction.queryList(
                    "SELECT id FROM schema_migrations",
                    statement -> {
                    },
                    resultSet -> resultSet.getString("id")
                )
            );

            final List<SchemaMigration> orderedMigrations = this.migrations.values().stream()
                .sorted(Comparator.comparing(SchemaMigration::id))
                .toList();

            final long appliedAt = System.currentTimeMillis();
            for (final SchemaMigration migration : orderedMigrations) {
                if (applied.contains(migration.id())) {
                    continue;
                }

                migration.migrate(transaction);
                transaction.update(
                    "INSERT INTO schema_migrations (id, description, applied_at) VALUES (?, ?, ?)",
                    statement -> {
                        statement.setString(1, migration.id());
                        statement.setString(2, migration.description());
                        statement.setLong(3, appliedAt);
                    }
                );
            }
            return null;
        });
    }
}

