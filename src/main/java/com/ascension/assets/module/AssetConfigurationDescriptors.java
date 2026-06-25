package com.ascension.assets.module;

import com.ascension.assets.loader.AssetFrameworkSettings;
import com.ascension.assets.loader.AssetFrameworkSettingsCodec;
import com.ascension.assets.localization.LocalizationSettings;
import com.ascension.assets.localization.LocalizationSettingsCodec;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.core.config.typed.ConfigurationUpgrader;
import com.ascension.core.config.typed.TypedConfigurationDescriptor;
import com.ascension.serialization.SerializedFormat;
import com.ascension.validation.ValidationCollector;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed configuration descriptors for the asset and localization framework.
 */
public final class AssetConfigurationDescriptors {

    public static final String OWNER = "assets";

    public static final TypedConfigurationDescriptor<AssetFrameworkSettings> ASSET_FRAMEWORK =
        new TypedConfigurationDescriptor<>(
            OWNER,
            "asset-framework",
            "config/assets.yml",
            1,
            SerializedFormat.YAML,
            new AssetFrameworkSettingsCodec(),
            settings -> {
                final ValidationCollector collector = new ValidationCollector();
                if (settings.rootDirectory().isBlank()) {
                    collector.error("root_directory_blank", "assets.root_directory", "Asset root directory must not be blank.");
                }
                if (settings.defaultNamespace().isBlank()) {
                    collector.error("default_namespace_blank", "assets.default_namespace", "Default namespace must not be blank.");
                }
                if (settings.ownedDirectories().isEmpty()) {
                    collector.error("owned_directories_empty", "assets.owned_directories", "At least one asset directory must be configured.");
                }
                for (final Map.Entry<String, String> entry : settings.ownedDirectories().entrySet()) {
                    if (entry.getKey().isBlank()) {
                        collector.error("owned_directory_key_blank", "assets.owned_directories", "Asset group id must not be blank.");
                    }
                    if (entry.getValue().isBlank()) {
                        collector.error(
                            "owned_directory_value_blank",
                            "assets.owned_directories." + entry.getKey(),
                            "Asset directory must not be blank."
                        );
                    }
                }
                return collector.report();
            },
            () -> new AssetFrameworkSettings(
                "assets",
                "ascension",
                new SemanticVersion(1, 0, 0),
                true,
                defaultOwnedDirectories()
            ),
            ConfigurationUpgrader.noop(),
            List.of(
                "Ascension asset framework settings.",
                "Every future content definition should be loaded through this pipeline."
            )
        );

    public static final TypedConfigurationDescriptor<LocalizationSettings> LOCALIZATION =
        new TypedConfigurationDescriptor<>(
            OWNER,
            "localization",
            "config/localization.yml",
            1,
            SerializedFormat.YAML,
            new LocalizationSettingsCodec(),
            settings -> {
                final ValidationCollector collector = new ValidationCollector();
                if (settings.defaultLanguage().isBlank()) {
                    collector.error("default_language_blank", "localization.default_language", "Default language must not be blank.");
                }
                if (settings.fallbackLanguage().isBlank()) {
                    collector.error("fallback_language_blank", "localization.fallback_language", "Fallback language must not be blank.");
                }
                if (settings.bundleNamespace().isBlank()) {
                    collector.error("bundle_namespace_blank", "localization.bundle_namespace", "Bundle namespace must not be blank.");
                }
                return collector.report();
            },
            () -> new LocalizationSettings("en_us", "en_us", "ascension"),
            ConfigurationUpgrader.noop(),
            List.of(
                "Ascension localization framework settings.",
                "Translation bundles are loaded as assets and resolved through the configured namespace."
            )
        );

    private AssetConfigurationDescriptors() {
    }

    private static Map<String, String> defaultOwnedDirectories() {
        final Map<String, String> directories = new LinkedHashMap<>();
        directories.put("localization", "localization");
        directories.put("stats", "stats");
        directories.put("items", "items");
        directories.put("skills", "skills");
        directories.put("bosses", "bosses");
        directories.put("floors", "floors");
        directories.put("quests", "quests");
        directories.put("loot_tables", "loot_tables");
        directories.put("recipes", "recipes");
        directories.put("professions", "professions");
        directories.put("dialogue", "dialogue");
        directories.put("npc", "npc");
        directories.put("events", "events");
        directories.put("factions", "factions");
        directories.put("titles", "titles");
        return directories;
    }
}
