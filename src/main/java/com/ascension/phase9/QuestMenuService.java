package com.ascension.phase9;

import com.ascension.assets.definition.QuestDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

/** Interactive quest journal used by NPCs and /quests. */
public final class QuestMenuService implements Listener {
    private static final String TITLE = "✦ Ascension Journal ✦";
    private static final List<String> FLOOR_ONE_QUESTS = List.of(
        "ascension:arrival", "ascension:first_hunt", "ascension:the_first_gate"
    );

    private final QuestService quests;
    private final RegistryHub registries;

    public QuestMenuService(final QuestService quests, final RegistryHub registries) {
        this.quests = Objects.requireNonNull(quests, "quests");
        this.registries = Objects.requireNonNull(registries, "registries");
    }

    public void open(final Player player) {
        final JournalHolder holder = new JournalHolder(player.getUniqueId(), null);
        final Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(TITLE));
        holder.bind(inventory);
        renderMain(player, inventory);
        player.openInventory(inventory);
    }

    private void openQuest(final Player player, final String questId) {
        final JournalHolder holder = new JournalHolder(player.getUniqueId(), questId);
        final Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(TITLE));
        holder.bind(inventory);
        renderQuest(player, inventory, questId);
        player.openInventory(inventory);
    }

    private void renderMain(final Player player, final Inventory inventory) {
        inventory.clear();
        fillFrame(inventory);
        int slot = 10;
        for (String id : FLOOR_ONE_QUESTS) {
            final QuestDefinition quest = definition(id);
            if (quest == null) continue;
            final boolean completed = this.quests.isCompleted(player.getUniqueId(), id);
            final boolean active = this.quests.active(player.getUniqueId()).contains(id);
            final Material material = completed ? Material.EMERALD : active ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK;
            final ItemStack item = icon(material, completed ? "§a✓ " + quest.displayName() : active ? "§e⚔ " + quest.displayName() : "§7" + quest.displayName(),
                List.of(
                    line(active ? "§eIn Progress" : completed ? "§aCompleted" : "§8Not Started"),
                    line("§7" + quest.description()),
                    line(""),
                    line("§fClick to inspect")
                ));
            inventory.setItem(slot++, item);
        }
        inventory.setItem(49, icon(Material.NETHER_STAR, "§6Floor 1", List.of(line("§7The First Field"), line("§7Haven → Woods → First Gate"))));
    }

    private void renderQuest(final Player player, final Inventory inventory, final String questId) {
        inventory.clear();
        fillFrame(inventory);
        final QuestDefinition quest = definition(questId);
        if (quest == null) return;
        final boolean completed = this.quests.isCompleted(player.getUniqueId(), questId);
        final boolean active = this.quests.active(player.getUniqueId()).contains(questId);
        inventory.setItem(13, icon(completed ? Material.EMERALD : active ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK,
            (completed ? "§a" : active ? "§e" : "§7") + quest.displayName(),
            List.of(line("§7" + quest.description()), line(""), line(active ? "§eQuest Active" : completed ? "§aQuest Complete" : "§8Quest Locked"))));

        final List<com.ascension.serialization.SerializedObject> objectives = quest.data().getObjectList("objectives");
        int slot = 20;
        for (int i = 0; i < objectives.size() && slot < 44; i++, slot++) {
            final var objective = objectives.get(i);
            final String type = objective.getString("type", "objective");
            final int current = this.quests.objectiveProgress(player.getUniqueId(), questId, i);
            final int required = Math.max(1, Math.toIntExact(objective.getLong("amount", 1L)));
            final boolean done = completed || current >= required;
            final String label = formatObjective(objective, type);
            inventory.setItem(slot, icon(done ? Material.LIME_DYE : Material.GRAY_DYE,
                (done ? "§a☑ " : "§7☐ ") + label,
                List.of(line("§7Progress: §f" + Math.min(current, required) + "§7/§f" + required))));
        }

        final var rewards = quest.data().getObject("rewards").orElse(com.ascension.serialization.SerializedObject.empty());
        inventory.setItem(31, icon(Material.GOLD_INGOT, "§6Rewards", List.of(
            line("§eXP: §f" + rewards.getLong("experience", 0L)),
            line("§eAscent Tokens: §f" + rewards.getLongMap("currency").getOrDefault("ascent_tokens", 0L)),
            line(rewards.getString("unlock_floor", "").isBlank() ? "§8No floor unlock" : "§bUnlocks the next floor")
        )));
        inventory.setItem(45, icon(Material.ARROW, "§7Back", List.of(line("§7Return to journal"))));
        inventory.setItem(49, icon(Material.BARRIER, "§8Close", List.of(line("§7Close journal"))));
    }

    private QuestDefinition definition(final String id) {
        try {
            return this.registries.getOrCreate(AscensionRegistries.QUEST_DEFINITIONS).find(AssetId.parse(id)).orElse(null);
        } catch (RuntimeException ignored) { return null; }
    }

    private static String formatObjective(final com.ascension.serialization.SerializedObject objective, final String type) {
        return switch (type) {
            case "talk_to_npc" -> "Speak with " + objective.getString("npc", "the NPC");
            case "defeat" -> "Defeat " + objective.getString("entity", "the enemy");
            case "defeat_boss" -> "Defeat " + objective.getString("boss", "the boss");
            case "reach_location" -> "Reach " + objective.getString("location", "the location");
            default -> "Complete objective";
        };
    }

    private static void fillFrame(final Inventory inventory) {
        final ItemStack glass = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inventory.setItem(i, glass);
    }

    private static ItemStack icon(final Material material, final String name, final List<Component> lore) {
        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.displayName(Component.text(strip(name))); meta.lore(lore); stack.setItemMeta(meta); }
        return stack;
    }

    private static Component line(final String text) { return Component.text(strip(text)); }
    private static String strip(final String text) { return text.replaceAll("§[0-9a-fk-or]", ""); }

    @EventHandler
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof JournalHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (holder.questId() == null) {
            if (event.getRawSlot() >= 10 && event.getRawSlot() <= 43) {
                final ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                    final String title = clicked.getItemMeta().getDisplayName();
                    for (String id : FLOOR_ONE_QUESTS) { final QuestDefinition q = definition(id); if (q != null && strip(title).contains(q.displayName())) { openQuest(player, id); return; } }
                }
            }
        } else {
            if (event.getRawSlot() == 45) { open(player); return; }
            if (event.getRawSlot() == 49) { player.closeInventory(); return; }
        }
    }

    @EventHandler public void onDrag(final InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof JournalHolder) event.setCancelled(true); }
    @EventHandler public void onClose(final InventoryCloseEvent event) { }

    private static final class JournalHolder implements InventoryHolder {
        private final UUID playerId;
        private final String questId;
        private Inventory inventory;
        private JournalHolder(final UUID playerId, final String questId) { this.playerId = playerId; this.questId = questId; }
        private void bind(final Inventory inventory) { this.inventory = inventory; }
        private String questId() { return questId; }
        @Override public Inventory getInventory() { return inventory; }
        @SuppressWarnings("unused") public UUID playerId() { return playerId; }
    }
}
