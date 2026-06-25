package com.ascension.database.model;

/**
 * Immutable runtime database settings.
 *
 * @param type selected database type
 * @param sqliteFile sqlite file location relative to the plugin data folder
 * @param host postgres host
 * @param port postgres port
 * @param database postgres database name
 * @param username postgres username
 * @param password postgres password
 * @param maximumPoolSize connection pool size
 * @param minimumIdle minimum idle connections
 * @param workerThreads async database worker threads
 */
public record DatabaseSettings(
    DatabaseType type,
    String sqliteFile,
    String host,
    int port,
    String database,
    String username,
    String password,
    int maximumPoolSize,
    int minimumIdle,
    int workerThreads
) {
}

