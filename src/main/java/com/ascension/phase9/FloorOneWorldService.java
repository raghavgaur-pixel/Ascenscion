package com.ascension.phase9;

import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.model.AbilityResult;
import com.ascension.abilities.service.AbilityService;
import com.ascension.assets.model.AssetId;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.service.ItemService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Playable Floor 1 runtime with deterministic terrain, hostile mobs, NPCs, quests and boss progression. */
public final class FloorOneWorldService implements Listener {
    public static final String WORLD_NAME = "ascension_floor_001";
    private static final String WORLD_VERSION = "floor_001_world_v8";
    private static final int SURFACE_Y = FloorOneWorldGenerator.SURFACE_Y;
    private static final int GATE_Z = 760;
    private static final int BOSS_Z = 810;
    private static final int[] WOLF_X = {-42, 42, -78, 78, -118, 118, -52, 52, -138, 138};
    private static final int[] WOLF_Z = {145, 160, 200, 218, 255, 275, 320, 350, 395, 425};
    private static final int[] BEETLE_X = {-20, 20, -62, 62, -95, 95};
    private static final int[] BEETLE_Z = {180, 235, 290, 340, 410, 455};

    private final JavaPlugin plugin;
    private final QuestService quests;
    private final MobService mobs;
    private final AbilityService abilities;
    private final ItemService items;
    private final ItemMetadataEncoder itemEncoder;
    private final QuestMenuService journal;
    private final NamespacedKey npcKey;
    private final NamespacedKey handledDeathKey;
    private final Path buildMarker;
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private World world;
    private int aiTask = -1;

    public FloorOneWorldService(final JavaPlugin plugin, final QuestService quests, final MobService mobs,
                                final AbilityService abilities, final ItemService items, final ItemMetadataEncoder itemEncoder,
                                final QuestMenuService journal) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.quests = Objects.requireNonNull(quests, "quests");
        this.mobs = Objects.requireNonNull(mobs, "mobs");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.items = Objects.requireNonNull(items, "items");
        this.itemEncoder = Objects.requireNonNull(itemEncoder, "itemEncoder");
        this.journal = Objects.requireNonNull(journal, "journal");
        this.npcKey = new NamespacedKey(plugin, "npc_id");
        this.handledDeathKey = new NamespacedKey(plugin, "phase9_death_handled");
        this.buildMarker = plugin.getDataFolder().toPath().resolve(".floor_001_provisioned");
    }

    public void start() {
        shuttingDown.set(false);
        migrateLegacyWorld();
        world = loadWorld();
        configureWorld(world);
        if (!hasCurrentBuild()) {
            buildFloor(world);
            markBuild();
        }
        ensureNpcs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        aiTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickMobAI, 10L, 10L);
        plugin.getLogger().info("Ascension Floor 1 ready: " + WORLD_VERSION);
    }

    public void stop() {
        shuttingDown.set(true);
        if (aiTask != -1) Bukkit.getScheduler().cancelTask(aiTask);
        if (world == null) return;
        for (Entity entity : world.getEntities()) {
            if (mobs.mobId(entity).isPresent() || mobs.bossId(entity).isPresent()) {
                mobs.forget(entity.getUniqueId());
                entity.remove();
            }
        }
    }

    public void preparePlayer(final Player player) {
        if (world == null || !player.isOnline()) return;
        quests.grantStarterItems(player.getUniqueId());
        final Location spawn = havenSpawn();
        if (!player.getWorld().equals(world) || player.getLocation().distanceSquared(spawn) > 3600D) {
            player.teleport(spawn);
            player.setRespawnLocation(spawn, true);
            player.sendTitle("§6ASCENSION", "§fHaven · Floor 1", 10, 70, 20);
            player.sendMessage("§7You have arrived in §6Haven§7. Speak with §eWarden Lyra§7.");
        }
        ensureHuntMobs();
    }

    private void buildFloor(final World t) {
        buildHaven(t);
        buildWilderness(t);
        buildFirstGate(t);
        buildCitadel(t);
    }

    private void buildHaven(final World t) {
        fill(t, -150, 70, 150, 105, SURFACE_Y + 1, Material.COBBLESTONE);
        fill(t, -124, -10, 124, 34, SURFACE_Y + 2, Material.STONE_BRICKS);
        buildPath(t, 0, -68, 0, GATE_Z - 70, 5, Material.POLISHED_ANDESITE);
        buildPath(t, -120, -22, 120, -22, 4, Material.STONE_BRICKS);
        buildPath(t, -82, 22, 82, 22, 4, Material.POLISHED_ANDESITE);
        buildTownBuilding(t, -80, -62, 58, 31, "WARDEN HALL", Material.STONE_BRICKS, Material.DEEPSLATE_TILES);
        buildTownBuilding(t, 22, -62, 54, 31, "REN'S MARKET", Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS);
        buildTownBuilding(t, -126, -12, 36, 28, "HAVEN INN", Material.OAK_PLANKS, Material.SPRUCE_PLANKS);
        buildTownBuilding(t, 90, -12, 36, 28, "BLACKSMITH", Material.STONE_BRICKS, Material.POLISHED_DEEPSLATE);
        buildTownBuilding(t, -126, 25, 36, 28, "GUILD HALL", Material.DARK_OAK_PLANKS, Material.DEEPSLATE_TILES);
        buildTownBuilding(t, 90, 25, 36, 28, "STABLES", Material.OAK_PLANKS, Material.OAK_SLAB);
        buildFountain(t, 0, -28);
        buildGatehouse(t, 0, 68, false);
        buildLanterns(t);
        buildSign(t, 0, 87, -69, "HAVEN", "Safe settlement · Floor 1");
    }

    private void buildWilderness(final World t) {
        for (int z = 105; z <= 500; z += 18) buildPath(t, 0, z, 0, z + 10, 3, Material.COARSE_DIRT);
        buildRiver(t, 0, 315);
        for (int[] camp : new int[][] {{-72,175},{72,245},{-88,365},{88,455}}) buildCamp(t, camp[0], camp[1]);
        buildRuins(t, -30, 560);
        buildRuins(t, 35, 635);
        buildSign(t, 0, SURFACE_Y + 4, 130, "WHISPERING WOODS", "First Hunt grounds");
        buildSign(t, 0, SURFACE_Y + 4, 550, "THE OLD ROAD", "Ruins of a forgotten ascent");
        buildPath(t, 0, 500, 0, GATE_Z + 15, 4, Material.DEEPSLATE_BRICKS);
    }

    private void buildFirstGate(final World t) {
        final int base = SURFACE_Y + 1;
        buildPath(t, 0, GATE_Z - 85, 0, GATE_Z + 22, 7, Material.POLISHED_DEEPSLATE);
        buildTower(t, -42, GATE_Z - 10, base);
        buildTower(t, 42, GATE_Z - 10, base);
        for (int x = -38; x <= 38; x++) {
            for (int h = 0; h <= 22; h++) {
                if (Math.abs(x) <= 8 && h <= 11) continue;
                t.getBlockAt(x, base + h, GATE_Z).setType(h % 5 == 0 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS);
            }
        }
        for (int x = -8; x <= 8; x++) for (int h = 12; h <= 22; h++) t.getBlockAt(x, base + h, GATE_Z).setType(Material.IRON_BARS);
        buildSign(t, 0, base + 25, GATE_Z - 3, "THE FIRST GATE", "Warden of the First Gate");
    }

    private void buildCitadel(final World t) {
        buildTower(t, 0, 960, SURFACE_Y + 1);
        buildTower(t, -65, 980, SURFACE_Y + 1);
        buildTower(t, 65, 980, SURFACE_Y + 1);
        buildSign(t, 0, SURFACE_Y + 22, 960, "THE ASCENSION CITADEL", "The Tower remembers the worthy");
    }

    private void buildTownBuilding(final World t, final int x, final int z, final int width, final int depth, final String label, final Material wall, final Material roof) {
        final int y = SURFACE_Y + 1;
        for (int px = x; px < x + width; px++) for (int pz = z; pz < z + depth; pz++) t.getBlockAt(px, y, pz).setType(wall);
        for (int px = x; px < x + width; px++) for (int h = 1; h <= 7; h++) { t.getBlockAt(px, y + h, z).setType(Material.OAK_LOG); t.getBlockAt(px, y + h, z + depth - 1).setType(Material.OAK_LOG); }
        for (int pz = z; pz < z + depth; pz++) for (int h = 1; h <= 7; h++) { t.getBlockAt(x, y + h, pz).setType(Material.OAK_LOG); t.getBlockAt(x + width - 1, y + h, pz).setType(Material.OAK_LOG); }
        for (int px = x + 1; px < x + width - 1; px++) for (int pz = z + 1; pz < z + depth - 1; pz++) for (int h = 1; h <= 6; h++) t.getBlockAt(px, y + h, pz).setType(Material.AIR);
        for (int layer = 0; layer < 3; layer++) for (int px = x + layer; px < x + width - layer; px++) for (int pz = z + layer; pz < z + depth - layer; pz++) t.getBlockAt(px, y + 8 + layer, pz).setType(roof);
        t.getBlockAt(x + width / 2, y + 1, z).setType(Material.AIR);
        t.getBlockAt(x + width / 2, y + 2, z).setType(Material.AIR);
        buildSign(t, x + 2, y + 10, z - 1, label, "");
    }

    private void buildFountain(final World t, final int x, final int z) {
        for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) if (dx * dx + dz * dz <= 100) t.getBlockAt(x + dx, SURFACE_Y + 2, z + dz).setType(Material.STONE_BRICKS);
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) t.getBlockAt(x + dx, SURFACE_Y + 3, z + dz).setType(Material.WATER);
        for (int h = 4; h <= 8; h++) t.getBlockAt(x, SURFACE_Y + h, z).setType(Material.QUARTZ_BLOCK);
    }

    private void buildGatehouse(final World t, final int x, final int z, final boolean south) {
        for (int s : new int[] {-1, 1}) for (int dx = 0; dx < 11; dx++) for (int dz = 0; dz < 9; dz++) for (int h = 0; h < 12; h++) t.getBlockAt(x + s * (13 + dx), SURFACE_Y + 1 + h, z + (south ? -dz : dz)).setType(Material.STONE_BRICKS);
    }

    private void buildTower(final World t, final int x, final int z, final int base) {
        for (int px = x - 5; px <= x + 5; px++) for (int pz = z - 5; pz <= z + 5; pz++) for (int h = 0; h <= 13; h++) t.getBlockAt(px, base + h, pz).setType(Material.STONE_BRICKS);
        for (int px = x - 3; px <= x + 3; px++) for (int pz = z - 3; pz <= z + 3; pz++) for (int h = 1; h <= 11; h++) t.getBlockAt(px, base + h, pz).setType(Material.AIR);
        for (int r = 0; r <= 5; r++) for (int px = x - r; px <= x + r; px++) for (int pz = z - r; pz <= z + r; pz++) t.getBlockAt(px, base + 14 + r, pz).setType(Material.DEEPSLATE_TILES);
        t.getBlockAt(x, base + 20, z).setType(Material.LANTERN);
    }

    private void buildCamp(final World t, final int x, final int z) {
        for (int dx = -5; dx <= 5; dx++) for (int dz = -4; dz <= 4; dz++) t.getBlockAt(x + dx, SURFACE_Y + 1, z + dz).setType(Material.COARSE_DIRT);
        t.getBlockAt(x, SURFACE_Y + 2, z).setType(Material.CAMPFIRE);
        t.getBlockAt(x + 4, SURFACE_Y + 1, z).setType(Material.CHEST);
    }

    private void buildRiver(final World t, final int x, final int z) {
        for (int dx = -520; dx <= 520; dx++) for (int dz = -8; dz <= 8; dz++) {
            t.getBlockAt(x + dx, SURFACE_Y, z + dz).setType(Material.SAND);
            t.getBlockAt(x + dx, SURFACE_Y + 1, z + dz).setType(Material.WATER);
            t.getBlockAt(x + dx, SURFACE_Y + 2, z + dz).setType(Material.WATER);
        }
        buildPath(t, x - 18, z, x + 18, z, 3, Material.SPRUCE_PLANKS);
    }

    private void buildRuins(final World t, final int x, final int z) {
        final int y = SURFACE_Y + 1;
        for (int i = -4; i <= 4; i++) for (int h = 0; h < 6 + (Math.abs(i) % 3); h++) t.getBlockAt(x + i * 9, y + h, z).setType(h % 3 == 0 ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS);
    }

    private void buildLanterns(final World t) {
        for (int z = -58; z <= 58; z += 14) {
            t.getBlockAt(-8, SURFACE_Y + 2, z).setType(Material.SPRUCE_FENCE);
            t.getBlockAt(-8, SURFACE_Y + 3, z).setType(Material.LANTERN);
            t.getBlockAt(8, SURFACE_Y + 2, z).setType(Material.SPRUCE_FENCE);
            t.getBlockAt(8, SURFACE_Y + 3, z).setType(Material.LANTERN);
        }
    }

    private void buildSign(final World t, final int x, final int y, final int z, final String title, final String sub) {
        t.getBlockAt(x, y, z).setType(Material.OAK_SIGN);
        if (t.getBlockAt(x, y, z).getState() instanceof org.bukkit.block.Sign sign) {
            sign.line(0, net.kyori.adventure.text.Component.text(title));
            sign.line(1, net.kyori.adventure.text.Component.text(sub));
            sign.update();
        }
    }

    private void buildPath(final World t, final int x1, final int z1, final int x2, final int z2, final int radius, final Material material) {
        int x = x1, z = z1;
        while (true) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) t.getBlockAt(x + dx, SURFACE_Y + 1, z + dz).setType(material);
            if (x == x2 && z == z2) break;
            if (x != x2) x += Integer.signum(x2 - x);
            if (z != z2) z += Integer.signum(z2 - z);
        }
    }

    private void fill(final World t, final int x1, final int z1, final int x2, final int z2, final int y, final Material material) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) t.getBlockAt(x, y, z).setType(material);
    }

    private void ensureNpcs() {
        ensureNpc(QuestService.LYRA, "§6✦ Warden Lyra", location(-20, SURFACE_Y + 2, -45), Villager.Profession.ARMORER);
        ensureNpc(QuestService.NPC_REN, "§a✦ Merchant Ren", location(48, SURFACE_Y + 2, -45), Villager.Profession.FLETCHER);
        ensureNpc("ascension:innkeeper_mara", "§dInnkeeper Mara", location(-108, SURFACE_Y + 2, -2), Villager.Profession.LIBRARIAN);
        ensureNpc("ascension:blacksmith_dain", "§cBlacksmith Dain", location(108, SURFACE_Y + 2, -2), Villager.Profession.TOOLSMITH);
        ensureNpc("ascension:guide_elian", "§bGuide Elian", location(0, SURFACE_Y + 2, 22), Villager.Profession.CARTOGRAPHER);
        ensureNpc("ascension:stablemaster_kael", "§eStablemaster Kael", location(108, SURFACE_Y + 2, 30), Villager.Profession.FARMER);
        ensureNpc("ascension:captain_soren", "§9Captain Soren", location(0, SURFACE_Y + 2, 55), Villager.Profession.LIBRARIAN);
    }

    private void ensureNpc(final String id, final String name, final Location location, final Villager.Profession profession) {
        for (Entity e : world.getNearbyEntities(location, 8, 6, 8)) if (id.equals(e.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING))) return;
        final Villager villager = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
        villager.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id);
        villager.setCustomName(name);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setPersistent(true);
        villager.setProfession(profession);
    }

    private void ensureHuntMobs() {
        if (world == null) return;
        int wolves = 0, beetles = 0;
        for (Entity e : world.getEntities()) {
            final String id = mobs.mobId(e).map(AssetId::toString).orElse("");
            if (QuestService.WOLF.equals(id) && !e.isDead()) wolves++;
            if (QuestService.BEETLE.equals(id) && !e.isDead()) beetles++;
        }
        for (int i = wolves; i < WOLF_X.length; i++) spawnAuthoredMob(QuestService.WOLF, WOLF_X[i], WOLF_Z[i]);
        for (int i = beetles; i < BEETLE_X.length; i++) spawnAuthoredMob(QuestService.BEETLE, BEETLE_X[i], BEETLE_Z[i]);
    }

    private void ensureBoss() {
        if (world == null) return;
        for (Entity e : world.getEntities()) if (QuestService.BOSS.equals(mobs.bossId(e).map(AssetId::toString).orElse("")) && !e.isDead()) return;
        mobs.spawnBoss(AssetId.parse(QuestService.BOSS), location(0, SURFACE_Y + 1, BOSS_Z)).ifPresent(b -> {
            b.setGlowing(true);
            b.setPersistent(true);
            b.setRemoveWhenFarAway(false);
            b.setCustomNameVisible(true);
        });
    }

    private void spawnAuthoredMob(final String id, final int x, final int z) {
        mobs.spawnMob(AssetId.parse(id), location(x, SURFACE_Y + 1, z)).ifPresent(entity -> {
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            if (entity instanceof Wolf wolf) {
                wolf.setAngry(true);
                wolf.setCollarColor(org.bukkit.DyeColor.GRAY);
            }
        });
    }

    private void tickMobAI() {
        if (shuttingDown.get() || world == null) return;
        for (Entity entity : world.getEntities()) {
            final String id = mobs.mobId(entity).map(AssetId::toString).orElse("");
            if (!QuestService.WOLF.equals(id) && !QuestService.BEETLE.equals(id)) continue;
            if (!(entity instanceof Mob mob) || entity.isDead()) continue;
            Player nearest = null;
            double best = 26D * 26D;
            for (Player player : world.getPlayers()) {
                if (!player.isOnline() || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
                final double distance = player.getLocation().distanceSquared(entity.getLocation());
                if (distance < best) { best = distance; nearest = player; }
            }
            if (nearest != null) {
                mob.setTarget(nearest);
                if (mob instanceof Wolf wolf) wolf.setAngry(true);
            } else mob.setTarget(null);
        }
    }

    @EventHandler public void onJoin(final PlayerJoinEvent event) { Bukkit.getScheduler().runTaskLater(plugin, () -> preparePlayer(event.getPlayer()), 40L); }

    @EventHandler public void onNpcInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity) || world == null || !world.equals(entity.getWorld())) return;
        final String id = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
        if (id == null) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        switch (id) {
            case QuestService.LYRA -> {
                final QuestService.Result result = quests.talkToNpc(player.getUniqueId(), QuestService.LYRA);
                player.sendMessage("§6Warden Lyra§f: Welcome to Haven, climber.");
                if (result.status() == QuestService.Status.SUCCESS || quests.isCompleted(player.getUniqueId(), QuestService.ARRIVAL)) player.sendMessage("§6Warden Lyra§f: Hunt §e5 Forest Wolves§f and §e3 Iron Beetles§f in the Whispering Woods.");
                player.sendMessage("§7Your §bAscension Journal §7has been updated.");
                journal.open(player);
                ensureHuntMobs();
            }
            case QuestService.NPC_REN -> { player.sendMessage("§aMerchant Ren§f: Better gear awaits above."); journal.open(player); }
            case "ascension:innkeeper_mara" -> player.sendMessage("§dInnkeeper Mara§f: Haven is safe. The road north is not.");
            case "ascension:blacksmith_dain" -> player.sendMessage("§cBlacksmith Dain§f: Your Rookie Sword is only your beginning.");
            case "ascension:guide_elian" -> { player.sendMessage("§bGuide Elian§f: Follow the northern road. River. Ruins. First Gate."); journal.open(player); }
            case "ascension:stablemaster_kael" -> player.sendMessage("§eStablemaster Kael§f: Keep your bearings once the woods close around you.");
            case "ascension:captain_soren" -> player.sendMessage("§9Captain Soren§f: The First Gate is the first true test.");
            default -> player.sendMessage("§7The resident nods.");
        }
    }

    @EventHandler public void onCombat(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (shuttingDown.get() || world == null || !world.equals(target.getWorld())) return;
        if (mobs.mobId(target).isEmpty() && mobs.bossId(target).isEmpty()) return;
        final AbilityResult result = player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD
            ? abilities.execute(new AbilityRequest(player.getUniqueId(), "ascension:quick_strike", target.getUniqueId())) : AbilityResult.rejected("Equip your Rookie Sword.");
        if (result.status() == AbilityResult.Status.SUCCESS) {
            event.setCancelled(true);
            target.playEffect(EntityEffect.HURT);
            player.sendActionBar("§c✦ Quick Strike");
        } else if (player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) {
            event.setCancelled(true);
            player.sendActionBar("§eEquip your Rookie Sword.");
        }
    }

    @EventHandler public void onTarget(final EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getTarget() instanceof Player player) || world == null || !world.equals(mob.getWorld())) return;
        final String id = mobs.mobId(mob).map(AssetId::toString).orElse("");
        if ((QuestService.WOLF.equals(id) || QuestService.BEETLE.equals(id)) && player.getGameMode() == GameMode.SPECTATOR) event.setCancelled(true);
    }

    @EventHandler public void onDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        if (world == null || !world.equals(entity.getWorld())) return;
        final String mobId = mobs.mobId(entity).map(AssetId::toString).orElse(null);
        final String bossId = mobs.bossId(entity).map(AssetId::toString).orElse(null);
        if (mobId == null && bossId == null) return;
        mobs.forget(entity.getUniqueId());
        final Player killer = entity.getKiller();
        if (killer != null && !entity.getPersistentDataContainer().has(handledDeathKey, PersistentDataType.BYTE)) {
            entity.getPersistentDataContainer().set(handledDeathKey, PersistentDataType.BYTE, (byte) 1);
            if (mobId != null) quests.grantKillRewards(killer.getUniqueId(), AssetId.parse(mobId), false);
            if (bossId != null) quests.grantKillRewards(killer.getUniqueId(), AssetId.parse(bossId), true);
            quests.defeat(killer.getUniqueId(), mobId, bossId);
        }
    }

    @EventHandler public void onMove(final PlayerMoveEvent event) {
        if (world == null || !world.equals(event.getPlayer().getWorld())) return;
        final Player player = event.getPlayer();
        if (player.getLocation().getZ() < GATE_Z || !quests.active(player.getUniqueId()).contains(QuestService.FIRST_GATE)) return;
        if (quests.reachLocation(player.getUniqueId(), "first_gate").status() == QuestService.Status.SUCCESS) {
            player.sendTitle("§cTHE FIRST GATE", "§7The guardian awakens", 5, 60, 15);
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
        target.setGameRule(GameRule.DO_FIRE_TICK, false);
        target.getWorldBorder().setCenter(0, 300);
        target.getWorldBorder().setSize(2000);
        target.setSpawnLocation(0, SURFACE_Y + 2, -45);
    }

    private Location havenSpawn() { return location(0, SURFACE_Y + 2, -45); }
    private Location location(final int x, final int y, final int z) { return new Location(world, x, y, z); }
    private boolean hasCurrentBuild() { try { return Files.exists(buildMarker) && WORLD_VERSION.equals(Files.readString(buildMarker).trim()); } catch (IOException exception) { return false; } }
    private void markBuild() { try { Files.createDirectories(buildMarker.getParent()); Files.writeString(buildMarker, WORLD_VERSION); } catch (IOException exception) { throw new IllegalStateException("Could not mark Floor 1 build", exception); } }

    private void migrateLegacyWorld() {
        if (hasCurrentBuild()) return;
        final World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) Bukkit.unloadWorld(existing, false);
        final Path dir = plugin.getServer().getWorldContainer().toPath().resolve(WORLD_NAME);
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not fully migrate Floor 1 world: " + exception.getMessage());
        }
    }
}
