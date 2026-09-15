package com.ascension.phase9;

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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

/** Player-facing Floor 1 journal. It renders from the authored quest contract, not registry availability. */
public final class QuestMenuService implements Listener {
    private static final String TITLE = "✦ Ascension Journal ✦";
    private static final String[] QUESTS = {QuestService.ARRIVAL, QuestService.FIRST_HUNT, QuestService.FIRST_GATE};
    private final QuestService quests;

    public QuestMenuService(final QuestService quests, final com.ascension.registry.RegistryHub registries) {
        this.quests = Objects.requireNonNull(quests, "quests");
    }

    public void open(final Player player) {
        final JournalHolder holder = new JournalHolder(player.getUniqueId(), null);
        final Inventory inv = Bukkit.createInventory(holder, 54, Component.text(TITLE));
        holder.bind(inv);
        renderMain(player, inv);
        player.openInventory(inv);
    }

    private void renderMain(final Player player, final Inventory inv) {
        inv.clear();
        frame(inv);
        inv.setItem(4, icon(Material.NETHER_STAR, "§6ASCENSION · FLOOR 1", List.of("§7The First Field", "§8Haven → Wilderness → First Gate")));
        int slot = 20;
        for (String id : QUESTS) {
            final boolean complete = quests.isCompleted(player.getUniqueId(), id);
            final boolean active = quests.active(player.getUniqueId()).contains(id);
            final Material mat = complete ? Material.EMERALD : active ? Material.ENCHANTED_BOOK : Material.WRITABLE_BOOK;
            inv.setItem(slot++, icon(mat, (complete ? "§a✓ " : active ? "§e⚔ " : "§7") + name(id),
                List.of(statusLine(active, complete), "", "§fClick to inspect")));
        }
        inv.setItem(49, icon(Material.BARRIER, "§8Close", List.of("§7Close journal")));
    }

    private void renderQuest(final Player player, final Inventory inv, final String id) {
        inv.clear(); frame(inv);
        final boolean complete = quests.isCompleted(player.getUniqueId(), id);
        final boolean active = quests.active(player.getUniqueId()).contains(id);
        inv.setItem(4, icon(complete ? Material.EMERALD : Material.ENCHANTED_BOOK, (complete ? "§a" : "§e") + name(id), List.of("§7" + description(id), "", statusLine(active, complete))));
        if (QuestService.ARRIVAL.equals(id)) {
            objective(inv, 20, "Speak with Warden Lyra", quests.objectiveProgress(player.getUniqueId(), id, 0), 1);
            inv.setItem(29, icon(Material.GOLD_INGOT, "§6Rewards", List.of("§f50 XP", "§e1 Ascent Token")));
        } else if (QuestService.FIRST_HUNT.equals(id)) {
            objective(inv, 19, "Kill 5 Forest Wolves", quests.objectiveProgress(player.getUniqueId(), id, 0), 5);
            objective(inv, 21, "Kill 3 Iron Beetles", quests.objectiveProgress(player.getUniqueId(), id, 1), 3);
            inv.setItem(29, icon(Material.GOLD_INGOT, "§6Rewards", List.of("§f150 XP", "§e1 Ascent Token", "§7Unlocks: The First Gate")));
        } else if (QuestService.FIRST_GATE.equals(id)) {
            objective(inv, 20, "Reach the First Gate", quests.objectiveProgress(player.getUniqueId(), id, 0), 1);
            objective(inv, 22, "Defeat the Warden of the First Gate", quests.objectiveProgress(player.getUniqueId(), id, 1), 1);
            inv.setItem(29, icon(Material.GOLD_INGOT, "§6Rewards", List.of("§f500 XP", "§e2 Ascent Tokens", "§bUnlocks Floor 2")));
        }
        inv.setItem(45, icon(Material.ARROW, "§7Back", List.of("§7Return to journal")));
        inv.setItem(49, icon(Material.BARRIER, "§8Close", List.of("§7Close journal")));
    }

    private void objective(final Inventory inv, final int slot, final String text, final int current, final int needed) {
        final boolean done = current >= needed;
        inv.setItem(slot, icon(done ? Material.LIME_DYE : Material.GRAY_DYE, (done ? "§a☑ " : "§7☐ ") + text, List.of("§7Progress: §f" + Math.min(current, needed) + "§7/§f" + needed)));
    }

    private static String name(final String id) { return switch (id) { case QuestService.ARRIVAL -> "Arrival"; case QuestService.FIRST_HUNT -> "The First Hunt"; case QuestService.FIRST_GATE -> "The First Gate"; default -> id; }; }
    private static String description(final String id) { return switch (id) { case QuestService.ARRIVAL -> "Begin your journey and meet the Warden."; case QuestService.FIRST_HUNT -> "Prove yourself in the wilderness outside Haven."; case QuestService.FIRST_GATE -> "Reach the sealed gate and face its guardian."; default -> ""; }; }
    private static String statusLine(final boolean active, final boolean complete) { return complete ? "§aCompleted" : active ? "§eIn Progress" : "§8Not Started"; }

    private static void frame(final Inventory inv) {
        final ItemStack pane = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, pane);
    }

    private static ItemStack icon(final Material mat, final String title, final List<String> lore) {
        final ItemStack stack = new ItemStack(mat);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.displayName(Component.text(strip(title))); meta.lore(lore.stream().map(s -> Component.text(strip(s))).toList()); stack.setItemMeta(meta); }
        return stack;
    }

    private static String strip(final String s) { return s.replaceAll("§[0-9a-fk-or]", ""); }

    @EventHandler public void onClick(final InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof JournalHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        if (holder.questId == null) {
            if (slot == 20) openQuest(player, QuestService.ARRIVAL);
            else if (slot == 21) openQuest(player, QuestService.FIRST_HUNT);
            else if (slot == 22) openQuest(player, QuestService.FIRST_GATE);
            else if (slot == 49) player.closeInventory();
        } else {
            if (slot == 45) open(player);
            else if (slot == 49) player.closeInventory();
        }
    }

    @EventHandler public void onDrag(final InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof JournalHolder) event.setCancelled(true); }

    private void openQuest(final Player player, final String id) {
        final JournalHolder holder = new JournalHolder(player.getUniqueId(), id);
        final Inventory inv = Bukkit.createInventory(holder, 54, Component.text(TITLE));
        holder.bind(inv);
        renderQuest(player, inv, id);
        player.openInventory(inv);
    }

    private static final class JournalHolder implements InventoryHolder {
        private final UUID playerId;
        private final String questId;
        private Inventory inv;
        JournalHolder(final UUID playerId, final String questId) { this.playerId = playerId; this.questId = questId; }
        void bind(final Inventory inv) { this.inv = inv; }
        @Override public Inventory getInventory() { return inv; }
    }
}
