package com.ascension.registry;

import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.definition.BossDefinition;
import com.ascension.assets.definition.FloorDefinition;
import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.definition.LootTableDefinition;
import com.ascension.assets.definition.NpcDefinition;
import com.ascension.assets.definition.ProfessionDefinition;
import com.ascension.assets.definition.QuestDefinition;
import com.ascension.assets.definition.SkillDefinition;
import com.ascension.assets.loader.AssetType;
import com.ascension.assets.localization.TranslationBundleDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.database.migration.SchemaMigration;
import com.ascension.effects.definition.EffectDefinition;
import com.ascension.profiles.component.ProfileComponentDefinition;
import com.ascension.stats.calculation.DerivedStatCalculatorFactory;
import com.ascension.stats.definition.StatDefinition;

/**
 * Canonical registry descriptors used by the foundation modules.
 */
public final class AscensionRegistries {

    public static final RegistryDescriptor<String, SchemaMigration> SCHEMA_MIGRATIONS =
        new RegistryDescriptor<>("schema_migrations", String.class, SchemaMigration.class);

    public static final RegistryDescriptor<String, ProfileComponentDefinition<?>> PROFILE_COMPONENTS =
        new RegistryDescriptor<>("profile_components", String.class, castProfileComponentDefinitionType());

    public static final RegistryDescriptor<String, AssetType<?>> ASSET_TYPES =
        new RegistryDescriptor<>("asset_types", String.class, castAssetType());

    public static final RegistryDescriptor<AssetId, TranslationBundleDefinition> LOCALIZATION_BUNDLES =
        new RegistryDescriptor<>("localization_bundles", AssetId.class, TranslationBundleDefinition.class);

    public static final RegistryDescriptor<AssetId, ItemDefinition> ITEM_DEFINITIONS =
        new RegistryDescriptor<>("item_definitions", AssetId.class, ItemDefinition.class);

    public static final RegistryDescriptor<AssetId, AbilityDefinition> ABILITY_DEFINITIONS =
        new RegistryDescriptor<>("ability_definitions", AssetId.class, AbilityDefinition.class);

    public static final RegistryDescriptor<AssetId, SkillDefinition> SKILL_DEFINITIONS =
        new RegistryDescriptor<>("skill_definitions", AssetId.class, SkillDefinition.class);

    public static final RegistryDescriptor<AssetId, BossDefinition> BOSS_DEFINITIONS =
        new RegistryDescriptor<>("boss_definitions", AssetId.class, BossDefinition.class);

    public static final RegistryDescriptor<AssetId, FloorDefinition> FLOOR_DEFINITIONS =
        new RegistryDescriptor<>("floor_definitions", AssetId.class, FloorDefinition.class);

    public static final RegistryDescriptor<AssetId, QuestDefinition> QUEST_DEFINITIONS =
        new RegistryDescriptor<>("quest_definitions", AssetId.class, QuestDefinition.class);

    public static final RegistryDescriptor<AssetId, ProfessionDefinition> PROFESSION_DEFINITIONS =
        new RegistryDescriptor<>("profession_definitions", AssetId.class, ProfessionDefinition.class);

    public static final RegistryDescriptor<AssetId, LootTableDefinition> LOOT_TABLE_DEFINITIONS =
        new RegistryDescriptor<>("loot_table_definitions", AssetId.class, LootTableDefinition.class);

    public static final RegistryDescriptor<AssetId, NpcDefinition> NPC_DEFINITIONS =
        new RegistryDescriptor<>("npc_definitions", AssetId.class, NpcDefinition.class);

    public static final RegistryDescriptor<AssetId, EffectDefinition> EFFECT_DEFINITIONS =
        new RegistryDescriptor<>("effect_definitions", AssetId.class, EffectDefinition.class);

    public static final RegistryDescriptor<AssetId, StatDefinition> STAT_DEFINITIONS =
        new RegistryDescriptor<>("stat_definitions", AssetId.class, StatDefinition.class);

    public static final RegistryDescriptor<String, DerivedStatCalculatorFactory> DERIVED_STAT_CALCULATOR_FACTORIES =
        new RegistryDescriptor<>(
            "derived_stat_calculator_factories",
            String.class,
            DerivedStatCalculatorFactory.class
        );

    private AscensionRegistries() {
    }

    @SuppressWarnings("unchecked")
    private static Class<ProfileComponentDefinition<?>> castProfileComponentDefinitionType() {
        return (Class<ProfileComponentDefinition<?>>) (Class<?>) ProfileComponentDefinition.class;
    }

    @SuppressWarnings("unchecked")
    private static Class<AssetType<?>> castAssetType() {
        return (Class<AssetType<?>>) (Class<?>) AssetType.class;
    }
}
