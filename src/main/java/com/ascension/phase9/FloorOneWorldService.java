package com.ascension.phase9;

import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.service.AbilityService;
import com.ascension.assets.model.AssetId;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.service.ItemService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.EntityEffect;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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

/** Playable Floor 1 world with a large procedural overworld and authored Haven settlement. */
public final class FloorOneWorldService implements Listener {
    public static final String WORLD_NAME = "ascension_floor_001";
    private static final String WORLD_VERSION = "floor_001_world_v3";
    private static final int GATE_Z = 720;
    private static final int BOSS_Z = 755;
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
        this.shuttingDown = false;
        this.prepareWorldMigration();
        this.world = loadWorld();
        configureWorld(this.world);
        if (!hasCurrentWorldBuild()) {
            buildFloor(this.world);
            markProvisioned();
        }
        ensureNpcs();
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.plugin.getLogger().info("Floor 1 world ready: " + WORLD_NAME + " / 10,000 block border / " + WORLD_VERSION);
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
        if (!entered || !player.getWorld().equals(this.world)) {
            giveStarterEquipment(player);
            player.getPersistentDataContainer().set(this.enteredKey, PersistentDataType.BYTE, (byte) 1);
            player.teleport(spawnLocation());
            player.setRespawnLocation(spawnLocation(), true);
            player.sendTitle("Ascension Tower", "Floor 1 — The First Field", 10, 70, 20);
            player.sendMessage("§8Welcome to §6Haven§8, last safe settlement before the First Gate.");
        } else if (player.getInventory().getItem(0) == null || player.getInventory().getItem(0).getType() != Material.IRON_SWORD) {
            giveStarterEquipment(player);
        }
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
            location(92, 74, 180), location(-90, 74, 205), location(150, 76, 260),
            location(-145, 75, 290), location(52, 74, 335)
        };
        final Location[] beetleSpawns = {
            location(175, 78, 220), location(-175, 78, 245), location(125, 75, 345)
        };
        for (int i = wolves; i < wolfSpawns.length; i++) this.mobs.spawnMob(AssetId.parse(MOB_WOLF), wolfSpawns[i]);
        for (int i = beetles; i < beetleSpawns.length; i++) this.mobs.spawnMob(AssetId.parse(MOB_BEETLE), beetleSpawns[i]);
    }

    public void ensureBoss() {
        if (this.world == null) return;
        for (final Entity entity : this.world.getEntities()) {
            if (BOSS_WARDEN.equals(this.mobs.bossId(entity).map(AssetId::toString).orElse("")) && !entity.isDead()) return;
        }
        final LivingEntity boss = this.mobs.spawnBoss(AssetId.parse(BOSS_WARDEN), location(0, terrainY(BOSS_Z), BOSS_Z)).orElse(null);
        if (boss != null) {
            boss.setPersistent(true);
            boss.setGlowing(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCustomNameVisible(true);
            boss.playEffect(EntityEffect.HURT);
            this.plugin.getLogger().info("Spawned Floor 1 Warden boss encounter.");
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
                player.sendMessage("§6Warden Lyra§f: The Tower has chosen you. Beyond these walls lie the Whispering Woods.");
                player.sendMessage("§6Warden Lyra§f: Hunt §f5 Forest Wolves §6and §f3 Iron Beetles§6, then follow the north road.");
                ensureHuntMobs();
            } else {
                player.sendMessage("§6Warden Lyra§f: Follow the north road. The old stones lead all the way to the First Gate.");
            }
        } else if (NPC_REN.equals(npc)) {
            player.sendMessage("§aMerchant Ren§f: Rookie gear will not carry you forever. Bring me trophies from the woods.");
        } else {
            player.sendMessage("§e" + displayNpcName(npc) + "§f: Haven remembers every climber who passes through its gates.");
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
        if (player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) {
            player.sendActionBar("§eEquip your Rookie Sword to attack.");
            return;
        }
        final var result = this.abilities.execute(new AbilityRequest(player.getUniqueId(), "ascension:quick_strike", target.getUniqueId()));
        if (result.status() == com.ascension.abilities.model.AbilityResult.Status.SUCCESS) {
            target.playEffect(EntityEffect.HURT);
            player.sendActionBar("§cQuick Strike");
        } else if (!result.reason().toLowerCase(Locale.ROOT).contains("cooldown")) {
            player.sendActionBar("§7" + result.reason());
        }
    }

    @EventHandler
    public void onTarget(final EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player) || this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        final String id = this.mobs.mobId(event.getEntity()).map(AssetId::toString).orElse("");
        if (!MOB_WOLF.equals(id) && !MOB_BEETLE.equals(id)) return;
        if (player.getLocation().distanceSquared(new Location(this.world, 0, 0, 110)) > 250000D) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(final EntityDeathEvent event) {
        if (this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        final boolean authored = this.mobs.mobId(event.getEntity()).isPresent() || this.mobs.bossId(event.getEntity()).isPresent();
        if (!authored) return;
        this.mobs.forget(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        if (this.world == null || !event.getPlayer().getWorld().equals(this.world)) return;
        final Player player = event.getPlayer();
        if (player.getLocation().getZ() < GATE_Z || !this.quests.active(player.getUniqueId()).contains(QUEST_FIRST_GATE)) return;
        final QuestService.Result result = this.quests.reachLocation(player.getUniqueId(), "first_gate");
        if (result.status() == QuestService.Status.SUCCESS) {
            player.sendMessage("§cThe First Gate§f: A stone colossus stirs beyond the battlements.");
            ensureBoss();
        }
    }

    private World loadWorld() {
        final World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) return existing;
        final WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(new FloorOneWorldGenerator());
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
        target.getWorldBorder().setCenter(0, 0);
        target.getWorldBorder().setSize(10000);
        target.setSpawnLocation(0, 74, -70);
    }

    private void buildFloor(final World target) {
        final int y = 72;
        buildHaven(target, y);
        buildNorthRoad(target);
        buildSouthRoad(target);
        buildEastRoad(target);
        buildWestRoad(target);
        buildWhisperingWoodsOutpost(target);
        buildFirstGate(target);
        buildSign(target, 0, y + 4, -102, "HAVEN", "Last safe settlement");
        buildSign(target, 0, terrainY(160) + 4, 160, "WHISPERING WOODS", "First Hunt grounds");
        buildSign(target, 0, terrainY(GATE_Z) + 4, GATE_Z - 8, "THE FIRST GATE", "Only the worthy pass");
    }

    private void buildHaven(final World target, final int y) {
        // Town square and broad stone avenues.
        fill(target, -72, -112, 72, -28, y, Material.STONE_BRICKS);
        fill(target, -52, -98, 52, -46, y + 1, Material.POLISHED_ANDESITE);
        buildPath(target, -160, -65, 160, -65, 5);
        buildPath(target, 0, -130, 0, 165, 5);

        // Central plaza.
        fill(target, -22, -92, 22, -48, y + 1, Material.SMOOTH_STONE);
        buildFountain(target, 0, y + 2, -70);
        buildBell(target, 0, y + 3, -38);

        buildBuilding(target, -60, y + 1, -100, 46, 25, "WARDEN HALL", Material.STONE_BRICKS, Material.DEEPSLATE_TILES, Material.OAK_LOG);
        buildBuilding(target, 14, y + 1, -100, 46, 25, "REN'S MARKET", Material.SPRUCE_PLANKS, Material.SPRUCE_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, -105, y + 1, -58, 34, 26, "HAVEN INN", Material.OAK_PLANKS, Material.SPRUCE_SLAB, Material.OAK_LOG);
        buildBuilding(target, 72, y + 1, -58, 34, 26, "BLACKSMITH", Material.STONE_BRICKS, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE);
        buildBuilding(target, -105, y + 1, -5, 34, 26, "GUILD HALL", Material.SPRUCE_PLANKS, Material.DARK_OAK_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, 72, y + 1, -5, 34, 26, "STABLES", Material.OAK_PLANKS, Material.OAK_SLAB, Material.OAK_LOG);
        buildBuilding(target, -48, y + 1, 30, 38, 26, "FLETCHER'S YARD", Material.OAK_PLANKS, Material.SPRUCE_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, 10, y + 1, 30, 38, 26, "FARMWARD", Material.OAK_PLANKS, Material.OAK_SLAB, Material.OAK_LOG);

        buildWatchtower(target, -125, y + 1, -115);
        buildWatchtower(target, 125, y + 1, -115);
        buildWatchtower(target, -125, y + 1, 45);
        buildWatchtower(target, 125, y + 1, 45);
        buildWall(target);
        buildMarketStalls(target, y + 2);
        buildFlowerBeds(target, y + 2);
    }

    private void buildNorthRoad(final World target) { buildPath(target, 0, 85, 0, GATE_Z, 4); }
    private void buildSouthRoad(final World target) { buildPath(target, 0, -105, 0, -430, 4); }
    private void buildEastRoad(final World target) { buildPath(target, 150, -60, 620, -60, 4); }
    private void buildWestRoad(final World target) { buildPath(target, -150, -60, -620, -60, 4); }

    private void buildWhisperingWoodsOutpost(final World target) {
        final int z = 405;
        buildBuilding(target, -38, terrainY(z) + 1, z, 30, 18, "RANGER CAMP", Material.SPRUCE_PLANKS, Material.SPRUCE_SLAB, Material.SPRUCE_LOG);
        buildWatchtower(target, 45, terrainY(z) + 1, z - 4);
        buildSign(target, -5, terrainY(z) + 4, z - 10, "WHISPERING WOODS", "Wolves roam ahead");
    }

    private void buildFirstGate(final World target) {
        final int baseY = terrainY(GATE_Z);
        for (int x = -18; x <= 18; x++) {
            for (int yy = baseY + 1; yy <= baseY + 13; yy++) {
                if (Math.abs(x) <= 4 && yy < baseY + 8) continue;
                target.getBlockAt(x, yy, GATE_Z).setType(Material.DEEPSLATE_BRICKS);
            }
        }
        for (int x = -4; x <= 4; x++) for (int yy = baseY + 8; yy <= baseY + 13; yy++) target.getBlockAt(x, yy, GATE_Z).setType(Material.IRON_BARS);
        for (int yy = baseY + 1; yy <= baseY + 16; yy++) {
            target.getBlockAt(-19, yy, GATE_Z).setType(Material.DEEPSLATE_BRICKS);
            target.getBlockAt(19, yy, GATE_Z).setType(Material.DEEPSLATE_BRICKS);
        }
        buildWatchtower(target, -26, baseY + 1, GATE_Z - 10);
        buildWatchtower(target, 26, baseY + 1, GATE_Z - 10);
        buildBuilding(target, -32, baseY + 1, GATE_Z + 35, 64, 22, "FIRST GATE ENCLAVE", Material.COBBLED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.DEEPSLATE);
    }

    private void buildWall(final World target) {
        for (int x = -150; x <= 150; x++) {
            for (int yy = 73; yy <= 78; yy++) {
                target.getBlockAt(x, yy, -122).setType(Material.STONE_BRICKS);
                target.getBlockAt(x, yy, 63).setType(Material.STONE_BRICKS);
            }
        }
        for (int z = -122; z <= 63; z++) {
            for (int yy = 73; yy <= 78; yy++) {
                target.getBlockAt(-150, yy, z).setType(Material.STONE_BRICKS);
                target.getBlockAt(150, yy, z).setType(Material.STONE_BRICKS);
            }
        }
        buildGateOpening(target, 0, -122);
        buildGateOpening(target, 0, 63);
    }

    private static void buildGateOpening(final World world, final int centerX, final int z) {
        for (int x = centerX - 7; x <= centerX + 7; x++) for (int y = 73; y <= 78; y++) world.getBlockAt(x, y, z).setType(Material.AIR);
    }

    private void buildBuilding(final World target, final int x, final int y, final int z, final int width, final int depth, final String label, final Material wall, final Material roof, final Material frame) {
        for (int px = x; px < x + width; px++) {
            for (int pz = z; pz < z + depth; pz++) {
                target.getBlockAt(px, y, pz).setType(wall);
            }
        }
        for (int px = x; px < x + width; px++) {
            for (int py = y + 1; py <= y + 6; py++) {
                target.getBlockAt(px, py, z).setType(frame);
                target.getBlockAt(px, py, z + depth - 1).setType(frame);
            }
        }
        for (int pz = z; pz < z + depth; pz++) {
            for (int py = y + 1; py <= y + 6; py++) {
                target.getBlockAt(x, py, pz).setType(frame);
                target.getBlockAt(x + width - 1, py, pz).setType(frame);
            }
        }
        for (int px = x + 1; px < x + width - 1; px++) for (int pz = z + 1; pz < z + depth - 1; pz++) {
            for (int py = y + 1; py <= y + 5; py++) target.getBlockAt(px, py, pz).setType(Material.AIR);
        }
        for (int px = x + 1; px < x + width - 1; px += 2) {
            for (int layer = 0; layer < 3; layer++) {
                target.getBlockAt(px, y + 7 + layer, z + layer).setType(roof);
                target.getBlockAt(px, y + 7 + layer, z + depth - 1 - layer).setType(roof);
            }
        }
        for (int py = y + 1; py <= y + 2; py++) target.getBlockAt(x + width / 2, py, z).setType(Material.AIR);
        addWindows(target, x, y + 3, z, width, depth);
        buildSign(target, x + 2, y + 7, z - 1, label, "");
    }

    private static void addWindows(final World target, final int x, final int y, final int z, final int width, final int depth) {
        for (int wx = x + 4; wx < x + width - 2; wx += 7) {
            target.getBlockAt(wx, y, z).setType(Material.GLASS_PANE);
            target.getBlockAt(wx, y, z + depth - 1).setType(Material.GLASS_PANE);
        }
        for (int wz = z + 4; wz < z + depth - 2; wz += 7) {
            target.getBlockAt(x, y, wz).setType(Material.GLASS_PANE);
            target.getBlockAt(x + width - 1, y, wz).setType(Material.GLASS_PANE);
        }
    }

    private static void buildWatchtower(final World target, final int x, final int y, final int z) {
        for (int px = x - 2; px <= x + 2; px++) for (int pz = z - 2; pz <= z + 2; pz++) {
            for (int py = y; py <= y + 8; py++) target.getBlockAt(px, py, pz).setType(Material.STONE_BRICKS);
        }
        for (int px = x - 1; px <= x + 1; px++) for (int pz = z - 1; pz <= z + 1; pz++) for (int py = y + 1; py <= y + 7; py++) target.getBlockAt(px, py, pz).setType(Material.AIR);
        for (int px = x - 3; px <= x + 3; px++) for (int pz = z - 3; pz <= z + 3; pz++) target.getBlockAt(px, y + 9, pz).setType(Material.DEEPSLATE_TILES);
        target.getBlockAt(x, y + 10, z).setType(Material.SOUL_LANTERN);
    }

    private static void buildFountain(final World target, final int x, final int y, final int z) {
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) target.getBlockAt(x + dx, y, z + dz).setType(Material.STONE_BRICKS);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) target.getBlockAt(x + dx, y + 1, z + dz).setType(Material.WATER);
        for (int dy = 1; dy <= 4; dy++) target.getBlockAt(x, y + dy, z).setType(Material.QUARTZ_BLOCK);
        target.getBlockAt(x, y + 5, z).setType(Material.WATER);
    }

    private static void buildBell(final World target, final int x, final int y, final int z) {
        target.getBlockAt(x, y, z).setType(Material.STONE_BRICKS);
        target.getBlockAt(x, y + 1, z).setType(Material.OAK_FENCE);
        target.getBlockAt(x, y + 2, z).setType(Material.BELL);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) if (Math.abs(dx) + Math.abs(dz) == 2) target.getBlockAt(x + dx, y + 1, z + dz).setType(Material.SPRUCE_FENCE);
    }

    private static void buildMarketStalls(final World target, final int y) {
        final int[][] points = {{-36, -56}, {-10, -56}, {16, -56}, {42, -56}, {-36, -44}, {-10, -44}, {16, -44}, {42, -44}};
        for (int[] p : points) {
            final int x = p[0], z = p[1];
            for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) target.getBlockAt(x + dx, y, z + dz).setType(Material.SPRUCE_PLANKS);
            for (int dx = -2; dx <= 2; dx++) {
                target.getBlockAt(x + dx, y + 1, z - 1).setType(Material.STRIPPED_SPRUCE_LOG);
                target.getBlockAt(x + dx, y + 4, z - 1).setType(Material.WHITE_WOOL);
                target.getBlockAt(x + dx, y + 4, z).setType(Material.WHITE_WOOL);
            }
            target.getBlockAt(x - 2, y + 1, z - 1).setType(Material.SPRUCE_FENCE);
            target.getBlockAt(x + 2, y + 1, z - 1).setType(Material.SPRUCE_FENCE);
        }
    }

    private static void buildFlowerBeds(final World target, final int y) {
        for (int x = -55; x <= 55; x += 11) {
            target.getBlockAt(x, y, -40).setType(Material.MOSS_BLOCK);
            target.getBlockAt(x + 1, y, -40).setType(Material.MOSS_BLOCK);
            target.getBlockAt(x, y + 1, -40).setType(Material.POPPY);
            target.getBlockAt(x + 1, y + 1, -40).setType(Material.DANDELION);
        }
    }

    private void buildPath(final World target, final int x1, final int z1, final int x2, final int z2, final int radius) {
        final int sx = Integer.signum(x2 - x1);
        final int sz = Integer.signum(z2 - z1);
        int x = x1, z = z1;
        int steps = 0;
        while (true) {
            final int surface = terrainY(x, z);
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                final int y = Math.max(1, surface);
                target.getBlockAt(x + dx, y, z + dz).setType(Material.POLISHED_ANDESITE);
            }
            if (x == x2 && z == z2) break;
            if (x != x2) x += sx;
            if (z != z2) z += sz;
            if (++steps > 7000) break;
        }
    }

    private void ensureNpcs() {
        ensureNpc(NPC_LYRA, "§6Warden Lyra", location(0, 73, -86), Villager.Profession.ARMORER);
        ensureNpc(NPC_REN, "§aMerchant Ren", location(38, 73, -86), Villager.Profession.FLETCHER);
        ensureNpc("ascension:innkeeper_mara", "§dInnkeeper Mara", location(-88, 73, -45), Villager.Profession.LIBRARIAN);
        ensureNpc("ascension:blacksmith_dain", "§cBlacksmith Dain", location(88, 73, -45), Villager.Profession.TOOLSMITH);
        ensureNpc("ascension:guide_elian", "§bGuide Elian", location(0, 73, 15), Villager.Profession.CARTOGRAPHER);
        ensureNpc("ascension:stablemaster_kael", "§eStablemaster Kael", location(88, 73, 10), Villager.Profession.FARMER);
    }

    private void ensureNpc(final String id, final String name, final Location location, final Villager.Profession profession) {
        for (final Entity entity : this.world.getNearbyEntities(location, 8, 5, 8)) {
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
            material = Material.valueOf(definition.data().getString("material", "WOODEN_SWORD").toUpperCase(Locale.ROOT));
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

    private int terrainY(final int z) {
        if (this.world == null) return 72;
        return this.world.getHighestBlockYAt(0, z);
    }

    private Location spawnLocation() { return location(0, 73, -72); }
    private Location location(final double x, final double y, final double z) { return new Location(this.world, x + 0.5, y, z + 0.5); }

    private void buildSign(final World world, final int x, final int y, final int z, final String line1, final String line2) {
        final var block = world.getBlockAt(x, y, z);
        block.setType(Material.OAK_SIGN);
        if (block.getState() instanceof Sign sign) {
            sign.line(1, net.kyori.adventure.text.Component.text(line1));
            if (!line2.isBlank()) sign.line(2, net.kyori.adventure.text.Component.text(line2));
            sign.update(true, false);
        }
    }

    private static String displayNpcName(final String npc) {
        final int colon = npc.indexOf(':');
        final String path = colon >= 0 ? npc.substring(colon + 1) : npc;
        return path.replace('_', ' ');
    }

    private void prepareWorldMigration() {
        if (!Files.exists(this.buildMarker)) return;
        try {
            if (Files.readString(this.buildMarker).contains(WORLD_VERSION)) return;
        } catch (IOException exception) {
            this.plugin.getLogger().warning("Could not read Floor 1 world version marker; rebuilding world.");
        }
        final World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) {
            final World fallback = Bukkit.getWorlds().stream().filter(candidate -> !candidate.equals(existing)).findFirst().orElse(null);
            if (fallback != null) {
                for (final Player player : existing.getPlayers()) player.teleport(fallback.getSpawnLocation());
            }
            if (!Bukkit.unloadWorld(existing, false)) {
                throw new IllegalStateException("Cannot unload legacy Ascension Floor 1 world.");
            }
        }
        final Path folder = this.plugin.getServer().getWorldContainer().toPath().resolve(WORLD_NAME);
        if (Files.exists(folder)) deleteRecursively(folder);
        try { Files.deleteIfExists(this.buildMarker); } catch (IOException exception) { throw new IllegalStateException("Cannot clear world version marker", exception); }
    }

    private boolean hasCurrentWorldBuild() {
        try { return Files.exists(this.buildMarker) && Files.readString(this.buildMarker).contains(WORLD_VERSION); }
        catch (IOException exception) { return false; }
    }

    private void markProvisioned() {
        try {
            Files.createDirectories(this.buildMarker.getParent());
            Files.writeString(this.buildMarker, WORLD_VERSION + "\n");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write Floor 1 build marker", exception);
        }
    }

    private static void deleteRecursively(final Path root) {
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException exception) { throw new IllegalStateException("Failed to delete " + path, exception); }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect legacy world " + root, exception);
        }
    }
}
