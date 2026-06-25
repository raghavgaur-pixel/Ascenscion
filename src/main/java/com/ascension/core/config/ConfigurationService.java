package com.ascension.core.config;

public interface ConfigurationService {

    ConfigurationFile load(String relativePath);

    ConfigurationFile require(String relativePath);
}

