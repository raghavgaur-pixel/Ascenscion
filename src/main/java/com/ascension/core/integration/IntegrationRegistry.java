package com.ascension.core.integration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public final class IntegrationRegistry {

    private static final List<String> OPTIONAL_INTEGRATIONS = List.of(
        "PlaceholderAPI",
        "LuckPerms",
        "Vault",
        "WorldEdit",
        "WorldGuard",
        "ProtocolLib",
        "Citizens",
        "MythicMobs"
    );

    private final JavaPlugin plugin;
    private final Map<String, ExternalIntegration> integrations = new ConcurrentHashMap<>();

    public IntegrationRegistry(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void discover() {
        for (final String pluginName : OPTIONAL_INTEGRATIONS) {
            final boolean available = this.plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
            this.integrations.put(pluginName, new ExternalIntegration(pluginName, available));
        }
    }

    public ExternalIntegration require(final String pluginName) {
        final ExternalIntegration integration = this.integrations.get(pluginName);
        if (integration == null) {
            throw new IllegalStateException("Integration has not been discovered: " + pluginName);
        }
        return integration;
    }

    public Collection<ExternalIntegration> all() {
        return List.copyOf(this.integrations.values());
    }
}

