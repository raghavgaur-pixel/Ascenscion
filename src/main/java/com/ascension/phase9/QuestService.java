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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;

/** Persistent Floor 1 quest engine with robust gameplay fallbacks. */
public final class QuestService {
    public static final String ARRIVAL = "ascension:arrival";
    public static final String FIRST_HUNT = "ascension:first_hunt";
    public static final String FIRST_GATE = "ascension:the_first_gate";
    public static final String LYRA = "ascension:warden_lyra";
    public static final String NPC_REN = "ascension:merchant_ren";
    public static final String WOLF = "ascension:forest_wolf";
    public static final String BEETLE = "ascension:iron_beetle";
    public static final String BOSS = "ascension:warden_of_the_first_gate";

    private final RegistryHub registries;
    private final PlayerProfileService profiles;
    private final ProgressionService progression;
    private final TowerService tower;
    private final ItemService items;
    private final ItemMetadataEncoder itemEncoder;
    private final NamespacedKey itemKey;

    public QuestService(final RegistryHub registries, final PlayerProfileService profiles, final ProgressionService progression,
                        final TowerService tower, final ItemService items, final ItemMetadataEncoder itemEncoder) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.tower = Objects.requireNonNull(tower, "tower");
        this.items = Objects.requireNonNull(items, "items");
        this.itemEncoder = Objects.requireNonNull(itemEncoder, "itemEncoder");
        this.itemKey = new NamespacedKey("ascension", "item_id");
    }

    public Result accept(final UUID playerId, final String questId) {
        final PlayerProfile profile = onlineProfile(playerId);
        final String id = normalize(questId);
        if (profile == null) return Result.rejected("Player profile is not loaded");
        if (!isFloorOneQuest(id)) return Result.rejected("Unknown quest: " + questId);
        final QuestProgressProfileComponent state = state(profile);
        if (state.isCompleted(id)) return Result.rejected("Quest is already completed");
        if (state.isActive(id)) return Result.rejected("Quest is already active");
        if (FIRST_HUNT.equals(id) && !state.isCompleted(ARRIVAL)) return Result.rejected("Finish Arrival first");
        if (FIRST_GATE.equals(id) && !state.isCompleted(FIRST_HUNT)) return Result.rejected("Finish The First Hunt first");
        state.accept(id);
        save(playerId);
        return Result.success();
    }

    public List<String> active(final UUID playerId) { final PlayerProfile profile = onlineProfile(playerId); return profile == null ? List.of() : List.copyOf(state(profile).activeSnapshot()); }
    public boolean isCompleted(final UUID playerId, final String questId) { final PlayerProfile profile = onlineProfile(playerId); return profile != null && state(profile).isCompleted(normalize(questId)); }
    public QuestDefinition definition(final String questId) { final AssetId id = parseId(questId); if (id == null) return null; try { return registries.getOrCreate(AscensionRegistries.QUEST_DEFINITIONS).find(id).orElse(null); } catch (RuntimeException ignored) { return null; } }
    public int objectiveProgress(final UUID playerId, final String questId, final int index) { final PlayerProfile profile = onlineProfile(playerId); return profile == null ? 0 : state(profile).objectiveProgress(progressKey(normalize(questId), index)); }

    public Result talkToNpc(final UUID playerId, final String npcId) {
        if (!LYRA.equalsIgnoreCase(npcId)) return Result.noop();
        final PlayerProfile profile = onlineProfile(playerId);
        if (profile == null) return Result.rejected("Player profile is not loaded");
        final QuestProgressProfileComponent state = state(profile);
        if (!state.isCompleted(ARRIVAL) && !state.isActive(ARRIVAL)) state.accept(ARRIVAL);
        grantStarterItems(playerId);
        return progress(playerId, ARRIVAL, 0, 1);
    }

    public Result reachLocation(final UUID playerId, final String location) { return "first_gate".equalsIgnoreCase(location) ? progress(playerId, FIRST_GATE, 0, 1) : Result.noop(); }
    public Result defeat(final UUID playerId, final String entityId, final String bossId) {
        Result result = Result.noop();
        if (WOLF.equalsIgnoreCase(entityId)) result = progress(playerId, FIRST_HUNT, 0, 5);
        if (BEETLE.equalsIgnoreCase(entityId)) result = progress(playerId, FIRST_HUNT, 1, 3);
        if (BOSS.equalsIgnoreCase(bossId)) result = progress(playerId, FIRST_GATE, 1, 1);
        return result;
    }

    public void grantKillRewards(final UUID playerId, final AssetId definitionId, final boolean boss) {
        final PlayerProfile profile = onlineProfile(playerId);
        if (profile == null) return;
        final long xp = boss ? 150L : (definitionId != null && WOLF.equals(definitionId.toString()) ? 25L : 30L);
        this.progression.grantExperience(component(profile, "progression", ProgressionProfileComponent.class), xp);
        save(playerId);
    }

    public void grantStarterItems(final UUID playerId) {
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        grantOne(player, "ascension:rookie_sword", 0, Material.IRON_SWORD, "Rookie Sword", List.of("Your first blade.", "Attack: +5"));
        grantOne(player, "ascension:rookie_iron_ring", 8, Material.IRON_NUGGET, "Rookie Iron Ring", List.of("A tower-forged starter ring.", "Strength: +2", "Maximum Health: +20"));
        player.getInventory().setHeldItemSlot(0);
    }

    private void grantOne(final Player player, final String id, final int slot, final Material fallback, final String name, final List<String> lore) {
        if (hasItem(player, id)) return;
        ItemStack stack = createItem(id, fallback, name, lore);
        ItemStack old = player.getInventory().getItem(slot);
        if (old == null || old.getType().isAir()) player.getInventory().setItem(slot, stack); else player.getInventory().addItem(stack);
    }

    private ItemStack createItem(final String id, final Material fallback, final String name, final List<String> lore) {
        try {
            AssetId assetId = AssetId.parse(id);
            ItemDefinition definition = items.findDefinition(assetId).orElse(null);
            if (definition != null) {
                Material material = Material.valueOf(definition.data().getString("material", fallback.name()).toUpperCase(Locale.ROOT));
                ItemStack stack = new ItemStack(material);
                var meta = stack.getItemMeta();
                if (meta != null) { meta.displayName(Component.text("§b" + definition.descriptor().displayName())); stack.setItemMeta(meta); }
                itemEncoder.encode(items.create(assetId), stack);
                tagItem(stack, id);
                return stack;
            }
        } catch (RuntimeException ignored) { }
        ItemStack stack = new ItemStack(fallback);
        var meta = stack.getItemMeta();
        if (meta != null) { meta.displayName(Component.text("§b" + name)); meta.lore(lore.stream().map(Component::text).toList()); meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id); stack.setItemMeta(meta); }
        return stack;
    }

    private void tagItem(final ItemStack stack, final String id) { var meta = stack.getItemMeta(); if (meta == null) return; meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id); stack.setItemMeta(meta); }
    private boolean hasItem(final Player player, final String id) { for (ItemStack stack : player.getInventory().getContents()) { if (stack == null || !stack.hasItemMeta()) continue; var pdc = stack.getItemMeta().getPersistentDataContainer(); String stored = pdc.get(itemKey, PersistentDataType.STRING); if (id.equals(stored)) return true; String name = stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName() : ""; if (id.endsWith("rookie_sword") && name.contains("Rookie Sword")) return true; if (id.endsWith("rookie_iron_ring") && name.contains("Rookie Iron Ring")) return true; } return false; }

    private Result progress(final UUID playerId, final String questId, final int objective, final int required) {
        final PlayerProfile profile = onlineProfile(playerId);
        if (profile == null) return Result.rejected("Player profile is not loaded");
        final QuestProgressProfileComponent state = state(profile);
        if (state.isCompleted(questId)) return Result.noop();
        if (!state.isActive(questId)) {
            if (FIRST_HUNT.equals(questId) && !state.isCompleted(ARRIVAL)) return Result.noop();
            if (FIRST_GATE.equals(questId) && !state.isCompleted(FIRST_HUNT)) return Result.noop();
            state.accept(questId);
        }
        String key = progressKey(questId, objective);
        if (state.objectiveProgress(key) < required) state.addProgress(key, 1);
        if (hardcodedComplete(state, questId)) complete(profile, questId);
        save(playerId);
        return Result.success();
    }

    private boolean hardcodedComplete(final QuestProgressProfileComponent state, final String questId) { return switch (questId) { case ARRIVAL -> state.objectiveProgress(progressKey(ARRIVAL, 0)) >= 1; case FIRST_HUNT -> state.objectiveProgress(progressKey(FIRST_HUNT, 0)) >= 5 && state.objectiveProgress(progressKey(FIRST_HUNT, 1)) >= 3; case FIRST_GATE -> state.objectiveProgress(progressKey(FIRST_GATE, 0)) >= 1 && state.objectiveProgress(progressKey(FIRST_GATE, 1)) >= 1; default -> false; }; }

    private void complete(final PlayerProfile profile, final String questId) {
        final QuestProgressProfileComponent state = state(profile); state.complete(questId);
        this.progression.grantExperience(component(profile, "progression", ProgressionProfileComponent.class), switch (questId) { case ARRIVAL -> 50L; case FIRST_HUNT -> 150L; case FIRST_GATE -> 500L; default -> 0L; });
        component(profile, "currency", CurrencyProfileComponent.class).add("ascent_tokens", switch (questId) { case ARRIVAL, FIRST_HUNT -> 1L; case FIRST_GATE -> 2L; default -> 0L; });
        if (FIRST_HUNT.equals(questId)) state.accept(FIRST_GATE);
        if (FIRST_GATE.equals(questId)) this.tower.unlock(component(profile, "unlocked_floors", UnlockedFloorsProfileComponent.class), new FloorId("ascension:floor_002"));
        Player player = Bukkit.getPlayer(profile.uniqueId()); if (player != null) player.sendMessage(Component.text("§6✦ Quest Complete: §f" + displayName(questId)));
    }

    private PlayerProfile onlineProfile(final UUID id) { return id == null ? null : profiles.online(id).orElse(null); }
    private void save(final UUID id) { profiles.save(id); }
    private static <T> T component(final PlayerProfile profile, final String id, final Class<T> type) { return profile.components().find(id).map(type::cast).orElseThrow(() -> new IllegalStateException("Missing profile component: " + id)); }
    private static QuestProgressProfileComponent state(final PlayerProfile profile) { return component(profile, "quests", QuestProgressProfileComponent.class); }
    private static String progressKey(final String quest, final int index) { return quest + ":" + index; }
    private static boolean isFloorOneQuest(final String id) { return ARRIVAL.equals(id) || FIRST_HUNT.equals(id) || FIRST_GATE.equals(id); }
    private static String displayName(final String id) { return switch (id) { case ARRIVAL -> "Arrival"; case FIRST_HUNT -> "The First Hunt"; case FIRST_GATE -> "The First Gate"; default -> id; }; }
    private static String normalize(final String id) { return id == null ? "" : id.toLowerCase(Locale.ROOT); }
    private static AssetId parseId(final String value) { try { return value == null ? null : AssetId.parse(value); } catch (RuntimeException ignored) { return null; } }
    public enum Status { SUCCESS, NOOP, REJECTED }
    public record Result(Status status, String reason) { public static Result success() { return new Result(Status.SUCCESS, ""); } public static Result noop() { return new Result(Status.NOOP, ""); } public static Result rejected(final String reason) { return new Result(Status.REJECTED, reason); } }
}
