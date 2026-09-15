package com.ascension.phase9;

import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.service.AbilityService;
import com.ascension.assets.model.AssetId;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.service.ItemService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.EntityEffect;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/** Self-contained playable Floor 1 world, NPC, mob, quest, and combat bridge. */
public final class FloorOneWorldService implements Listener {
    public static final String WORLD_NAME = "ascension_floor_001";
    private static final String NPC_LYRA = "ascension:warden_lyra";
    private static final String NPC_REN = "ascension:merchant_ren";
    private static final String QUEST_FIRST_HUNT = "ascension:first_hunt";
    private static final String QUEST_FIRST_GATE = "ascension:the_first_gate";
    private static final String MOB_WOLF = "ascension:forest_wolf";
    private static final String MOB_BEETLE = "ascension:iron_beetle";
    private static final String BOSS_WARDEN = "ascension:warden_of_the_first_gate";

    private final JavaPlugin plugin;
    private final QuestService quests;
    private final MobService mobs;
    private final AbilityService abilities;
    private final ItemService items;
    private final ItemMetadataEncoder itemEncoder;
    private final NamespacedKey npcKey;
    private final NamespacedKey enteredKey;
    private final Path buildMarker;
    private World world;
    private boolean shuttingDown;

    public FloorOneWorldService(
        final JavaPlugin plugin,
        final QuestService quests,
        final MobService mobs,
        final AbilityService abilities,
        final ItemService items,
        final ItemMetadataEncoder itemEncoder
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.mobs = Objects.requireNonNull(mobs, "mobs");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.items = Objects.requireNonNull(items, "items");
        this.itemEncoder = Objects.requireNonNull(itemEncoder, "itemEncoder");
        this.npcKey = new NamespacedKey(plugin, "npc_id");
        this.enteredKey = new NamespacedKey(plugin, "entered_floor_001");
        this.buildMarker = plugin.getDataFolder().toPath().resolve(".floor_001_provisioned");
    }

    public void start() {
        this.world = loadWorld();
        configureWorld(this.world);
        if (!Files.exists(this.buildMarker)) {
            buildFloor(this.world);
            markProvisioned();
        }
        ensureNpcs();
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.plugin.getLogger().info("Floor 1 playable world ready: " + WORLD_NAME);
    }

    public void stop() {
        this.shuttingDown = true;
        if (this.world == null) return;
        for (final Entity entity : this.world.getEntities()) {
            if (this.mobs.mobId(entity).isPresent() || this.mobs.bossId(entity).isPresent()) {
                this.mobs.forget(entity.getUniqueId());
                entity.remove();
            }
        }
    }

    public void preparePlayer(final Player player) {
        if (this.world == null || !player.isOnline()) return;
        final boolean entered = player.getPersistentDataContainer().has(this.enteredKey, PersistentDataType.BYTE);
        if (entered || player.getWorld().equals(this.world)) return;
        giveStarterEquipment(player);
        player.getPersistentDataContainer().set(this.enteredKey, PersistentDataType.BYTE, (byte) 1);
        player.teleport(spawnLocation());
        player.setRespawnLocation(spawnLocation(), true);
        player.sendTitle("Ascension Tower", "Floor 1 — The First Field", 10, 60, 20);
        player.sendMessage("Welcome to Haven. Speak to Warden Lyra to begin your climb.");
    }

    public void ensureHuntMobs() {
        if (this.world == null) return;
        int wolves = 0;
        int beetles = 0;
        for (final Entity entity : this.world.getEntities()) {
            final String id = this.mobs.mobId(entity).map(AssetId::toString).orElse("");
            if (MOB_WOLF.equals(id) && !entity.isDead()) wolves++;
            if (MOB_BEETLE.equals(id) && !entity.isDead()) beetles++;
        }
        final Location[] wolfSpawns = {
            location(18, 65, 18), location(-18, 65, 21), location(24, 65, 31),
            location(-23, 65, 33), location(9, 65, 38)
        };
        final Location[] beetleSpawns = {
            location(28, 65, 25), location(-28, 65, 28), location(20, 65, 39)
        };
        for (int i = wolves; i < wolfSpawns.length; i++) this.mobs.spawnMob(AssetId.parse(MOB_WOLF), wolfSpawns[i]);
        for (int i = beetles; i < beetleSpawns.length; i++) this.mobs.spawnMob(AssetId.parse(MOB_BEETLE), beetleSpawns[i]);
    }

    public void ensureBoss() {
        if (this.world == null) return;
        for (final Entity entity : this.world.getEntities()) {
            if (BOSS_WARDEN.equals(this.mobs.bossId(entity).map(AssetId::toString).orElse("") ) && !entity.isDead()) return;
        }
        final LivingEntity boss = this.mobs.spawnBoss(AssetId.parse(BOSS_WARDEN), location(0, 65, 60)).orElse(null);
        if (boss != null) {
            boss.setPersistent(true);
            boss.setGlowing(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCustomNameVisible(true);
            boss.playEffect(EntityEffect.HURT);
            this.plugin.getLogger().info("Spawned Floor 1 boss encounter.");
        }
    }

    @EventHandler
    public void onNpcInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity)) return;
        final String npc = entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
        if (npc == null || this.world == null || !entity.getWorld().equals(this.world)) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        if (NPC_LYRA.equals(npc)) {
            final QuestService.Result result = this.quests.talkToNpc(player.getUniqueId(), NPC_LYRA);
            if (result.status() == QuestService.Status.SUCCESS) {
                player.sendMessage("§6Warden Lyra§f: The Tower has chosen you. Head into the field and prove you can survive.");
                player.sendMessage("§eQuest started: The First Hunt §7— 5 Forest Wolves, 3 Iron Beetles.");
                ensureHuntMobs();
            } else {
                player.sendMessage("§6Warden Lyra§f: Your path continues beyond Haven. Do not lose sight of the gate.");
            }
        } else if (NPC_REN.equals(npc)) {
            player.sendMessage("§aMerchant Ren§f: Your Rookie Sword is a start. Better gear will come as you climb.");
        }
    }

    @EventHandler
    public void onCombat(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (this.shuttingDown || this.world == null || !target.getWorld().equals(this.world)) return;
        final boolean authored = this.mobs.mobId(target).isPresent() || this.mobs.bossId(target).isPresent();
        if (!authored) return;
        event.setCancelled(true);
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.IRON_SWORD)) {
            player.sendActionBar("§eEquip your Rookie Sword to attack.");
            return;
        }
        final var result = this.abilities.execute(new AbilityRequest(player.getUniqueId(), "ascension:quick_strike", target.getUniqueId()));
        if (result.status() == com.ascension.abilities.model.AbilityResult.Status.SUCCESS) {
            target.playEffect(EntityEffect.HURT);
            player.sendActionBar("§cQuick Strike");
        } else if (!result.reason().contains("cooldown")) {
            player.sendActionBar("§7" + result.reason());
        }
    }

    @EventHandler
    public void onTarget(final EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player) || this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        final String id = this.mobs.mobId(event.getEntity()).map(AssetId::toString).orElse("");
        if (!MOB_WOLF.equals(id) && !MOB_BEETLE.equals(id)) return;
        if (player.getLocation().distanceSquared(spawnLocation()) > 10000D) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(final EntityDeathEvent event) {
        if (this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        final boolean authored = this.mobs.mobId(event.getEntity()).isPresent() || this.mobs.bossId(event.getEntity()).isPresent();
        if (!authored) return;
        this.mobs.forget(event.getEntity().getUniqueId());
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.shuttingDown) return;
            boolean huntActive = false;
            for (final Player online : Bukkit.getOnlinePlayers()) {
                if (this.quests.active(online.getUniqueId()).contains(QUEST_FIRST_HUNT)) {
                    huntActive = true;
                    break;
                }
            }
            if (huntActive) ensureHuntMobs();
        });
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        if (this.world == null || !event.getPlayer().getWorld().equals(this.world)) return;
        final Player player = event.getPlayer();
        if (player.getLocation().getZ() < 47 || !this.quests.active(player.getUniqueId()).contains(QUEST_FIRST_GATE)) return;
        final QuestService.Result result = this.quests.reachLocation(player.getUniqueId(), "first_gate");
        if (result.status() == QuestService.Status.SUCCESS) {
            player.sendMessage("§cThe First Gate§f: The Warden of the First Gate steps forward.");
            ensureBoss();
        }
    }

    private World loadWorld() {
        final World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) return existing;
        final WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generatorSettings("{\"layers\":[{\"height\":1,\"block\":\"minecraft:bedrock\"},{\"height\":63,\"block\":\"minecraft:dirt\"},{\"height\":1,\"block\":\"minecraft:grass_block\"}],\"biome\":\"minecraft:plains\"}");
        return creator.createWorld();
    }

    private void configureWorld(final World target) {
        target.setDifficulty(Difficulty.NORMAL);
        target.setStorm(false);
        target.setThundering(false);
        target.setTime(1000L);
        target.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        target.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        target.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        target.getWorldBorder().setCenter(0, 20);
        target.getWorldBorder().setSize(160);
        target.setSpawnLocation(0, 66, -18);
    }

    private void buildFloor(final World target) {
        final int y = 64;
        fill(target, -50, -35, 50, 75, y, Material.GRASS_BLOCK);
        fill(target, -18, -28, 18, 2, y + 1, Material.STONE_BRICKS);
        path(target, 0, 0, 0, 22);
        path(target, 0, 22, 0, 48);
        path(target, -2, 4, 0, -28);
        buildHaven(target, y);
        buildGate(target, y);
        buildSign(target, 0, y + 3, -29, "HAVEN", "Warden Lyra");
        buildSign(target, 0, y + 3, 45, "THE FIRST GATE", "Guardian beyond");
    }

    private void buildHaven(final World target, final int y) {
        buildHouse(target, -14, y + 1, -8, "WARDEN HALL");
        buildHouse(target, 6, y + 1, -8, "REN'S MARKET");
        fill(target, -5, -8, 5, 2, y + 1, Material.POLISHED_ANDESITE);
        for (int x = -3; x <= 3; x++) for (int z = -6; z <= 0; z++) target.getBlockAt(x, y + 2, z).setType(Material.STONE_BRICKS);
        for (int x = -1; x <= 1; x++) for (int z = -4; z <= -2; z++) target.getBlockAt(x, y + 2, z).setType(Material.WATER);
        target.getBlockAt(0, y + 1, -3).setType(Material.SEA_LANTERN);
    }

    private void buildGate(final World target, final int y) {
        for (int x = -13; x <= 13; x++) for (int yy = y + 1; yy <= y + 8; yy++) {
            if (Math.abs(x) <= 3 && yy < y + 5) continue;
            target.getBlockAt(x, yy, 50).setType(Material.STONE_BRICKS);
        }
        for (int x = -3; x <= 3; x++) for (int yy = y + 5; yy <= y + 8; yy++) target.getBlockAt(x, yy, 50).setType(Material.IRON_BARS);
        for (int yy = y + 1; yy <= y + 10; yy++) {
            target.getBlockAt(-14, yy, 50).setType(Material.STONE_BRICKS);
            target.getBlockAt(14, yy, 50).setType(Material.STONE_BRICKS);
        }
    }

    private void buildHouse(final World target, final int x, final int y, final int z, final String label) {
        fill(target, x, z, x + 10, z + 8, y, Material.OAK_PLANKS);
        for (int px = x; px <= x + 10; px++) {
            target.getBlockAt(px, y + 3, z).setType(Material.OAK_LOG);
            target.getBlockAt(px, y + 3, z + 8).setType(Material.OAK_LOG);
        }
        for (int pz = z; pz <= z + 8; pz++) {
            target.getBlockAt(x, y + 3, pz).setType(Material.OAK_LOG);
            target.getBlockAt(x + 10, y + 3, pz).setType(Material.OAK_LOG);
        }
        for (int px = x + 1; px < x + 10; px++) for (int pz = z + 1; pz < z + 8; pz++) target.getBlockAt(px, y + 1, pz).setType(Material.AIR);
        target.getBlockAt(x + 5, y + 1, z).setType(Material.AIR);
        target.getBlockAt(x + 5, y + 2, z).setType(Material.AIR);
        for (int px = x; px <= x + 10; px++) for (int pz = z; pz <= z + 8; pz++) if ((px + pz) % 2 == 0) target.getBlockAt(px, y + 4, pz).setType(Material.OAK_SLAB);
        buildSign(target, x + 2, y + 3, z - 1, label, "");
    }

    private void ensureNpcs() {
        ensureNpc(NPC_LYRA, "§6Warden Lyra", location(0, 65, -10), Villager.Profession.ARMORER);
        ensureNpc(NPC_REN, "§aMerchant Ren", location(10, 65, -6), Villager.Profession.FLETCHER);
    }

    private void ensureNpc(final String id, final String name, final Location location, final Villager.Profession profession) {
        for (final Entity entity : this.world.getNearbyEntities(location, 5, 3, 5)) {
            if (id.equals(entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING))) return;
        }
        final Villager villager = (Villager) this.world.spawnEntity(location, EntityType.VILLAGER);
        villager.getPersistentDataContainer().set(this.npcKey, PersistentDataType.STRING, id);
        villager.setCustomName(name);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setProfession(profession);
        villager.setPersistent(true);
    }

    private void giveStarterEquipment(final Player player) {
        final var sword = createItem("ascension:rookie_sword");
        final var ring = createItem("ascension:rookie_iron_ring");
        if (sword != null) player.getInventory().setItem(0, sword);
        if (ring != null) player.getInventory().setItem(8, ring);
        player.getInventory().setHeldItemSlot(0);
    }

    private org.bukkit.inventory.ItemStack createItem(final String rawId) {
        final AssetId id = AssetId.parse(rawId);
        final var definition = this.items.findDefinition(id).orElse(null);
        if (definition == null) return null;
        final Material material;
        try {
            material = Material.valueOf(definition.data().getString("material", "WOODEN_SWORD").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
        final var stack = new org.bukkit.inventory.ItemStack(material);
        final var meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(definition.descriptor().displayName()));
            stack.setItemMeta(meta);
        }
        this.itemEncoder.encode(this.items.create(id), stack);
        return stack;
    }

    private Location spawnLocation() { return location(0, 66, -18); }
    private Location location(final double x, final double y, final double z) { return new Location(this.world, x + 0.5, y, z + 0.5); }

    private static void path(final World world, final int x1, final int z1, final int x2, final int z2) {
        final int sx = Integer.signum(x2 - x1);
        final int sz = Integer.signum(z2 - z1);
        int x = x1, z = z1;
        while (true) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) world.getBlockAt(x + dx, 65, z + dz).setType(Material.POLISHED_ANDESITE);
            if (x == x2 && z == z2) break;
            if (x != x2) x += sx;
            if (z != z2) z += sz;
        }
    }

    private static void fill(final World world, final int x1, final int z1, final int x2, final int z2, final int y, final Material material) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) world.getBlockAt(x, y, z).setType(material);
    }

    private static void buildSign(final World world, final int x, final int y, final int z, final String line1, final String line2) {
        final var block = world.getBlockAt(x, y, z);
        block.setType(Material.OAK_SIGN);
        if (block.getState() instanceof Sign sign) {
            sign.line(1, net.kyori.adventure.text.Component.text(line1));
            if (!line2.isBlank()) sign.line(2, net.kyori.adventure.text.Component.text(line2));
            sign.update(true, false);
        }
    }

    private void markProvisioned() {
        try {
            Files.createDirectories(this.buildMarker.getParent());
            Files.writeString(this.buildMarker, "Floor 1 provisioned by Ascension Phase 9\n");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write Floor 1 build marker", exception);
        }
    }
}
