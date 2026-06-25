package com.ascension.core.config;

import org.bukkit.configuration.file.YamlConfiguration;

public record ConfigurationFile(String path, YamlConfiguration configuration) {
}

