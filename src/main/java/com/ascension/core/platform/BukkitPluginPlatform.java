package com.ascension.core.platform;

import java.io.File;
import java.util.Objects;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitPluginPlatform implements PluginPlatform {

    private final JavaPlugin plugin;

    public BukkitPluginPlatform(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public File dataFolder() {
        return this.plugin.getDataFolder();
    }

    @Override
    public void ensureDataDirectories() {
        final File dataFolder = this.dataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create data folder at " + dataFolder.getAbsolutePath());
        }
    }

    @Override
    public boolean isPluginEnabled(final String pluginName) {
        final PluginManager pluginManager = this.plugin.getServer().getPluginManager();
        return pluginManager.isPluginEnabled(pluginName);
    }
}

