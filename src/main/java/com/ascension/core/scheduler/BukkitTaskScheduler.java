package com.ascension.core.scheduler;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitTaskScheduler implements TaskScheduler {

    private final JavaPlugin plugin;

    public BukkitTaskScheduler(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void runSync(final Runnable task) {
        Bukkit.getScheduler().runTask(this.plugin, task);
    }

    @Override
    public void runAsync(final Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, task);
    }
}

