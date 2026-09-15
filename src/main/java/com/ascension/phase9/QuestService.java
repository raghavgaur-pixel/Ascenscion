package com.ascension.phase9;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.definition.QuestDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.runtime.AscensionItem;
import com.ascension.items.service.ItemService;
import com.ascension.profiles.component.CurrencyProfileComponent;
import com.ascension.profiles.component.ProgressionProfileComponent;
import com.ascension.profiles.component.QuestProgressProfileComponent;
import com.ascension.profiles.component.UnlockedFloorsProfileComponent;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.progression.service.ExperienceCurve;
import com.ascension.progression.service.ProgressionService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.tower.model.FloorId;
import com.ascension.tower.service.TowerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

/** Runtime quest orchestration over persistent profile state and authored quest assets. */
public final class QuestService {

    private final RegistryHub registries;
    private final PlayerProfileService profiles;
    private final ProgressionService progression;
    private final TowerService tower;
    private final ItemService items;
    private final ItemMetadataEncoder itemEncoder;

    public QuestService(
        final RegistryHub registries,
        final PlayerProfileService profiles,
        final ProgressionService progression,
        final TowerService tower,
        final ItemService items,
        final ItemMetadataEncoder itemEncoder
    ) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.tower = Objects.requireNonNull(tower, "tower");
        this.items = Objects.requireNonNull(items, "items");
        this.itemEncoder = Objects.requireNonNull(itemEncoder, "itemEncoder");
    }

    public Result accept(final UUID playerId, final String questIdValue) {
        final PlayerProfile profile = onlineProfile(playerId).orElse(null);
        if (profile == null) return Result.rejected("Player profile is not loaded");
        final AssetId questId = parseId(questIdValue);
        if (questId == null) return Result.rejected("Invalid quest id: " + questIdValue);
        final QuestDefinition quest = this.registries.require(AscensionRegistries.QUEST_DEFINITIONS, questId);
        final QuestProgressProfileComponent state = state(profile);
        if (state.isCompleted(questId.toString())) return Result.rejected("Quest is already completed");
        if (state.isActive(questId.toString())) return Result.rejected("Quest is already active");
        for (final String prerequisite : quest.data().getStringSet("prerequisites")) {
            if (!state.isCompleted(prerequisite)) return Result.rejected("Missing prerequisite: " + prerequisite);
        }
        state.accept(questId.toString());
        save(playerId);
        return Result.success();
    }

    public List<String> active(final UUID playerId) {
        return onlineProfile(playerId).map(profile -> List.copyOf(state(profile).activeSnapshot())).orElse(List.of());
    }

    public Result talkToNpc(final UUID playerId, final String npcId) {
        return progress(playerId, ObjectiveMatcher.type("talk_to_npc").value(npcId));
    }

    public Result reachLocation(final UUID playerId, final String location) {
        return progress(playerId, ObjectiveMatcher.type("reach_location").value(location));
    }

    public Result defeat(final UUID playerId, final String entityId, final String bossId) {
        final Result mob = progress(playerId, ObjectiveMatcher.type("defeat").value(entityId));
        final Result boss = bossId == null ? Result.noop() : progress(playerId, ObjectiveMatcher.type("defeat_boss").value(bossId));
        return combine(mob, boss);
    }

    private Result progress(final UUID playerId, final ObjectiveMatcher matcher) {
        final PlayerProfile profile = onlineProfile(playerId).orElse(null);
        if (profile == null) return Result.rejected("Player profile is not loaded");
        final QuestProgressProfileComponent state = state(profile);
        boolean changed = false;
        for (final String questIdValue : new ArrayList<>(state.activeSnapshot())) {
            final AssetId questId = parseId(questIdValue);
            if (questId == null) continue;
            final QuestDefinition quest = this.registries.getOrCreate(AscensionRegistries.QUEST_DEFINITIONS).find(questId).orElse(null);
            if (quest == null) continue;
            final List<com.ascension.serialization.SerializedObject> objectives = quest.data().getObjectList("objectives");
            for (int index = 0; index < objectives.size(); index++) {
                final var objective = objectives.get(index);
                if (!matcher.matches(objective)) continue;
                final String key = progressKey(questIdValue, index);
                final int current = state.objectiveProgress(key);
                final int required = Math.max(1, (int) objective.getLong("amount", 1L));
                if (current < required) {
                    state.addProgress(key, 1);
                    changed = true;
                }
            }
            if (allObjectivesComplete(state, quest)) {
                complete(profile, quest);
                changed = true;
            }
        }
        if (changed) save(playerId);
        return changed ? Result.success() : Result.noop();
    }

    private void complete(final PlayerProfile profile, final QuestDefinition quest) {
        final QuestProgressProfileComponent state = state(profile);
        final String questId = quest.id().toString();
        state.complete(questId);
        final ProgressionProfileComponent progressionState = component(profile, "progression", ProgressionProfileComponent.class);
        final long experience = quest.data().getObject("rewards").map(data -> data.getLong("experience", 0L)).orElse(0L);
        if (experience > 0L) this.progression.grantExperience(progressionState, experience);
        final CurrencyProfileComponent currency = component(profile, "currency", CurrencyProfileComponent.class);
        quest.data().getObject("rewards").ifPresent(rewards -> {
            for (final var entry : rewards.getLongMap("currency").entrySet()) currency.add(entry.getKey(), entry.getValue());
            for (final String itemIdValue : rewards.getStringSet("items")) giveItem(profile.uniqueId(), itemIdValue);
            final String floor = rewards.getString("unlock_floor", "");
            if (!floor.isBlank()) {
                final UnlockedFloorsProfileComponent floors = component(profile, "unlocked_floors", UnlockedFloorsProfileComponent.class);
                final AssetId id = parseId(floor);
                if (id != null) this.tower.unlock(floors, new FloorId(id.toString()));
            }
        });
        final String followUp = quest.data().getString("follow_up", "");
        if (!followUp.isBlank()) {
            final AssetId id = parseId(followUp);
            if (id != null) state.accept(id.toString());
        }
        final Player onlinePlayer = Bukkit.getPlayer(profile.uniqueId());
        if (onlinePlayer != null) onlinePlayer.sendMessage(Component.text("Quest complete: " + quest.displayName()));
    }

    private boolean allObjectivesComplete(final QuestProgressProfileComponent state, final QuestDefinition quest) {
        final List<com.ascension.serialization.SerializedObject> objectives = quest.data().getObjectList("objectives");
        for (int index = 0; index < objectives.size(); index++) {
            final var objective = objectives.get(index);
            final int required = Math.max(1, (int) objective.getLong("amount", 1L));
            if (state.objectiveProgress(progressKey(quest.id().toString(), index)) < required) return false;
        }
        return true;
    }

    private void giveItem(final UUID playerId, final String itemIdValue) {
        final AssetId itemId = parseId(itemIdValue);
        final Player player = Bukkit.getPlayer(playerId);
        if (itemId == null || player == null) return;
        final ItemDefinition definition = this.items.findDefinition(itemId).orElse(null);
        if (definition == null) return;
        final String materialName = definition.data().getString("material", "WOODEN_SWORD");
        final Material material;
        try { material = Material.valueOf(materialName.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return; }
        final AscensionItem item = this.items.create(itemId);
        final ItemStack stack = new ItemStack(material, 1);
        final var meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(definition.descriptor().displayName()));
            stack.setItemMeta(meta);
        }
        this.itemEncoder.encode(item, stack);
        player.getInventory().addItem(stack);
    }

    private PlayerProfile onlineProfile(final UUID playerId) { return this.profiles.online(playerId).orElse(null); }
    private void save(final UUID playerId) { this.profiles.save(playerId); }

    private static <T> T component(final PlayerProfile profile, final String id, final Class<T> type) {
        return profile.components().find(id).map(type::cast).orElseThrow(() -> new IllegalStateException("Missing profile component: " + id));
    }
    private static QuestProgressProfileComponent state(final PlayerProfile profile) { return component(profile, "quests", QuestProgressProfileComponent.class); }
    private static String progressKey(final String questId, final int index) { return questId + ":" + index; }
    private static AssetId parseId(final String value) { try { return AssetId.parse(value); } catch (RuntimeException exception) { return null; } }
    private static Result combine(final Result first, final Result second) { return first.status() == Status.SUCCESS || second.status() == Status.SUCCESS ? Result.success() : (first.status() == Status.NOOP && second.status() == Status.NOOP ? Result.noop() : first); }

    public enum Status { SUCCESS, NOOP, REJECTED }
    public record Result(Status status, String reason) {
        public static Result success() { return new Result(Status.SUCCESS, ""); }
        public static Result noop() { return new Result(Status.NOOP, ""); }
        public static Result rejected(final String reason) { return new Result(Status.REJECTED, reason); }
    }

    private record ObjectiveMatcher(String type, String valueKey, String value) {
        static ObjectiveMatcher type(final String type) { return new ObjectiveMatcher(type, "", ""); }
        ObjectiveMatcher value(final String value) { return new ObjectiveMatcher(this.type, this.type.equals("defeat_boss") ? "boss" : this.type.equals("defeat") ? "entity" : this.type.equals("talk_to_npc") ? "npc" : "location", value); }
        boolean matches(final com.ascension.serialization.SerializedObject objective) { return this.type.equalsIgnoreCase(objective.getString("type", "")) && (this.value.isBlank() || this.value.equals(objective.getString(this.valueKey, ""))); }
    }
}
