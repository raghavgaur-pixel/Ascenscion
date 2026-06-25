package com.ascension.stats.module;

import com.ascension.assets.loader.AssetReloadResult;
import com.ascension.assets.loader.AssetService;
import com.ascension.assets.loader.AssetType;
import com.ascension.assets.model.AssetId;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.MutableRegistry;
import com.ascension.registry.RegistryHub;
import com.ascension.stats.calculation.LinearDerivedStatCalculatorFactory;
import com.ascension.stats.definition.StatDefinition;
import com.ascension.stats.definition.StatDefinitionSerializer;
import com.ascension.stats.service.AttributeService;
import com.ascension.stats.service.DefaultAttributeCalculationService;
import com.ascension.stats.service.DefaultAttributeService;
import com.ascension.stats.service.DefaultStatService;
import com.ascension.stats.service.StatService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Boots the stat and attribute engine.
 */
public final class StatsModule extends AbstractModule {

    private static final AssetType<StatDefinition> STAT_ASSET_TYPE = new AssetType<>(
        "stats",
        "stats",
        "stats",
        AscensionRegistries.STAT_DEFINITIONS,
        new StatDefinitionSerializer(),
        StatValidators.STAT_DEFINITION
    );

    private static final List<String> SEEDED_STAT_ASSETS = List.of(
        "health.yml",
        "mana.yml",
        "stamina.yml",
        "strength.yml",
        "dexterity.yml",
        "vitality.yml",
        "intelligence.yml",
        "defense.yml",
        "magic_defense.yml",
        "attack_speed.yml",
        "movement_speed.yml",
        "critical_chance.yml",
        "critical_damage.yml",
        "armor_penetration.yml",
        "magic_penetration.yml",
        "healing_power.yml",
        "cooldown_reduction.yml",
        "luck.yml",
        "experience_gain.yml",
        "gathering_speed.yml",
        "maximum_health.yml",
        "maximum_mana.yml",
        "health_regeneration.yml",
        "mana_regeneration.yml",
        "attack_power.yml",
        "magic_power.yml",
        "defense_rating.yml",
        "effective_health.yml",
        "movement_speed_rating.yml",
        "critical_damage_rating.yml"
    );

    @Override
    public String id() {
        return "stats";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("assets", "registry");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final AssetService assetService = services.require(AssetService.class);
        assetService.registerType(STAT_ASSET_TYPE);

        registerCalculatorFactories(services.require(RegistryHub.class));
        seedDefaultStatAssets(
            services.require(JavaPlugin.class),
            services.require(PluginPlatform.class)
        );

        final AssetReloadResult result = assetService.reloadAssetGroup("stats");
        final DefaultStatService statService = new DefaultStatService(
            services.require(RegistryHub.class).require(AscensionRegistries.STAT_DEFINITIONS),
            services.require(RegistryHub.class).require(AscensionRegistries.DERIVED_STAT_CALCULATOR_FACTORIES)
        );
        final DefaultAttributeCalculationService calculationService = new DefaultAttributeCalculationService(statService);
        final AttributeService attributeService = new DefaultAttributeService(statService, calculationService);

        services.register(StatService.class, statService);
        services.register(AttributeService.class, attributeService);
        services.require(PluginLogger.class).info(
            "Stats module started with " + result.assetCount() + " loaded stat assets."
        );
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(PluginLogger.class).info("Stats module stopped.");
    }

    private static void registerCalculatorFactories(final RegistryHub registryHub) {
        final MutableRegistry<String, com.ascension.stats.calculation.DerivedStatCalculatorFactory> registry =
            registryHub.getOrCreate(AscensionRegistries.DERIVED_STAT_CALCULATOR_FACTORIES);
        if (!registry.contains("linear")) {
            registry.register("linear", new LinearDerivedStatCalculatorFactory());
        }
    }

    private static void seedDefaultStatAssets(
        final JavaPlugin plugin,
        final PluginPlatform platform
    ) {
        final Path root = platform.dataFolder().toPath().resolve("assets").resolve("stats");
        createDirectory(root);
        for (final String resource : SEEDED_STAT_ASSETS) {
            final Path target = root.resolve(resource);
            if (Files.exists(target)) {
                continue;
            }
            copyBundledResource(plugin, "assets/stats/" + resource, target);
        }
    }

    private static void copyBundledResource(
        final JavaPlugin plugin,
        final String resourcePath,
        final Path target
    ) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Bundled stat asset is missing: " + resourcePath);
            }
            createDirectory(target.getParent());
            Files.copy(stream, target);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to copy bundled stat asset " + resourcePath + " to " + target, exception);
        }
    }

    private static void createDirectory(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create stat asset directory: " + path, exception);
        }
    }
}
