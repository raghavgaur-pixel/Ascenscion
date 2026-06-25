package com.ascension.core.config;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlConfigurationService implements ConfigurationService {

    private final JavaPlugin plugin;
    private final Map<String, ConfigurationFile> files = new ConcurrentHashMap<>();

    public YamlConfigurationService(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public ConfigurationFile load(final String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");

        final File targetFile = new File(this.plugin.getDataFolder(), relativePath);
        if (!targetFile.exists()) {
            final File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Failed to create configuration directory: " + parent.getAbsolutePath());
            }
            this.plugin.saveResource(relativePath, false);
        }

        final ConfigurationFile configurationFile =
            new ConfigurationFile(relativePath, YamlConfiguration.loadConfiguration(targetFile));
        this.files.put(relativePath, configurationFile);
        return configurationFile;
    }

    @Override
    public ConfigurationFile require(final String relativePath) {
        final ConfigurationFile file = this.files.get(relativePath);
        if (file == null) {
            throw new IllegalStateException("Configuration has not been loaded: " + relativePath);
        }
        return file;
    }
}

