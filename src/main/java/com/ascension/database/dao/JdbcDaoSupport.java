package com.ascension.database.dao;

import com.ascension.database.service.DatabaseService;
import com.ascension.database.service.DatabaseTransaction;
import com.ascension.database.service.DatabaseWork;
import com.ascension.database.service.ResultSetMapper;
import com.ascension.database.service.StatementBinder;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for JDBC-backed DAO implementations.
 */
public abstract class JdbcDaoSupport {

    private final DatabaseService databaseService;

    protected JdbcDaoSupport(final DatabaseService databaseService) {
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
    }

    protected final <T> CompletableFuture<T> execute(final DatabaseWork<T> work) {
        return this.databaseService.execute(work);
    }

    protected final <T> CompletableFuture<T> inTransaction(final DatabaseWork<T> work) {
        return this.databaseService.inTransaction(work);
    }

    protected final int update(final DatabaseTransaction transaction, final String sql, final StatementBinder binder)
        throws SQLException {
        return transaction.update(sql, binder);
    }

    protected final <T> Optional<T> queryOne(
        final DatabaseTransaction transaction,
        final String sql,
        final StatementBinder binder,
        final ResultSetMapper<T> mapper
    ) throws SQLException {
        return transaction.queryOne(sql, binder, mapper);
    }

    protected final <T> List<T> queryList(
        final DatabaseTransaction transaction,
        final String sql,
        final StatementBinder binder,
        final ResultSetMapper<T> mapper
    ) throws SQLException {
        return transaction.queryList(sql, binder, mapper);
    }
}

