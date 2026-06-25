package com.ascension.core.logging;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JulPluginLogger implements PluginLogger {

    private final Logger logger;

    public JulPluginLogger(final Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void info(final String message) {
        this.logger.info(message);
    }

    @Override
    public void warn(final String message) {
        this.logger.warning(message);
    }

    @Override
    public void error(final String message) {
        this.logger.severe(message);
    }

    @Override
    public void error(final String message, final Throwable throwable) {
        this.logger.log(Level.SEVERE, message, throwable);
    }
}
