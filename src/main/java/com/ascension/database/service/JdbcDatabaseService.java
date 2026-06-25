package com.ascension.database.service;

import com.ascension.core.logging.PluginLogger;
import com.ascension.database.model.DatabaseSettings;
import com.ascension.database.model.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JDBC-backed async database service with connection pooling and transaction support.
 */
public final class JdbcDatabaseService implements DatabaseService {

    private final PluginLogger logger;
    private final DatabaseSettings settings;
    private final HikariConfig configuration;
    private HikariDataSource dataSource;
    private ExecutorService executorService;

    public JdbcDatabaseService(
        final PluginLogger logger,
        final DatabaseSettings settings,
        final HikariConfig configuration
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public synchronized void start() {
        if (this.dataSource != null) {
            throw new IllegalStateException("Database service is already started.");
        }

        this.executorService = Executors.newFixedThreadPool(
            Math.max(1, this.settings.workerThreads()),
            new DatabaseThreadFactory()
        );
        this.dataSource = new HikariDataSource(this.configuration);
        this.logger.info("Database service started using " + this.settings.type() + ".");
    }

    @Override
    public synchronized void stop() {
        if (this.executorService != null) {
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    this.executorService.shutdownNow();
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                this.executorService.shutdownNow();
            }
            this.executorService = null;
        }

        if (this.dataSource != null) {
            this.dataSource.close();
            this.dataSource = null;
        }
        this.logger.info("Database service stopped.");
    }

    @Override
    public <T> CompletableFuture<T> execute(final DatabaseWork<T> work) {
        return CompletableFuture.supplyAsync(() -> this.run(work, false), this.requireExecutor());
    }

    @Override
    public <T> CompletableFuture<T> inTransaction(final DatabaseWork<T> work) {
        return CompletableFuture.supplyAsync(() -> this.run(work, true), this.requireExecutor());
    }

    @Override
    public DatabaseType type() {
        return this.settings.type();
    }

    private <T> T run(final DatabaseWork<T> work, final boolean transactional) {
        try (Connection connection = this.requireDataSource().getConnection()) {
            connection.setAutoCommit(!transactional);
            final JdbcDatabaseTransaction transaction = new JdbcDatabaseTransaction(connection);
            try {
                final T result = work.execute(transaction);
                if (transactional) {
                    connection.commit();
                }
                return result;
            } catch (final Exception exception) {
                if (transactional) {
                    connection.rollback();
                }
                throw new IllegalStateException("Database work failed.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (final SQLException exception) {
            throw new IllegalStateException("Database connection failure.", exception);
        }
    }

    private ExecutorService requireExecutor() {
        final ExecutorService executor = this.executorService;
        if (executor == null) {
            throw new IllegalStateException("Database service has not been started.");
        }
        return executor;
    }

    private HikariDataSource requireDataSource() {
        final HikariDataSource source = this.dataSource;
        if (source == null) {
            throw new IllegalStateException("Database service has not been started.");
        }
        return source;
    }

    private static final class JdbcDatabaseTransaction implements DatabaseTransaction {

        private final Connection connection;

        private JdbcDatabaseTransaction(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public int update(final String sql, final StatementBinder binder) throws SQLException {
            try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
                binder.bind(statement);
                return statement.executeUpdate();
            }
        }

        @Override
        public <T> Optional<T> queryOne(
            final String sql,
            final StatementBinder binder,
            final ResultSetMapper<T> mapper
        ) throws SQLException {
            try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapper.map(resultSet));
                }
            }
        }

        @Override
        public <T> List<T> queryList(
            final String sql,
            final StatementBinder binder,
            final ResultSetMapper<T> mapper
        ) throws SQLException {
            try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<T> values = new ArrayList<>();
                    while (resultSet.next()) {
                        values.add(mapper.map(resultSet));
                    }
                    return List.copyOf(values);
                }
            }
        }
    }

    private static final class DatabaseThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable, "Ascension-Database-" + this.counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
