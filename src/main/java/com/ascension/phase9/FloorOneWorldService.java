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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.EntityEffect;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
import org.bukkit.plugin.java.JavaPlugin;

/** Authored Floor 1 world: dense Haven, living wilderness, ruins, river crossing, monumental First Gate. */
public final class FloorOneWorldService implements Listener {
    public static final String WORLD_NAME = "ascension_floor_001";
    private static final String WORLD_VERSION = "floor_001_world_v7";
    private static final int GATE_Z = 760;
    private static final int BOSS_Z = 808;
    private final JavaPlugin plugin;
    private final QuestService quests;
    private final MobService mobs;
    private final AbilityService abilities;
    private final ItemService items;
    private final ItemMetadataEncoder itemEncoder;
    private final QuestMenuService journal;
    private final NamespacedKey npcKey;
    private final Path buildMarker;
    private final Map<UUID, Integer> dialogue = new HashMap<>();
    private World world;
    private boolean shuttingDown;

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
        this.buildMarker = plugin.getDataFolder().toPath().resolve(".floor_001_provisioned");
    }

    public void start() {
        this.shuttingDown = false;
        migrateLegacyWorld();
        this.world = loadWorld();
        configureWorld(this.world);
        if (!hasCurrentBuild()) {
            buildFloor(this.world);
            markBuild();
        }
        ensureNpcs();
        register();
        plugin.getLogger().info("Ascension Floor 1 loaded: authored Haven + Wilderness + First Gate / " + WORLD_VERSION);
    }

    public void stop() {
        this.shuttingDown = true;
        if (this.world == null) return;
        for (Entity e : this.world.getEntities()) {
            if (mobs.mobId(e).isPresent() || mobs.bossId(e).isPresent()) { mobs.forget(e.getUniqueId()); e.remove(); }
        }
    }

    public void preparePlayer(final Player player) {
        if (world == null || !player.isOnline()) return;
        quests.grantStarterItems(player.getUniqueId());
        final Location spawn = havenSpawn();
        if (!player.getWorld().equals(world) || player.getLocation().distanceSquared(spawn) > 250000) {
            player.teleport(spawn);
            player.setRespawnLocation(spawn, true);
            player.sendTitle("§6ASCENSION", "§fHaven · Floor 1", 10, 70, 20);
            player.sendMessage("§8You stand beneath the Tower at §6Haven§8. The northern road leads toward the First Gate.");
        }
        ensureHuntMobs();
    }

    public void ensureHuntMobs() {
        if (world == null) return;
        int wolves = 0, beetles = 0;
        for (Entity e : world.getEntities()) {
            if (mobs.mobId(e).map(AssetId::toString).orElse("").equals(QuestService.WOLF) && !e.isDead()) wolves++;
            if (mobs.mobId(e).map(AssetId::toString).orElse("").equals(QuestService.BEETLE) && !e.isDead()) beetles++;
        }
        int[][] wolf = {{-34,132},{35,150},{-72,188},{76,205},{-112,248},{108,270},{-48,318},{58,345},{-130,372},{125,400}};
        int[][] beetle = {{18,170},{-18,228},{45,292},{-55,350},{-82,420},{82,448}};
        for (int i = wolves; i < wolf.length; i++) spawnAuthoredMob(QuestService.WOLF, wolf[i][0], wolf[i][1]);
        for (int i = beetles; i < beetle.length; i++) spawnAuthoredMob(QuestService.BEETLE, beetle[i][0], beetle[i][1]);
    }

    public void ensureBoss() {
        if (world == null) return;
        for (Entity e : world.getEntities()) if (mobs.bossId(e).map(AssetId::toString).orElse("").equals(QuestService.BOSS) && !e.isDead()) return;
        Location loc = location(0, terrainYAt(0, BOSS_Z) + 1, BOSS_Z);
        mobs.spawnBoss(AssetId.parse(QuestService.BOSS), loc).ifPresent(boss -> {
            boss.setGlowing(true); boss.setPersistent(true); boss.setRemoveWhenFarAway(false); boss.setCustomNameVisible(true);
            boss.playEffect(EntityEffect.HURT);
        });
    }

    @EventHandler
    public void onNpcInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity) || world == null || !entity.getWorld().equals(world)) return;
        final String id = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
        if (id == null) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        final String key = player.getUniqueId() + ":" + id;
        final UUID keyUuid = UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final int stage = dialogue.merge(keyUuid, 1, Integer::sum) - 1;
        switch (id) {
            case QuestService.LYRA -> {
                final QuestService.Result result = quests.talkToNpc(player.getUniqueId(), QuestService.LYRA);
                if (stage == 0) {
                    player.sendMessage("§6Warden Lyra§f: So, the Tower finally chose another climber.");
                    player.sendMessage("§6Warden Lyra§f: Welcome to Haven. Beyond our walls lies the First Field.");
                    player.sendMessage("§6Warden Lyra§f: Take this blade. Learn to fight, then earn your way to the First Gate.");
                } else if (stage == 1) {
                    player.sendMessage("§6Warden Lyra§f: The woods north of the river are thick with beasts.");
                    player.sendMessage("§6Warden Lyra§f: Hunt §e5 Forest Wolves§f and §e3 Iron Beetles§f. Your journal tracks them.");
                } else {
                    player.sendMessage("§6Warden Lyra§f: Haven has survived because climbers respect the road and the road respects no one.");
                }
                if (result.status() == QuestService.Status.REJECTED) player.sendMessage("§7Your quest record is already complete. Keep climbing.");
                journal.open(player);
            }
            case QuestService.NPC_REN -> {
                if (stage % 2 == 0) {
                    player.sendMessage("§aMerchant Ren§f: Fresh from the Tower? Then you will need supplies.");
                    player.sendMessage("§aMerchant Ren§f: Bring trophies from the First Field. The market pays for useful work.");
                } else player.sendMessage("§aMerchant Ren§f: Keep the Rookie Sword close. Better steel is waiting further up the Tower.");
                journal.open(player);
            }
            case "ascension:innkeeper_mara" -> player.sendMessage("§dInnkeeper Mara§f: Rooms are warm, beds are clean, and stories are always welcome here.");
            case "ascension:blacksmith_dain" -> player.sendMessage("§cBlacksmith Dain§f: Every great weapon starts as a piece of ordinary metal. Yours is no exception.");
            case "ascension:guide_elian" -> { player.sendMessage("§bGuide Elian§f: Follow the north road. Cross the old river bridge. Ignore the ruins unless you have steel."); journal.open(player); }
            case "ascension:stablemaster_kael" -> player.sendMessage("§eStablemaster Kael§f: The valley beyond the woods is wide enough to lose a careless traveler.");
            case "ascension:captain_soren" -> player.sendMessage("§9Captain Soren§f: The First Gate is the first place the Tower stops forgiving mistakes.");
            default -> player.sendMessage("§7The resident nods, clearly familiar with the climb.");
        }
    }

    @EventHandler
    public void onCombat(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (shuttingDown || world == null || !target.getWorld().equals(world)) return;
        if (mobs.mobId(target).isEmpty() && mobs.bossId(target).isEmpty()) return;
        event.setCancelled(true);
        if (player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) { player.sendActionBar("§eEquip your Rookie Sword."); return; }
        final var result = abilities.execute(new AbilityRequest(player.getUniqueId(), "ascension:quick_strike", target.getUniqueId()));
        if (result.status() == com.ascension.abilities.model.AbilityResult.Status.SUCCESS) { target.playEffect(EntityEffect.HURT); player.sendActionBar("§c✦ Quick Strike"); }
    }

    @EventHandler public void onTarget(final EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player) || world == null || !event.getEntity().getWorld().equals(world)) return;
        if (mobs.mobId(event.getEntity()).isPresent()) return;
    }

    @EventHandler public void onDeath(final EntityDeathEvent event) {
        if (world == null || !event.getEntity().getWorld().equals(world)) return;
        if (mobs.mobId(event.getEntity()).isPresent() || mobs.bossId(event.getEntity()).isPresent()) mobs.forget(event.getEntity().getUniqueId());
    }

    @EventHandler public void onMove(final PlayerMoveEvent event) {
        if (world == null || !event.getPlayer().getWorld().equals(world)) return;
        if (event.getPlayer().getLocation().getZ() < GATE_Z) return;
        if (!quests.active(event.getPlayer().getUniqueId()).contains(QuestService.FIRST_GATE)) return;
        if (quests.reachLocation(event.getPlayer().getUniqueId(), "first_gate").status() == QuestService.Status.SUCCESS) {
            event.getPlayer().sendTitle("§cTHE FIRST GATE", "§7The guardian awakens", 5, 60, 15);
            ensureBoss();
        }
    }

    private void register() { plugin.getServer().getPluginManager().registerEvents(this, plugin); }

    private World loadWorld() {
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) return existing;
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(new FloorOneWorldGenerator());
        return creator.createWorld();
    }

    private void configureWorld(final World target) {
        target.setDifficulty(Difficulty.NORMAL);
        target.setStorm(false); target.setThundering(false); target.setTime(1000L);
        target.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); target.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        target.setGameRule(GameRule.DO_MOB_SPAWNING, false); target.setGameRule(GameRule.DO_FIRE_TICK, false);
        target.getWorldBorder().setCenter(0, 0); target.getWorldBorder().setSize(10000);
        target.setSpawnLocation(0, terrainYAt(0, -90) + 2, -90);
    }

    private void buildFloor(final World target) {
        buildHaven(target);
        buildNorthRoad(target);
        buildWilderness(target);
        buildFirstGate(target);
        buildCitadel(target);
    }

    private void buildHaven(final World target) {
        // Natural grass is deliberately left exposed. Only streets, plazas and structures are authored.
        buildCobbleSquare(target, 0, -38, 82);
        buildPath(target, 0, -128, 0, GATE_Z - 80, 6, Material.POLISHED_ANDESITE);
        buildPath(target, -125, -62, 125, -62, 6, Material.STONE_BRICKS);
        buildPath(target, -105, 8, 105, 8, 4, Material.STONE_BRICKS);
        buildPath(target, -95, 52, 95, 52, 4, Material.POLISHED_ANDESITE);
        buildFountain(target, 0, -46);
        buildGreatHall(target, -62, -106);
        buildBuilding(target, 16, -106, 45, 32, "REN'S MARKET", Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.SPRUCE_LOG);
        buildBuilding(target, -122, -50, 34, 28, "HAVEN INN", Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.OAK_LOG);
        buildBuilding(target, 88, -50, 34, 28, "BLACKSMITH", Material.STONE_BRICKS, Material.DEEPSLATE_TILES, Material.DEEPSLATE);
        buildBuilding(target, -122, 12, 34, 28, "GUILD HALL", Material.DARK_OAK_PLANKS, Material.DEEPSLATE_TILES, Material.DARK_OAK_LOG);
        buildBuilding(target, 88, 12, 34, 28, "STABLES", Material.OAK_PLANKS, Material.OAK_SLAB, Material.OAK_LOG);
        buildBuilding(target, -54, 42, 36, 24, "FLETCHER'S YARD", Material.SPRUCE_PLANKS, Material.SPRUCE_SLAB, Material.SPRUCE_LOG);
        buildBuilding(target, 12, 42, 36, 24, "FARMWARD", Material.OAK_PLANKS, Material.OAK_SLAB, Material.OAK_LOG);
        buildBuilding(target, 50, -12, 38, 28, "TOWER ARCHIVE", Material.STONE_BRICKS, Material.POLISHED_DEEPSLATE, Material.OAK_LOG);
        buildBuilding(target, -88, -12, 30, 28, "HERBALIST", Material.MOSS_BLOCK, Material.OAK_SLAB, Material.OAK_LOG);
        buildTownWall(target);
        buildGatehouse(target, 0, -132, true);
        buildGatehouse(target, 0, 76, false);
        buildWatchtower(target, -150, -127); buildWatchtower(target, 150, -127);
        buildWatchtower(target, -150, 70); buildWatchtower(target, 150, 70);
        buildMarketStalls(target);
        buildStreetTrees(target);
        buildHavenLanterns(target);
        buildSign(target, 0, 78, -92, "HAVEN", "Safe ground beneath the Tower");
        buildSign(target, 0, 78, 92, "NORTHERN ROAD", "Whispering Woods · 700 blocks");
    }

    private void buildGreatHall(final World target, final int x, final int z) {
        buildBuilding(target, x, 75, z, 76, 34, "WARDEN HALL", Material.STONE_BRICKS, Material.DEEPSLATE_TILES, Material.OAK_LOG);
        for (int side : new int[]{-1, 1}) {
            int tx = x + (side < 0 ? -8 : 76);
            buildWatchtower(target, tx, z + 1);
            buildSpire(target, tx, 92, z + 14);
        }
        buildSpire(target, x + 38, 96, z + 16);
    }

    private void buildNorthRoad(final World target) {
        for (int z = 110; z < GATE_Z - 60; z += 24) {
            int y = terrainYAt(0, z) + 1;
            target.getBlockAt(-10, y, z).setType(Material.SPRUCE_FENCE); target.getBlockAt(-10, y + 1, z).setType(Material.LANTERN);
            target.getBlockAt(10, y, z).setType(Material.SPRUCE_FENCE); target.getBlockAt(10, y + 1, z).setType(Material.LANTERN);
        }
    }

    private void buildWilderness(final World target) {
        int[][] camps = {{-55,180},{65,255},{-78,360},{80,470}};
        for (int[] p : camps) buildForestCamp(target, p[0], p[1]);
        buildRiverBridge(target, 0, 315);
        buildRuins(target, -28, 565);
        buildRuins(target, 35, 635);
        buildWatchtower(target, -50, 500); buildWatchtower(target, 50, 540);
        buildSign(target, 0, terrainYAt(0, 155) + 5, 155, "WHISPERING WOODS", "Hunt grounds");
        buildSign(target, 0, terrainYAt(0, 565) + 7, 565, "THE OLD ROAD", "Ruins from before Haven");
    }

    private void buildFirstGate(final World target) {
        final int y = terrainYAt(0, GATE_Z);
        buildPath(target, 0, GATE_Z - 90, 0, GATE_Z + 16, 8, Material.POLISHED_DEEPSLATE);
        for (int side : new int[]{-1, 1}) {
            int x = side * 50;
            buildWatchtower(target, x, GATE_Z - 20);
            buildSpire(target, x, y + 1, GATE_Z - 20);
        }
        for (int x = -38; x <= 38; x++) for (int h = 1; h <= 23; h++) {
            if (Math.abs(x) <= 9 && h <= 12) continue;
            target.getBlockAt(x, y + h, GATE_Z).setType(h % 6 == 0 ? Material.POLISHED_DEEPSLATE : Material.DEEPSLATE_BRICKS);
        }
        for (int x = -9; x <= 9; x++) for (int h = 13; h <= 23; h++) target.getBlockAt(x, y + h, GATE_Z).setType(Material.IRON_BARS);
        for (int x = -9; x <= 9; x += 3) target.getBlockAt(x, y + 25, GATE_Z).setType(Material.SOUL_LANTERN);
        for (int x = -70; x <= 70; x += 14) { target.getBlockAt(x, y + 1, GATE_Z - 18).setType(Material.BLACKSTONE); target.getBlockAt(x, y + 2, GATE_Z - 18).setType(Material.SOUL_LANTERN); }
        buildSign(target, 0, y + 27, GATE_Z - 4, "THE FIRST GATE", "Warden of the First Gate");
    }

    private void buildCitadel(final World target) {
        int baseZ = 960; int y = terrainYAt(0, baseZ) + 1;
        buildSpire(target, 0, y, baseZ);
        for (int x : new int[]{-70,70}) buildSpire(target, x, y, baseZ + 10);
        for (int z = baseZ - 30; z <= baseZ + 30; z += 15) buildPath(target, -70, z, 70, z, 2, Material.STONE_BRICKS);
        buildSign(target, 0, y + 20, baseZ - 5, "THE ASCENSION CITADEL", "The Tower remembers the worthy");
    }

    private void buildCobbleSquare(final World target, final int cx, final int cz, final int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d <= radius) target.getBlockAt(cx + dx, 73, cz + dz).setType(d > radius - 4 ? Material.STONE_BRICKS : Material.COBBLESTONE);
        }
    }

    private void buildTownWall(final World target) {
        for (int x = -158; x <= 158; x++) for (int h = 73; h <= 81; h++) { target.getBlockAt(x,h,-138).setType(Material.STONE_BRICKS); target.getBlockAt(x,h,76).setType(Material.STONE_BRICKS); }
        for (int z = -138; z <= 76; z++) for (int h = 73; h <= 81; h++) { target.getBlockAt(-158,h,z).setType(Material.STONE_BRICKS); target.getBlockAt(158,h,z).setType(Material.STONE_BRICKS); }
    }

    private void buildGatehouse(final World target, final int x, final int z, final boolean south) {
        int y = 73;
        for (int side : new int[]{-1,1}) for (int dx = 0; dx < 13; dx++) for (int dz = 0; dz < 11; dz++) for (int h = 0; h < 15; h++) target.getBlockAt(x + side * (14 + dx), y + h, z + (south ? -dz : dz)).setType(Material.STONE_BRICKS);
        for (int side : new int[]{-1,1}) for (int dx = 0; dx < 10; dx++) for (int dz = 0; dz < 8; dz++) for (int h = 1; h < 13; h++) target.getBlockAt(x + side * (16 + dx), y + h, z + (south ? -dz : dz)).setType(Material.AIR);
        buildSpire(target, x - 19, y, z + (south ? -5 : 5)); buildSpire(target, x + 19, y, z + (south ? -5 : 5));
    }

    private void buildBuilding(final World target, final int x, final int y, final int z, final int width, final int depth, final String label, final Material wall, final Material roof, final Material frame) {
        for (int px = x; px < x + width; px++) for (int pz = z; pz < z + depth; pz++) target.getBlockAt(px,y,pz).setType(wall);
        for (int px = x; px < x + width; px++) for (int h = 1; h <= 8; h++) { target.getBlockAt(px,y+h,z).setType(frame); target.getBlockAt(px,y+h,z+depth-1).setType(frame); }
        for (int pz = z; pz < z + depth; pz++) for (int h = 1; h <= 8; h++) { target.getBlockAt(x,y+h,pz).setType(frame); target.getBlockAt(x+width-1,y+h,pz).setType(frame); }
        for (int px = x+1; px < x+width-1; px++) for (int pz = z+1; pz < z+depth-1; pz++) for (int h = 1; h <= 7; h++) target.getBlockAt(px,y+h,pz).setType(Material.AIR);
        for (int layer = 0; layer < 3; layer++) for (int px = x+layer; px < x+width-layer; px++) for (int pz = z+layer; pz < z+depth-layer; pz++) target.getBlockAt(px,y+9+layer,pz).setType(roof);
        target.getBlockAt(x+width/2,y+1,z).setType(Material.AIR); target.getBlockAt(x+width/2,y+2,z).setType(Material.AIR);
        for (int wx=x+5; wx<x+width-3; wx+=8) { target.getBlockAt(wx,y+4,z).setType(Material.GLASS_PANE); target.getBlockAt(wx,y+4,z+depth-1).setType(Material.GLASS_PANE); }
        buildSign(target,x+2,y+11,z-1,label,"");
    }

    private void buildWatchtower(final World target, final int x, final int z) { final int y = terrainYAt(x,z)+1; buildSpire(target,x,y,z); }

    private void buildSpire(final World target, final int x, final int y, final int z) {
        for (int px=x-5; px<=x+5; px++) for (int pz=z-5; pz<=z+5; pz++) for (int h=0; h<=13; h++) target.getBlockAt(px,y+h,pz).setType(Material.STONE_BRICKS);
        for (int px=x-3; px<=x+3; px++) for (int pz=z-3; pz<=z+3; pz++) for (int h=1; h<=11; h++) target.getBlockAt(px,y+h,pz).setType(Material.AIR);
        for (int layer=0; layer<5; layer++) for (int px=x-layer; px<=x+layer; px++) for (int pz=z-layer; pz<=z+layer; pz++) target.getBlockAt(px,y+14+layer,pz).setType(Material.DEEPSLATE_TILES);
        target.getBlockAt(x,y+20,z).setType(Material.LANTERN);
    }

    private void buildFountain(final World target, final int x, final int z) {
        for (int dx=-10;dx<=10;dx++) for(int dz=-10;dz<=10;dz++) if(dx*dx+dz*dz<=100) target.getBlockAt(x+dx,75,z+dz).setType(Material.STONE_BRICKS);
        for (int dx=-6;dx<=6;dx++) for(int dz=-6;dz<=6;dz++) target.getBlockAt(x+dx,76,z+dz).setType(Material.WATER);
        for(int h=77;h<=82;h++) target.getBlockAt(x,h,z).setType(Material.QUARTZ_BLOCK);
        target.getBlockAt(x,83,z).setType(Material.WATER);
    }

    private void buildMarketStalls(final World target) { for(int[] p:new int[][]{{-40,-38},{-16,-38},{10,-38},{36,-38},{-40,-22},{-16,-22},{10,-22},{36,-22}}){ for(int dx=-3;dx<=3;dx++) target.getBlockAt(p[0]+dx,77,p[1]).setType(Material.SPRUCE_PLANKS); for(int dx=-3;dx<=3;dx++){target.getBlockAt(p[0]+dx,81,p[1]-1).setType(Material.RED_WOOL);target.getBlockAt(p[0]+dx,81,p[1]).setType(Material.WHITE_WOOL);} }}
    private void buildStreetTrees(final World target) { for(int x=-132;x<=132;x+=24){ buildTree(target,x,-125); buildTree(target,x,62); } }
    private void buildTree(final World target,int x,int z){int y=terrainYAt(x,z)+1;for(int h=0;h<5;h++)target.getBlockAt(x,y+h,z).setType(Material.OAK_LOG);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<=2;dy++)if(Math.abs(dx)+Math.abs(dz)+dy<5)target.getBlockAt(x+dx,y+5+dy,z+dz).setType(Material.OAK_LEAVES);}
    private void buildHavenLanterns(final World target){for(int z=-120;z<=70;z+=14){int y=terrainYAt(0,z)+1;target.getBlockAt(-8,y,z).setType(Material.SPRUCE_FENCE);target.getBlockAt(-8,y+1,z).setType(Material.LANTERN);target.getBlockAt(8,y,z).setType(Material.SPRUCE_FENCE);target.getBlockAt(8,y+1,z).setType(Material.LANTERN);}}
    private void buildForestCamp(final World target,int x,int z){int y=terrainYAt(x,z)+1;for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=5;dz++)target.getBlockAt(x+dx,y,z+dz).setType(Material.COARSE_DIRT);for(int dx=-3;dx<=3;dx++)for(int dz=-2;dz<=2;dz++)target.getBlockAt(x+dx,y+1,z+dz).setType(Material.CAMPFIRE);buildTree(target,x-7,z+6);buildTree(target,x+7,z+6);}
    private void buildRiverBridge(final World target,int x,int z){int y=terrainYAt(x,z)+1;buildPath(target,x-55,z,x+55,z,4,Material.SPRUCE_PLANKS);for(int px=x-50;px<=x+50;px+=10){target.getBlockAt(px,y-2,z).setType(Material.DARK_OAK_LOG);target.getBlockAt(px,y+2,z-4).setType(Material.SPRUCE_FENCE);target.getBlockAt(px,y+2,z+4).setType(Material.SPRUCE_FENCE);}}
    private void buildRuins(final World target,int x,int z){int y=terrainYAt(x,z)+1;for(int i=-4;i<=4;i++){int px=x+i*9;for(int h=0;h<6+(Math.abs(i)%3);h++)target.getBlockAt(px,y+h,z+5).setType(h%3==0?Material.MOSSY_STONE_BRICKS:Material.STONE_BRICKS);}buildTree(target,x-10,z+7);buildTree(target,x+11,z+4);}
    private void buildSign(final World target,int x,int y,int z,String title,String subtitle){target.getBlockAt(x,y,z).setType(Material.OAK_SIGN); if(target.getBlockAt(x,y,z).getState() instanceof org.bukkit.block.Sign s){s.line(0,net.kyori.adventure.text.Component.text(title));s.line(1,net.kyori.adventure.text.Component.text(subtitle));s.update();}}
    private void buildPath(final World target,int x1,int z1,int x2,int z2,int radius,Material material){int sx=Integer.signum(x2-x1),sz=Integer.signum(z2-z1),steps=0;int x=x1,z=z1;while(true){int y=terrainYAt(x,z);for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)target.getBlockAt(x+dx,y,z+dz).setType(material);if(x==x2&&z==z2)break;if(x!=x2)x+=sx;if(z!=z2)z+=sz;if(++steps>12000)break;}}

    private void ensureNpcs(){ensureNpc(QuestService.LYRA,"§6✦ Warden Lyra",location(-26,77,-91),Villager.Profession.ARMORER);ensureNpc(QuestService.NPC_REN,"§a✦ Merchant Ren",location(42,77,-91),Villager.Profession.FLETCHER);ensureNpc("ascension:innkeeper_mara","§dInnkeeper Mara",location(-105,78,-39),Villager.Profession.LIBRARIAN);ensureNpc("ascension:blacksmith_dain","§cBlacksmith Dain",location(105,78,-39),Villager.Profession.TOOLSMITH);ensureNpc("ascension:guide_elian","§bGuide Elian",location(0,78,22),Villager.Profession.CARTOGRAPHER);ensureNpc("ascension:stablemaster_kael","§eStablemaster Kael",location(105,78,18),Villager.Profession.FARMER);ensureNpc("ascension:captain_soren","§9Captain Soren",location(0,79,61),Villager.Profession.LIBRARIAN);}
    private void ensureNpc(final String id,final String name,final Location loc,final Villager.Profession profession){for(Entity e:world.getNearbyEntities(loc,12,8,12))if(id.equals(e.getPersistentDataContainer().get(npcKey,PersistentDataType.STRING)))return;Villager v=(Villager)world.spawnEntity(loc,EntityType.VILLAGER);v.getPersistentDataContainer().set(npcKey,PersistentDataType.STRING,id);v.setCustomName(name);v.setCustomNameVisible(true);v.setAI(false);v.setInvulnerable(true);v.setSilent(true);v.setCollidable(false);v.setPersistent(true);v.setProfession(profession);}

    private void spawnAuthoredMob(final String id,final int x,final int z){int y=terrainYAt(x,z)+1;mobs.spawnMob(AssetId.parse(id),location(x,y,z));}
    private Location havenSpawn(){return location(-2,terrainYAt(-2,-88)+2,-88);}
    private Location location(final int x,final int y,final int z){return new Location(world,x,y,z);}
    private int terrainYAt(final int x,final int z){double n=72.0D+smoothNoise(x/520.0D,z/520.0D)*26.0D+smoothNoise(x/180.0D,z/180.0D)*15.0D+smoothNoise(x/62.0D,z/62.0D)*4.0D;if(Math.sqrt((double)x*x+(double)z*z)<150)n=72;double north=Math.max(0.0D,Math.min(1.0D,(-z-850.0D)/900.0D));n+=north*north*55;double river=Math.abs(z-(260.0D+Math.sin(x/310.0D)*180.0D+Math.sin(x/97.0D)*34.0D));n-=Math.max(0,1-river/110.0D)*18;return Math.max(59,(int)Math.round(n));}
    private static double smoothNoise(final double x,final double z){int x0=(int)Math.floor(x),z0=(int)Math.floor(z);double tx=x-x0,tz=z-z0;tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);return lerp(lerp(valueNoise(x0,z0),valueNoise(x0+1,z0),tx),lerp(valueNoise(x0,z0+1),valueNoise(x0+1,z0+1),tx),tz);}
    private static double valueNoise(final int x,final int z){long h=0x9E3779B97F4A7C15L;h^=(long)x*0xBF58476D1CE4E5B9L;h=Long.rotateLeft(h,27);h^=(long)z*0x94D049BB133111EBL;h^=h>>>30;h*=0xBF58476D1CE4E5B9L;h^=h>>>27;h*=0x94D049BB133111EBL;return ((h^(h>>>31))&0xFFFFFFL)/8388607.5D-1;}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private boolean hasCurrentBuild(){try{return Files.exists(buildMarker)&&WORLD_VERSION.equals(Files.readString(buildMarker).trim());}catch(IOException e){return false;}}
    private void markBuild(){try{Files.createDirectories(buildMarker.getParent());Files.writeString(buildMarker,WORLD_VERSION);}catch(IOException e){throw new IllegalStateException("Could not mark Floor 1 build",e);}}
    private void migrateLegacyWorld(){if(hasCurrentBuild())return;World existing=Bukkit.getWorld(WORLD_NAME);if(existing!=null)Bukkit.unloadWorld(existing,false);Path dir=plugin.getServer().getWorldContainer().toPath().resolve(WORLD_NAME);if(!Files.exists(dir))return;try(var s=Files.walk(dir)){s.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}catch(IOException e){plugin.getLogger().warning("Floor 1 migration incomplete: "+e.getMessage());}}
}
