package com.ascension.phase9;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.definition.QuestDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.service.ItemService;
import com.ascension.profiles.component.CurrencyProfileComponent;
import com.ascension.profiles.component.ProgressionProfileComponent;
import com.ascension.profiles.component.QuestProgressProfileComponent;
import com.ascension.profiles.component.UnlockedFloorsProfileComponent;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.progression.service.ProgressionService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.tower.model.FloorId;
import com.ascension.tower.service.TowerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

/** Runtime quest orchestration over persistent profile state and authored quest assets. */
public final class QuestService {
    private static final String ARRIVAL_QUEST = "ascension:arrival";
    private static final String LYRA_NPC = "ascension:warden_lyra";
    private final RegistryHub registries;
    private final PlayerProfileService profiles;
    private final ProgressionService progression;
    private final TowerService tower;
    private final ItemService items;
    private final ItemMetadataEncoder itemEncoder;

    public QuestService(final RegistryHub registries, final PlayerProfileService profiles, final ProgressionService progression, final TowerService tower, final ItemService items, final ItemMetadataEncoder itemEncoder) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.tower = Objects.requireNonNull(tower, "tower");
        this.items = Objects.requireNonNull(items, "items");
        this.itemEncoder = Objects.requireNonNull(itemEncoder, "itemEncoder");
    }

    public Result accept(final UUID playerId, final String questIdValue) {
        final PlayerProfile profile = onlineProfile(playerId);
        if (profile == null) return Result.rejected("Player profile is not loaded");
        final AssetId questId = parseId(questIdValue);
        if (questId == null) return Result.rejected("Invalid quest id: " + questIdValue);
        final QuestDefinition quest = definition(questIdValue);
        if (quest == null) return Result.rejected("Unknown quest: " + questIdValue);
        final QuestProgressProfileComponent state = state(profile);
        if (state.isCompleted(questId.toString())) return Result.rejected("Quest is already completed");
        if (state.isActive(questId.toString())) return Result.rejected("Quest is already active");
        for (final String prerequisite : quest.data().getStringSet("prerequisites")) if (!state.isCompleted(prerequisite)) return Result.rejected("Missing prerequisite: " + prerequisite);
        state.accept(questId.toString()); save(playerId); return Result.success();
    }

    public List<String> active(final UUID playerId) { if (playerId == null) return List.of(); final PlayerProfile profile = onlineProfile(playerId); return profile == null ? List.of() : List.copyOf(state(profile).activeSnapshot()); }
    public boolean isCompleted(final UUID playerId, final String questId) { final PlayerProfile profile = onlineProfile(playerId); return profile != null && state(profile).isCompleted(questId); }
    public QuestDefinition definition(final String questIdValue) { final AssetId id = parseId(questIdValue); return id == null ? null : this.registries.getOrCreate(AscensionRegistries.QUEST_DEFINITIONS).find(id).orElse(null); }
    public int objectiveProgress(final UUID playerId, final String questId, final int index) { final PlayerProfile profile = onlineProfile(playerId); return profile == null ? 0 : state(profile).objectiveProgress(progressKey(questId, index)); }

    public Result talkToNpc(final UUID playerId, final String npcId) { if (LYRA_NPC.equals(npcId)) { accept(playerId, ARRIVAL_QUEST); grantStarterItems(playerId); } return progress(playerId, ObjectiveMatcher.type("talk_to_npc").value(npcId)); }
    public Result reachLocation(final UUID playerId, final String location) { return progress(playerId, ObjectiveMatcher.type("reach_location").value(location)); }
    public Result defeat(final UUID playerId, final String entityId, final String bossId) { final Result mob = entityId == null ? Result.noop() : progress(playerId, ObjectiveMatcher.type("defeat").value(entityId)); final Result boss = bossId == null ? Result.noop() : progress(playerId, ObjectiveMatcher.type("defeat_boss").value(bossId)); return combine(mob, boss); }

    public void grantKillRewards(final UUID playerId, final AssetId definitionId, final boolean boss) {
        final PlayerProfile profile = onlineProfile(playerId); if (profile == null) return;
        final com.ascension.assets.model.AssetDefinition definition = boss ? this.registries.getOrCreate(AscensionRegistries.BOSS_DEFINITIONS).find(definitionId).orElse(null) : this.registries.getOrCreate(AscensionRegistries.MOB_DEFINITIONS).find(definitionId).orElse(null);
        if (definition == null) return;
        final long experience = definition.data().getObject("rewards").map(r -> r.getLong("experience", 0L)).orElse(0L);
        if (experience > 0L) this.progression.grantExperience(component(profile, "progression", ProgressionProfileComponent.class), experience);
        definition.data().getObject("rewards").ifPresent(rewards -> { final CurrencyProfileComponent currency = component(profile, "currency", CurrencyProfileComponent.class); for (final var entry : rewards.getLongMap("currency").entrySet()) currency.add(entry.getKey(), entry.getValue()); final String floor = rewards.getString("unlock_floor", ""); if (!floor.isBlank()) { final AssetId floorId = parseId(floor); if (floorId != null) this.tower.unlock(component(profile, "unlocked_floors", UnlockedFloorsProfileComponent.class), new FloorId(floorId.toString())); } for (final String item : rewards.getStringSet("items")) giveItem(playerId, item); }); save(playerId);
    }

    public void grantStarterItems(final UUID playerId) { giveItem(playerId, "ascension:rookie_sword", 0); giveItem(playerId, "ascension:rookie_iron_ring", 8); final Player player = Bukkit.getPlayer(playerId); if (player != null) player.getInventory().setHeldItemSlot(0); }

    private Result progress(final UUID playerId, final ObjectiveMatcher matcher) {
        final PlayerProfile profile = onlineProfile(playerId); if (profile == null) return Result.rejected("Player profile is not loaded");
        final QuestProgressProfileComponent state = state(profile); boolean changed = false;
        for (final String questIdValue : new ArrayList<>(state.activeSnapshot())) {
            final QuestDefinition quest = definition(questIdValue); if (quest == null) continue;
            final List<com.ascension.serialization.SerializedObject> objectives = quest.data().getObjectList("objectives");
            for (int index = 0; index < objectives.size(); index++) { final var objective = objectives.get(index); if (!matcher.matches(objective)) continue; final String key = progressKey(questIdValue, index); final int current = state.objectiveProgress(key); final int required = Math.max(1, Math.toIntExact(objective.getLong("amount", 1L))); if (current < required) { state.addProgress(key, 1); changed = true; } }
            if (allObjectivesComplete(state, quest)) { complete(profile, quest); changed = true; }
        }
        if (changed) save(playerId); return changed ? Result.success() : Result.noop();
    }

    private void complete(final PlayerProfile profile, final QuestDefinition quest) {
        final QuestProgressProfileComponent state = state(profile); state.complete(quest.id().toString());
        quest.data().getObject("rewards").ifPresent(rewards -> { final long xp = rewards.getLong("experience", 0L); if (xp > 0L) this.progression.grantExperience(component(profile, "progression", ProgressionProfileComponent.class), xp); final CurrencyProfileComponent currency = component(profile, "currency", CurrencyProfileComponent.class); for (final var entry : rewards.getLongMap("currency").entrySet()) currency.add(entry.getKey(), entry.getValue()); for (final String item : rewards.getStringSet("items")) giveItem(profile.uniqueId(), item); final String floor = rewards.getString("unlock_floor", ""); if (!floor.isBlank()) { final AssetId floorId = parseId(floor); if (floorId != null) this.tower.unlock(component(profile, "unlocked_floors", UnlockedFloorsProfileComponent.class), new FloorId(floorId.toString())); } });
        final AssetId next = parseId(quest.data().getString("follow_up", "")); if (next != null && !state.isCompleted(next.toString())) state.accept(next.toString());
        final Player online = Bukkit.getPlayer(profile.uniqueId()); if (online != null) online.sendMessage(Component.text("Quest complete: " + quest.displayName()));
    }

    private boolean allObjectivesComplete(final QuestProgressProfileComponent state, final QuestDefinition quest) { final List<com.ascension.serialization.SerializedObject> objectives = quest.data().getObjectList("objectives"); for (int index = 0; index < objectives.size(); index++) { final int required = Math.max(1, Math.toIntExact(objectives.get(index).getLong("amount", 1L))); if (state.objectiveProgress(progressKey(quest.id().toString(), index)) < required) return false; } return true; }
    private void giveItem(final UUID playerId, final String itemIdValue) { giveItem(playerId, itemIdValue, -1); }
    private void giveItem(final UUID playerId, final String itemIdValue, final int preferredSlot) { final AssetId itemId = parseId(itemIdValue); final Player player = Bukkit.getPlayer(playerId); if (itemId == null || player == null) return; final ItemDefinition definition = this.items.findDefinition(itemId).orElse(null); if (definition == null) return; final Material material; try { material = Material.valueOf(definition.data().getString("material", "WOODEN_SWORD").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException exception) { return; } final ItemStack stack = new ItemStack(material, 1); final var meta = stack.getItemMeta(); if (meta != null) { meta.displayName(Component.text(definition.descriptor().displayName())); stack.setItemMeta(meta); } this.itemEncoder.encode(this.items.create(itemId), stack); if (preferredSlot >= 0) player.getInventory().setItem(preferredSlot, stack); else player.getInventory().addItem(stack); }
    private PlayerProfile onlineProfile(final UUID playerId) { return playerId == null ? null : this.profiles.online(playerId).orElse(null); }
    private void save(final UUID playerId) { this.profiles.save(playerId); }
    private static <T> T component(final PlayerProfile profile, final String id, final Class<T> type) { return profile.components().find(id).map(type::cast).orElseThrow(() -> new IllegalStateException("Missing profile component: " + id)); }
    private static QuestProgressProfileComponent state(final PlayerProfile profile) { return component(profile, "quests", QuestProgressProfileComponent.class); }
    private static String progressKey(final String questId, final int index) { return questId + ":" + index; }
    private static AssetId parseId(final String value) { if (value == null || value.isBlank()) return null; try { return AssetId.parse(value); } catch (RuntimeException exception) { return null; } }
    private static Result combine(final Result first, final Result second) { return first.status() == Status.SUCCESS || second.status() == Status.SUCCESS ? Result.success() : first.status() == Status.NOOP && second.status() == Status.NOOP ? Result.noop() : first; }

    public enum Status { SUCCESS, NOOP, REJECTED }
    public record Result(Status status, String reason) { public static Result success() { return new Result(Status.SUCCESS, ""); } public static Result noop() { return new Result(Status.NOOP, ""); } public static Result rejected(final String reason) { return new Result(Status.REJECTED, reason); } }
    private record ObjectiveMatcher(String type, String valueKey, String value) { static ObjectiveMatcher type(final String type) { return new ObjectiveMatcher(type, "", ""); } ObjectiveMatcher value(final String value) { return new ObjectiveMatcher(this.type, this.type.equals("defeat_boss") ? "boss" : this.type.equals("defeat") ? "entity" : this.type.equals("talk_to_npc") ? "npc" : "location", value); } boolean matches(final com.ascension.serialization.SerializedObject objective) { return this.type.equalsIgnoreCase(objective.getString("type", "")) && (this.value.isBlank() || this.value.equals(objective.getString(this.valueKey, ""))); } }
}
