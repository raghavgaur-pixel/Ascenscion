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
import org.bukkit.block.BlockFace;
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

/** Authored Floor 1 world: cinematic Haven, living wilderness, quest encounters, and first boss gate. */
public final class FloorOneWorldService implements Listener {
    public static final String WORLD_NAME = "ascension_floor_001";
    private static final String WORLD_VERSION = "floor_001_world_v5";
    private static final int GATE_Z = 720;
    private static final int BOSS_Z = 770;
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
    private final QuestMenuService journal;
    private final NamespacedKey npcKey;
    private final NamespacedKey enteredKey;
    private final Path buildMarker;
    private World world;
    private boolean shuttingDown;

    public FloorOneWorldService(final JavaPlugin plugin, final QuestService quests, final MobService mobs,
                                final AbilityService abilities, final ItemService items,
                                final ItemMetadataEncoder itemEncoder, final QuestMenuService journal) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.mobs = Objects.requireNonNull(mobs, "mobs");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.items = Objects.requireNonNull(items, "items");
        this.itemEncoder = Objects.requireNonNull(itemEncoder, "itemEncoder");
        this.journal = Objects.requireNonNull(journal, "journal");
        this.npcKey = new NamespacedKey(plugin, "npc_id");
        this.enteredKey = new NamespacedKey(plugin, "entered_floor_001");
        this.buildMarker = plugin.getDataFolder().toPath().resolve(".floor_001_provisioned");
    }

    public void start() {
        this.shuttingDown = false;
        migrateLegacyWorld();
        this.world = loadWorld();
        configureWorld(this.world);
        if (!hasCurrentWorldBuild()) {
            buildFloor(this.world);
            markProvisioned();
        }
        ensureNpcs();
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.plugin.getLogger().info("Floor 1 ready: Haven + wilderness + First Gate / " + WORLD_VERSION);
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
            player.sendTitle("§6ASCENSION", "§fFloor 1 · The First Field", 10, 70, 20);
            player.sendMessage("§8Welcome to §6Haven§8, the last light before the wilds.");
        } else if (!hasRookieSword(player)) {
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
        final int[][] wolfSpawns = {{88,178},{-96,215},{140,250},{-155,305},{62,350},{-55,390},{190,420}};
        final int[][] beetleSpawns = {{172,225},{-180,265},{118,338},{-110,375},{225,445}};
        for (int i = wolves; i < wolfSpawns.length; i++) spawnIfMissing(MOB_WOLF, wolfSpawns[i][0], wolfSpawns[i][1]);
        for (int i = beetles; i < beetleSpawns.length; i++) spawnIfMissing(MOB_BEETLE, beetleSpawns[i][0], beetleSpawns[i][1]);
    }

    public void ensureBoss() {
        if (this.world == null) return;
        for (final Entity entity : this.world.getEntities()) {
            if (BOSS_WARDEN.equals(this.mobs.bossId(entity).map(AssetId::toString).orElse("")) && !entity.isDead()) return;
        }
        final LivingEntity boss = this.mobs.spawnBoss(AssetId.parse(BOSS_WARDEN), location(0, terrainYAt(0, BOSS_Z) + 1, BOSS_Z)).orElse(null);
        if (boss != null) {
            boss.setPersistent(true);
            boss.setGlowing(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCustomNameVisible(true);
            boss.setSilent(false);
            boss.playEffect(EntityEffect.HURT);
        }
    }

    @EventHandler
    public void onNpcInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity)) return;
        final String npc = entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
        if (npc == null || this.world == null || !entity.getWorld().equals(this.world)) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        switch (npc) {
            case NPC_LYRA -> {
                final QuestService.Result result = this.quests.talkToNpc(player.getUniqueId(), NPC_LYRA);
                if (result.status() == QuestService.Status.SUCCESS) {
                    player.sendMessage("§6Warden Lyra§f: The Tower has chosen you. Listen carefully.");
                    player.sendMessage("§6Warden Lyra§f: The Whispering Woods are hungry tonight. Hunt §e5 Wolves§f and §e3 Iron Beetles§f.");
                    player.sendMessage("§6Warden Lyra§f: Return to the north road when the hunt is complete.");
                    ensureHuntMobs();
                } else {
                    player.sendMessage("§6Warden Lyra§f: The north road leads to the First Gate. Do not mistake courage for readiness.");
                }
                this.journal.open(player);
            }
            case NPC_REN -> {
                player.sendMessage("§aMerchant Ren§f: Every trophy tells me how deep you dared to go.");
                player.sendMessage("§7Bring me what you find in the woods. The good stuff never comes cheap.");
                this.journal.open(player);
            }
            case "ascension:innkeeper_mara" -> {
                player.sendMessage("§dInnkeeper Mara§f: Beds are warm, stew is hot, and the Tower never sleeps.");
            }
            case "ascension:blacksmith_dain" -> {
                player.sendMessage("§cBlacksmith Dain§f: That iron sword will do for the first mile. After that, earn something better.");
            }
            case "ascension:guide_elian" -> {
                player.sendMessage("§bGuide Elian§f: Haven is only the beginning. The road north ends at a gate older than the Tower records.");
                this.journal.open(player);
            }
            case "ascension:stablemaster_kael" -> {
                player.sendMessage("§eStablemaster Kael§f: The wild grows harsher beyond the river. Keep your feet under you.");
            }
            default -> player.sendMessage("§7The villager watches you in silence.");
        }
    }

    @EventHandler
    public void onCombat(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (this.shuttingDown || this.world == null || !target.getWorld().equals(this.world)) return;
        if (this.mobs.mobId(target).isEmpty() && this.mobs.bossId(target).isEmpty()) return;
        event.setCancelled(true);
        if (player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) {
            player.sendActionBar("§eRookie Sword required");
            return;
        }
        final var result = this.abilities.execute(new AbilityRequest(player.getUniqueId(), "ascension:quick_strike", target.getUniqueId()));
        if (result.status() == com.ascension.abilities.model.AbilityResult.Status.SUCCESS) {
            target.playEffect(EntityEffect.HURT);
            player.sendActionBar("§c✦ Quick Strike");
        } else if (!result.reason().toLowerCase(Locale.ROOT).contains("cooldown")) {
            player.sendActionBar("§7" + result.reason());
        }
    }

    @EventHandler
    public void onTarget(final EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player) || this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        final String id = this.mobs.mobId(event.getEntity()).map(AssetId::toString).orElse("");
        if (!MOB_WOLF.equals(id) && !MOB_BEETLE.equals(id)) return;
        if (player.getLocation().distanceSquared(new Location(this.world, 0, 0, 100)) > 160000D) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(final EntityDeathEvent event) {
        if (this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        if (this.mobs.mobId(event.getEntity()).isPresent() || this.mobs.bossId(event.getEntity()).isPresent()) {
            this.mobs.forget(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        if (this.world == null || !event.getPlayer().getWorld().equals(this.world)) return;
        final Player player = event.getPlayer();
        if (player.getLocation().getZ() < GATE_Z || !this.quests.active(player.getUniqueId()).contains(QUEST_FIRST_GATE)) return;
        final QuestService.Result result = this.quests.reachLocation(player.getUniqueId(), "first_gate");
        if (result.status() == QuestService.Status.SUCCESS) {
            player.sendTitle("§cTHE FIRST GATE", "§7Something ancient awakens", 10, 70, 20);
            ensureBoss();
        }
    }

    private void spawnIfMissing(final String mobId, final int x, final int z) {
        this.mobs.spawnMob(AssetId.parse(mobId), location(x, terrainYAt(x, z) + 1, z));
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
        target.setTime(1500L);
        target.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        target.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        target.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        target.setGameRule(GameRule.DO_FIRE_TICK, false);
        target.getWorldBorder().setCenter(0, 0);
        target.getWorldBorder().setSize(10000);
        target.setSpawnLocation(0, terrainYAt(0, -70) + 1, -70);
    }

    private void buildFloor(final World target) {
        buildHaven(target);
        buildNorthRoad(target, 88, GATE_Z);
        buildWoodsRoute(target);
        buildFirstGate(target);
        buildAncientRuins(target);
        buildRiverBridge(target);
        buildSign(target, 0, terrainYAt(0, -110) + 4, -110, "HAVEN", "Last safe settlement");
        buildSign(target, 0, terrainYAt(0, 210) + 4, 210, "WHISPERING WOODS", "First Hunt grounds");
        buildSign(target, 0, terrainYAt(0, GATE_Z) + 5, GATE_Z - 8, "THE FIRST GATE", "Only the worthy pass");
    }

    private void buildHaven(final World target) {
        final int base = 72;
        // Broad civic square and layered avenues.
        fill(target, -145, -130, 145, 82, base, Material.STONE_BRICKS);
        fill(target, -125, -112, 125, 62, base + 1, Material.COBBLED_DEEPSLATE);
        buildPath(target, -150, -63, 150, -63, 6, Material.POLISHED_ANDESITE);
        buildPath(target, 0, -140, 0, 180, 6, Material.POLISHED_ANDESITE);
        buildPath(target, -115, -5, 115, -5, 4, Material.STONE_BRICKS);
        buildPath(target, -108, 57, 108, 57, 4, Material.STONE_BRICKS);

        // Central plaza.
        fill(target, -35, -104, 35, -35, base + 2, Material.SMOOTH_STONE);
        buildFountain(target, 0, base + 4, -70);
        buildMonument(target, 0, base + 3, -43);
        buildLampPosts(target, -30, -40, 30, -40);
        buildLampPosts(target, -30, -101, 30, -101);

        buildBuilding(target, -72, base + 3, -106, 56, 30, "WARDEN HALL", Material.STONE_BRICKS, Material.DEEPSLATE_TILES, Material.OAK_LOG);
        buildBuilding(target, 16, base + 3, -106, 56, 30, "REN'S MARKET", Material.SPRUCE_PLANKS, Material.DARK_OAK_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, -116, base + 3, -58, 40, 30, "HAVEN INN", Material.OAK_PLANKS, Material.SPRUCE_SLAB, Material.OAK_LOG);
        buildBuilding(target, 76, base + 3, -58, 40, 30, "BLACKSMITH", Material.STONE_BRICKS, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE);
        buildBuilding(target, -116, base + 3, 1, 40, 30, "GUILD HALL", Material.SPRUCE_PLANKS, Material.DARK_OAK_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, 76, base + 3, 1, 40, 30, "STABLES", Material.OAK_PLANKS, Material.OAK_SLAB, Material.OAK_LOG);
        buildBuilding(target, -52, base + 3, 40, 40, 27, "FLETCHER'S YARD", Material.OAK_PLANKS, Material.SPRUCE_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, 12, base + 3, 40, 40, 27, "FARMWARD", Material.OAK_PLANKS, Material.OAK_SLAB, Material.OAK_LOG);

        buildTownWall(target, base);
        buildGatehouse(target, 0, base + 1, -130, true);
        buildGatehouse(target, 0, base + 1, 82, false);
        buildWatchtower(target, -137, base + 3, -122);
        buildWatchtower(target, 137, base + 3, -122);
        buildWatchtower(target, -137, base + 3, 74);
        buildWatchtower(target, 137, base + 3, 74);
        buildMarketStalls(target, base + 5);
        buildFlowerBeds(target, base + 5);
        buildStreetTrees(target, base + 3);
        buildDocks(target, base + 2, 102, -18);

        // Civic NPCs are distributed through the town, not stacked together.
        // Lyra / Ren remain near their respective buildings.
    }

    private void buildWoodsRoute(final World target) {
        final int[] stations = {170, 260, 370, 485, 600};
        for (int z : stations) {
            buildPath(target, -16, z, 16, z, 3, Material.COARSE_DIRT);
            buildPath(target, -50, z, 50, z, 1, Material.PODZOL);
            buildForestCamp(target, 58, z - 18);
            buildForestCamp(target, -58, z + 12);
        }
    }

    private void buildFirstGate(final World target) {
        final int baseY = terrainYAt(0, GATE_Z);
        for (int x = -34; x <= 34; x++) {
            for (int y = baseY + 1; y <= baseY + 16; y++) {
                if (Math.abs(x) < 7 && y < baseY + 9) continue;
                target.getBlockAt(x, y, GATE_Z).setType(Material.DEEPSLATE_BRICKS);
            }
        }
        for (int side : new int[]{-1, 1}) {
            for (int x = side * 26; x != side * 8; x += side) {
                for (int y = baseY + 1; y <= baseY + 22; y++) target.getBlockAt(x, y, GATE_Z).setType(Material.DEEPSLATE_BRICKS);
            }
            buildWatchtower(target, side * 31, baseY + 1, GATE_Z - 13);
        }
        for (int x = -7; x <= 7; x++) for (int y = baseY + 9; y <= baseY + 16; y++) target.getBlockAt(x, y, GATE_Z).setType(Material.IRON_BARS);
        for (int x = -7; x <= 7; x++) target.getBlockAt(x, baseY + 18, GATE_Z).setType(Material.SOUL_LANTERN);
        buildPath(target, 0, GATE_Z - 55, 0, GATE_Z, 4, Material.POLISHED_DEEPSLATE);
        buildPath(target, 0, GATE_Z, 0, GATE_Z + 70, 5, Material.BLACKSTONE);
        for (int x = -18; x <= 18; x++) for (int z = GATE_Z + 22; z <= GATE_Z + 60; z++) if (Math.abs(x) > 12 || z > GATE_Z + 46) target.getBlockAt(x, terrainYAt(x, z), z).setType(Material.OBSIDIAN);
    }

    private void buildAncientRuins(final World target) {
        final int z = 555;
        final int y = terrainYAt(0, z);
        for (int i = 0; i < 5; i++) {
            final int x = -38 + i * 19;
            for (int h = 0; h < 9 - i; h++) {
                target.getBlockAt(x, y + h, z + (i % 2 == 0 ? 0 : 6)).setType(Material.MOSSY_STONE_BRICKS);
            }
        }
        for (int x = -50; x <= 50; x++) {
            if (x % 7 == 0) target.getBlockAt(x, y + 1, z + 12).setType(Material.CRACKED_STONE_BRICKS);
        }
        buildSign(target, -8, y + 7, z + 11, "THE OLD ROAD", "Ruins from before the Tower");
    }

    private void buildRiverBridge(final World target) {
        final int z = 320;
        final int y = terrainYAt(0, z);
        buildPath(target, -28, z, 28, z, 4, Material.SPRUCE_PLANKS);
        for (int x = -28; x <= 28; x += 7) {
            for (int dz = -3; dz <= 3; dz++) {
                target.getBlockAt(x, y - 2, z + dz).setType(Material.DARK_OAK_LOG);
            }
        }
        buildFenceLine(target, -28, z - 3, 28, z - 3);
        buildFenceLine(target, -28, z + 3, 28, z + 3);
    }

    private void buildForestCamp(final World target, final int x, final int z) {
        final int y = terrainYAt(x, z) + 1;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) target.getBlockAt(x + dx, y, z + dz).setType(Material.COARSE_DIRT);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) if ((dx + dz) % 2 == 0) target.getBlockAt(x + dx, y + 1, z + dz).setType(Material.CAMPFIRE);
        target.getBlockAt(x, y + 1, z).setType(Material.CHEST);
        for (int i = -4; i <= 4; i += 4) buildTree(target, x + i, z + 5);
    }

    private static void buildBuilding(final World target, final int x, final int y, final int z, final int width, final int depth,
                                      final String label, final Material wall, final Material roof, final Material frame) {
        for (int px = x; px < x + width; px++) for (int pz = z; pz < z + depth; pz++) target.getBlockAt(px, y, pz).setType(wall);
        for (int px = x; px < x + width; px++) for (int py = y + 1; py <= y + 7; py++) { target.getBlockAt(px, py, z).setType(frame); target.getBlockAt(px, py, z + depth - 1).setType(frame); }
        for (int pz = z; pz < z + depth; pz++) for (int py = y + 1; py <= y + 7; py++) { target.getBlockAt(x, py, pz).setType(frame); target.getBlockAt(x + width - 1, py, pz).setType(frame); }
        for (int px = x + 1; px < x + width - 1; px++) for (int pz = z + 1; pz < z + depth - 1; pz++) for (int py = y + 1; py <= y + 6; py++) target.getBlockAt(px, py, pz).setType(Material.AIR);
        for (int layer = 0; layer < 4; layer++) { for (int px = x + layer; px < x + width - layer; px++) { target.getBlockAt(px, y + 8 + layer, z + layer).setType(roof); target.getBlockAt(px, y + 8 + layer, z + depth - 1 - layer).setType(roof); } for (int pz = z + layer; pz < z + depth - layer; pz++) { target.getBlockAt(x + layer, y + 8 + layer, pz).setType(roof); target.getBlockAt(x + width - 1 - layer, y + 8 + layer, pz).setType(roof); } }
        for (int py = y + 1; py <= y + 3; py++) target.getBlockAt(x + width / 2, py, z).setType(Material.AIR);
        for (int wx = x + 5; wx < x + width - 3; wx += 8) { target.getBlockAt(wx, y + 3, z).setType(Material.GLASS_PANE); target.getBlockAt(wx, y + 3, z + depth - 1).setType(Material.GLASS_PANE); }
        buildSign(target, x + 3, y + 8, z - 1, label, "");
    }

    private static void buildTownWall(final World target, final int y) {
        for (int x = -150; x <= 150; x++) for (int h = 0; h < 9; h++) { target.getBlockAt(x, y + h, -124).setType(Material.STONE_BRICKS); target.getBlockAt(x, y + h, 70).setType(Material.STONE_BRICKS); }
        for (int z = -124; z <= 70; z++) for (int h = 0; h < 9; h++) { target.getBlockAt(-150, y + h, z).setType(Material.STONE_BRICKS); target.getBlockAt(150, y + h, z).setType(Material.STONE_BRICKS); }
        for (int x = -12; x <= 12; x++) for (int h = 0; h < 8; h++) { target.getBlockAt(x, y + h, -124).setType(Material.AIR); target.getBlockAt(x, y + h, 70).setType(Material.AIR); }
        for (int z = -124; z <= 70; z += 12) { target.getBlockAt(-150, y + 9, z).setType(Material.TORCH); target.getBlockAt(150, y + 9, z).setType(Material.TORCH); }
    }

    private static void buildGatehouse(final World target, final int x, final int y, final int z, final boolean south) {
        for (int side : new int[]{-1, 1}) for (int dx = 0; dx < 10; dx++) for (int dz = 0; dz < 9; dz++) for (int dy = 0; dy < 12; dy++) target.getBlockAt(x + side * (12 + dx), y + dy, z + (south ? -dz : dz)).setType(Material.STONE_BRICKS);
        for (int side : new int[]{-1, 1}) for (int dx = 0; dx < 7; dx++) for (int dz = 0; dz < 7; dz++) for (int dy = 1; dy < 10; dy++) target.getBlockAt(x + side * (15 + dx), y + dy, z + (south ? -dz : dz)).setType(Material.AIR);
        buildSign(target, x - 9, y + 10, z + (south ? -1 : 1), south ? "HAVEN" : "NORTH GATE", "");
    }

    private static void buildWatchtower(final World target, final int x, final int y, final int z) {
        for (int px = x - 4; px <= x + 4; px++) for (int pz = z - 4; pz <= z + 4; pz++) for (int py = y; py <= y + 12; py++) target.getBlockAt(px, py, pz).setType(Material.STONE_BRICKS);
        for (int px = x - 3; px <= x + 3; px++) for (int pz = z - 3; pz <= z + 3; pz++) for (int py = y + 1; py <= y + 10; py++) target.getBlockAt(px, py, pz).setType(Material.AIR);
        for (int px = x - 5; px <= x + 5; px++) for (int pz = z - 5; pz <= z + 5; pz++) target.getBlockAt(px, y + 13, pz).setType(Material.DEEPSLATE_TILES);
        target.getBlockAt(x, y + 14, z).setType(Material.LANTERN);
    }

    private static void buildFountain(final World target, final int x, final int y, final int z) {
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) target.getBlockAt(x + dx, y, z + dz).setType(Material.STONE_BRICKS);
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) target.getBlockAt(x + dx, y + 1, z + dz).setType(Material.WATER);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) target.getBlockAt(x + dx, y + 2, z + dz).setType(Material.QUARTZ_BLOCK);
        for (int dy = 2; dy <= 7; dy++) target.getBlockAt(x, y + dy, z).setType(Material.QUARTZ_BLOCK);
        target.getBlockAt(x, y + 8, z).setType(Material.WATER);
    }

    private static void buildMonument(final World target, final int x, final int y, final int z) {
        for (int dy = 0; dy < 10; dy++) target.getBlockAt(x, y + dy, z).setType(dy == 9 ? Material.GOLD_BLOCK : Material.QUARTZ_BLOCK);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) target.getBlockAt(x + dx, y, z + dz).setType(Material.POLISHED_DIORITE);
        target.getBlockAt(x, y + 5, z).setType(Material.LANTERN);
    }

    private static void buildLampPosts(final World target, final int x1, final int z1, final int x2, final int z2) {
        final int step = x2 >= x1 ? 10 : -10;
        for (int x = x1; x != x2 + step; x += step) { target.getBlockAt(x, terrainYAt(x, z1) + 1, z1).setType(Material.SPRUCE_FENCE); target.getBlockAt(x, terrainYAt(x, z1) + 2, z1).setType(Material.LANTERN); }
    }

    private static void buildMarketStalls(final World target, final int y) {
        final int[][] spots = {{-36,-56},{-12,-56},{14,-56},{40,-56},{-36,-43},{-12,-43},{14,-43},{40,-43}};
        for (final int[] p : spots) { for (int dx = -3; dx <= 3; dx++) { target.getBlockAt(p[0]+dx, y, p[1]-2).setType(Material.SPRUCE_PLANKS); target.getBlockAt(p[0]+dx, y+4, p[1]-2).setType(Material.WHITE_WOOL); target.getBlockAt(p[0]+dx, y+4, p[1]-1).setType(Material.RED_WOOL); } target.getBlockAt(p[0]-3, y, p[1]-2).setType(Material.SPRUCE_FENCE); target.getBlockAt(p[0]+3, y, p[1]-2).setType(Material.SPRUCE_FENCE); }
    }

    private static void buildFlowerBeds(final World target, final int y) {
        for (int x = -86; x <= 86; x += 14) { target.getBlockAt(x, y, -34).setType(Material.MOSS_BLOCK); target.getBlockAt(x+1, y, -34).setType(Material.MOSS_BLOCK); target.getBlockAt(x, y+1, -34).setType((x & 1) == 0 ? Material.POPPY : Material.BLUE_ORCHID); }
    }

    private static void buildStreetTrees(final World target, final int y) { for (int x = -118; x <= 118; x += 24) { buildTree(target, x, -115); buildTree(target, x, 58); } }

    private static void buildTree(final World target, final int x, final int z) {
        final int y = terrainYAt(x, z) + 1;
        for (int h = 0; h < 5; h++) target.getBlockAt(x, y+h, z).setType(Material.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 0; dy <= 2; dy++) if (Math.abs(dx)+Math.abs(dz)+dy < 5) target.getBlockAt(x+dx, y+4+dy, z+dz).setType(Material.OAK_LEAVES);
    }

    private static void buildDocks(final World target, final int y, final int x, final int z) {
        for (int dx = -18; dx <= 18; dx++) for (int dz = -5; dz <= 5; dz++) target.getBlockAt(x+dx, y, z+dz).setType(Material.SPRUCE_PLANKS);
        for (int dx = -16; dx <= 16; dx += 8) target.getBlockAt(x+dx, y+1, z).setType(Material.OAK_FENCE);
    }

    private static void buildFenceLine(final World target, final int x1, final int z1, final int x2, final int z2) {
        final int sx = Integer.signum(x2-x1), sz = Integer.signum(z2-z1); int x=x1,z=z1; while(true){ target.getBlockAt(x, terrainYAt(x,z)+1, z).setType(Material.SPRUCE_FENCE); if(x==x2&&z==z2)break; if(x!=x2)x+=sx; if(z!=z2)z+=sz; }
    }

    private void buildPath(final World target, int x1, int z1, final int x2, final int z2, final int radius, final Material material) {
        final int sx = Integer.signum(x2-x1), sz = Integer.signum(z2-z1); int steps=0; while(true){ final int y=terrainYAt(x1,z1); for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)target.getBlockAt(x1+dx,y,z1+dz).setType(material); if(x1==x2&&z1==z2)break; if(x1!=x2)x1+=sx; if(z1!=z2)z1+=sz; if(++steps>12000)break; }
    }

    private void buildNorthRoad(final World target, final int startZ, final int endZ) { buildPath(target, 0, startZ, 0, endZ, 5, Material.POLISHED_ANDESITE); for(int z=startZ;z<=endZ;z+=20){ final int y=terrainYAt(0,z)+1; target.getBlockAt(-8,y,z).setType(Material.LANTERN); target.getBlockAt(8,y,z).setType(Material.LANTERN); } }

    private boolean hasCurrentWorldBuild() { try { return Files.exists(this.buildMarker) && WORLD_VERSION.equals(Files.readString(this.buildMarker).trim()); } catch(IOException e){ return false; } }
    private void markProvisioned() { try { Files.createDirectories(this.buildMarker.getParent()); Files.writeString(this.buildMarker, WORLD_VERSION); } catch(IOException e){ throw new IllegalStateException("Failed to mark Floor 1 world", e); } }

    private void migrateLegacyWorld() {
        if (hasCurrentWorldBuild()) return;
        final World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) Bukkit.unloadWorld(existing, false);
        final Path worldDir = this.plugin.getServer().getWorldContainer().toPath().resolve(WORLD_NAME);
        if (!Files.exists(worldDir)) return;
        try (var stream = Files.walk(worldDir)) { stream.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch(IOException ignored) {} }); }
        catch(IOException exception){ this.plugin.getLogger().warning("Could not fully migrate old Floor 1 world: " + exception.getMessage()); }
    }

    private void ensureNpcs() {
        ensureNpc(NPC_LYRA, "§6✦ Warden Lyra", location(-26, 77, -88), Villager.Profession.ARMORER);
        ensureNpc(NPC_REN, "§a✦ Merchant Ren", location(42, 77, -88), Villager.Profession.FLETCHER);
        ensureNpc("ascension:innkeeper_mara", "§dInnkeeper Mara", location(-96, 77, -43), Villager.Profession.LIBRARIAN);
        ensureNpc("ascension:blacksmith_dain", "§cBlacksmith Dain", location(96, 77, -43), Villager.Profession.TOOLSMITH);
        ensureNpc("ascension:guide_elian", "§bGuide Elian", location(0, 77, 14), Villager.Profession.CARTOGRAPHER);
        ensureNpc("ascension:stablemaster_kael", "§eStablemaster Kael", location(96, 77, 10), Villager.Profession.FARMER);
    }

    private void ensureNpc(final String id, final String name, final Location loc, final Villager.Profession profession) {
        for (final Entity entity : this.world.getNearbyEntities(loc, 10, 6, 10)) if (id.equals(entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING))) return;
        final Villager villager = (Villager)this.world.spawnEntity(loc, EntityType.VILLAGER);
        villager.getPersistentDataContainer().set(this.npcKey, PersistentDataType.STRING, id);
        villager.setCustomName(name); villager.setCustomNameVisible(true); villager.setAI(false); villager.setInvulnerable(true); villager.setSilent(false); villager.setCollidable(false); villager.setPersistent(true); villager.setProfession(profession);
    }

    private void giveStarterEquipment(final Player player) { final var sword=createItem("ascension:rookie_sword"); final var ring=createItem("ascension:rookie_iron_ring"); if(sword!=null)player.getInventory().setItem(0,sword); if(ring!=null)player.getInventory().setItem(8,ring); player.getInventory().setHeldItemSlot(0); }
    private boolean hasRookieSword(final Player player) { for(var item:player.getInventory().getContents()) if(item!=null&&item.getType()==Material.IRON_SWORD&&item.hasItemMeta()&&item.getItemMeta().hasDisplayName()) return true; return false; }
    private org.bukkit.inventory.ItemStack createItem(final String rawId) { final AssetId id=AssetId.parse(rawId); final var definition=this.items.findDefinition(id).orElse(null); if(definition==null)return null; final Material material; try{material=Material.valueOf(definition.data().getString("material","WOODEN_SWORD").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){return null;} final var stack=new org.bukkit.inventory.ItemStack(material); final var meta=stack.getItemMeta(); if(meta!=null){meta.displayName(net.kyori.adventure.text.Component.text(definition.descriptor().displayName())); stack.setItemMeta(meta);} this.itemEncoder.encode(this.items.create(id),stack); return stack; }

    private Location spawnLocation() { return location(0, terrainYAt(0,-70)+1, -70); }
    private Location location(final double x, final double y, final double z) { return new Location(this.world,x+0.5,y,z+0.5); }
    private int terrainYAt(final int x, final int z) { return Math.max(63, terrainHeightApprox(x,z)); }
    private static int terrainHeightApprox(final int x, final int z) { final double n=Math.sin(x/310.0)*6.0+Math.sin(z/220.0)*5.0+Math.sin((x+z)/90.0)*2.0; final double river=Math.max(0,1.0-Math.abs(z-(260+Math.sin(x/310.0)*180))/110.0)*18.0; final double lake=Math.max(0,1.0-Math.sqrt((x+620.0)*(x+620.0)+(z-540.0)*(z-540.0))/110.0)*32.0; return (int)Math.round(72+n-river-lake); }

    private static void buildSign(final World world, final int x, final int y, final int z, final String line1, final String line2) { final var block=world.getBlockAt(x,y,z); block.setType(Material.OAK_SIGN); if(block.getState() instanceof Sign sign){sign.line(1,net.kyori.adventure.text.Component.text(line1)); if(!line2.isBlank())sign.line(2,net.kyori.adventure.text.Component.text(line2)); sign.update(true,false);} }
}
