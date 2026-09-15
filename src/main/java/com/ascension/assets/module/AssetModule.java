package com.ascension.assets.module;

import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.definition.BossDefinition;
import com.ascension.assets.definition.FloorDefinition;
import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.definition.LootTableDefinition;
import com.ascension.assets.definition.MobDefinition;
import com.ascension.assets.definition.NpcDefinition;
import com.ascension.assets.definition.ProfessionDefinition;
import com.ascension.assets.definition.QuestDefinition;
import com.ascension.assets.definition.SkillDefinition;
import com.ascension.assets.loader.AssetFrameworkSettings;
import com.ascension.assets.loader.AssetReloadResult;
import com.ascension.assets.loader.AssetService;
import com.ascension.assets.loader.AssetType;
import com.ascension.assets.loader.DefaultAssetService;
import com.ascension.assets.loader.GenericAssetSerializer;
import com.ascension.assets.loader.TranslationBundleSerializer;
import com.ascension.assets.localization.DefaultLocalizationService;
import com.ascension.assets.localization.LocalizationService;
import com.ascension.assets.localization.LocalizationSettings;
import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.abilities.loader.AbilityDefinitionSerializer;
import com.ascension.core.config.typed.TypedConfigurationService;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryDescriptor;
import com.ascension.registry.RegistryHub;
import com.ascension.serialization.SerializedObjectCodecRegistry;
import com.ascension.validation.ValidationCollector;
import com.ascension.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Boots the asset, localization, typed configuration, and hot reload framework.
 */
public final class AssetModule extends AbstractModule {

    @Override
    public String id() {
        return "assets";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("core-infrastructure", "registry");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final TypedConfigurationService typedConfigurationService = services.require(TypedConfigurationService.class);
        typedConfigurationService.registerAndLoad(AssetConfigurationDescriptors.ASSET_FRAMEWORK);
        typedConfigurationService.registerAndLoad(AssetConfigurationDescriptors.LOCALIZATION);

        final Supplier<AssetFrameworkSettings> assetSettingsSupplier =
            () -> typedConfigurationService.require(AssetConfigurationDescriptors.ASSET_FRAMEWORK).value();
        final Supplier<LocalizationSettings> localizationSettingsSupplier =
            () -> typedConfigurationService.require(AssetConfigurationDescriptors.LOCALIZATION).value();

        final AssetService assetService = new DefaultAssetService(
            services.require(PluginPlatform.class),
            services.require(PluginLogger.class),
            services.require(RegistryHub.class),
            services.require(SerializedObjectCodecRegistry.class),
            typedConfigurationService,
            assetSettingsSupplier
        );

        services.register(AssetService.class, assetService);
        services.register(com.ascension.assets.reload.AssetReloadService.class, assetService);

        final AssetType<TranslationBundleDefinition> localizationType = registerTypes(assetService);
        createAssetDirectories(assetSettingsSupplier.get(), services.require(PluginPlatform.class));
        saveBundledLocalizationResource(services.require(JavaPlugin.class), assetSettingsSupplier.get());

        final AssetReloadResult result = assetService.reloadAllAssets();
        services.register(LocalizationService.class, new DefaultLocalizationService(
            localizationSettingsSupplier,
            assetService.registry(localizationType)
        ));

        services.require(PluginLogger.class).info(
            "Asset module started with " + result.assetCount() + " loaded assets across "
                + assetService.assetTypes().size() + " asset groups."
        );
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(PluginLogger.class).info("Asset module stopped.");
    }

    private static AssetType<TranslationBundleDefinition> registerTypes(final AssetService assetService) {
        final AssetType<TranslationBundleDefinition> localization = new AssetType<>(
            AssetConfigurationDescriptors.OWNER,
            "localization",
            "localization",
            AscensionRegistries.LOCALIZATION_BUNDLES,
            new TranslationBundleSerializer(),
            translationValidator()
        );

        assetService.registerType(localization);
        assetService.registerType(genericType("skills", "skills", AscensionRegistries.SKILL_DEFINITIONS, SkillDefinition::new));
        assetService.registerType(genericType("bosses", "bosses", AscensionRegistries.BOSS_DEFINITIONS, BossDefinition::new));
        assetService.registerType(genericType("mobs", "mobs", AscensionRegistries.MOB_DEFINITIONS, MobDefinition::new));
        assetService.registerType(genericType("floors", "floors", AscensionRegistries.FLOOR_DEFINITIONS, FloorDefinition::new));
        assetService.registerType(genericType("quests", "quests", AscensionRegistries.QUEST_DEFINITIONS, QuestDefinition::new));
        assetService.registerType(genericType("professions", "professions", AscensionRegistries.PROFESSION_DEFINITIONS, ProfessionDefinition::new));
        assetService.registerType(genericType("loot_tables", "loot_tables", AscensionRegistries.LOOT_TABLE_DEFINITIONS, LootTableDefinition::new));
        assetService.registerType(genericType("npc", "npc", AscensionRegistries.NPC_DEFINITIONS, NpcDefinition::new));
        assetService.registerType(new AssetType<>(
            AssetConfigurationDescriptors.OWNER,
            "abilities",
            "abilities",
            AscensionRegistries.ABILITY_DEFINITIONS,
            new AbilityDefinitionSerializer(),
            Validator.noop()
        ));
        assetService.registerType(new AssetType<>(
            AssetConfigurationDescriptors.OWNER,
            "effects",
            "effects",
            AscensionRegistries.EFFECT_DEFINITIONS,
            new com.ascension.effects.definition.EffectDefinitionSerializer(),
            Validator.noop()
        ));
        return localization;
    }

    private static <T extends AssetDefinition> AssetType<T> genericType(
        final String id,
        final String directory,
        final RegistryDescriptor<AssetId, T> registryDescriptor,
        final BiFunction<com.ascension.assets.model.AssetDescriptor, com.ascension.serialization.SerializedObject, T> factory
    ) {
        return new AssetType<>(
            AssetConfigurationDescriptors.OWNER,
            id,
            directory,
            registryDescriptor,
            new GenericAssetSerializer<>(id, factory),
            Validator.noop()
        );
    }

    private static Validator<TranslationBundleDefinition> translationValidator() {
        return definition -> {
            final ValidationCollector collector = new ValidationCollector();
            if (definition.localeTag().isBlank()) {
                collector.error("locale_blank", definition.id().toString(), "Translation locale tag must not be blank.");
            }
            if (definition.messages().isEmpty()) {
                collector.error("messages_empty", definition.id().toString(), "Translation bundle must contain at least one message.");
            }
            return collector.report();
        };
    }

    private static void createAssetDirectories(final AssetFrameworkSettings settings, final PluginPlatform platform) {
        final Path root = platform.dataFolder().toPath().resolve(settings.rootDirectory());
        createDirectory(root);
        for (final String directory : settings.ownedDirectories().values()) {
            createDirectory(root.resolve(directory));
        }
    }

    private static void createDirectory(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create asset directory: " + path, exception);
        }
    }

    private static void saveBundledLocalizationResource(final JavaPlugin plugin, final AssetFrameworkSettings settings) {
        final String relativeDirectory = settings.ownedDirectories().getOrDefault("localization", "localization");
        final Path target = plugin.getDataFolder().toPath().resolve(settings.rootDirectory()).resolve(relativeDirectory).resolve("en_us.yml");
        if (Files.exists(target)) {
            return;
        }
        createDirectory(target.getParent());
        try (InputStream stream = plugin.getResource("assets/localization/en_us.yml")) {
            if (stream == null) {
                throw new IllegalStateException("Bundled localization resource is missing: assets/localization/en_us.yml");
            }
            Files.copy(stream, target);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to copy bundled localization asset to " + target, exception);
        }
    }
}
