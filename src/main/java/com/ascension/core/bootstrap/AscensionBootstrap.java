package com.ascension.core.bootstrap;

import com.ascension.abilities.module.AbilityModule;
import com.ascension.assets.module.AssetModule;
import com.ascension.core.config.ConfigurationService;
import com.ascension.core.config.YamlConfigurationService;
import com.ascension.core.di.ServiceContainer;
import com.ascension.core.integration.IntegrationRegistry;
import com.ascension.core.lifecycle.AscensionApplication;
import com.ascension.core.logging.JulPluginLogger;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.CoreInfrastructureModule;
import com.ascension.core.module.ModuleManager;
import com.ascension.core.platform.BukkitPluginPlatform;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.core.scheduler.BukkitTaskScheduler;
import com.ascension.core.scheduler.TaskScheduler;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.database.module.DatabaseModule;
import com.ascension.effects.module.EffectsModule;
import com.ascension.profiles.module.ProfileModule;
import com.ascension.registry.RegistryModule;
import com.ascension.runtime.module.RuntimeEngineModule;
import com.ascension.session.module.SessionModule;
import com.ascension.stats.module.StatsModule;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class AscensionBootstrap {
    private final JavaPlugin plugin;
    private AscensionApplication application;
    public AscensionBootstrap(final JavaPlugin plugin) { this.plugin = plugin; }

    public void enable() {
        final PluginPlatform platform = new BukkitPluginPlatform(plugin);
        final PluginLogger logger = new JulPluginLogger(plugin.getLogger());
        final TaskScheduler scheduler = new BukkitTaskScheduler(plugin);
        final ConfigurationService configurationService = new YamlConfigurationService(plugin);
        final ServiceContainer container = new ServiceContainer();
        container.registerInstance(JavaPlugin.class, plugin);
        container.registerInstance(PluginPlatform.class, platform);
        container.registerInstance(PluginLogger.class, logger);
        container.registerInstance(TaskScheduler.class, scheduler);
        container.registerInstance(ConfigurationService.class, configurationService);
        container.registerInstance(IntegrationRegistry.class, new IntegrationRegistry(plugin));
        final ServiceRegistry serviceRegistry = new ServiceRegistry(container);
        final ModuleManager moduleManager = new ModuleManager(List.of(
            new CoreInfrastructureModule(), new RuntimeEngineModule(), new RegistryModule(), new AssetModule(),
            new StatsModule(), new DatabaseModule(), new ProfileModule(), new SessionModule(),
            new com.ascension.items.module.ItemModule(), new com.ascension.equipment.module.EquipmentModule(),
            new EffectsModule(), new com.ascension.combat.module.CombatModule(), new AbilityModule(),
            new com.ascension.phase9.PhaseNineModule()
        ));
        application = new AscensionApplication(logger, serviceRegistry, moduleManager);
        try { application.start(); }
        catch (final Exception exception) {
            logger.error("Failed to start Ascension cleanly.", exception);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public void disable() { if (application != null) application.stop(); }
}
