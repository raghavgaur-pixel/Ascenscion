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

/** Cinematic authored Floor 1: a lived-in Haven, wilderness route, and monumental first gate. */
public final class FloorOneWorldService implements Listener {
    public static final String WORLD_NAME = "ascension_floor_001";
    private static final String WORLD_VERSION = "floor_001_world_v6";
    private static final int GATE_Z = 720;
    private static final int BOSS_Z = 770;
    private static final String NPC_LYRA = "ascension:warden_lyra";
    private static final String NPC_REN = "ascension:merchant_ren";
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
        this.plugin.getLogger().info("Floor 1 ready: cinematic Haven + living wilderness + First Gate / " + WORLD_VERSION);
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
            player.sendTitle("§6ASCENSION", "§fHaven · Floor 1", 10, 70, 20);
            player.sendMessage("§8The Tower has carried you to §6Haven§8, a safe city beneath the First Gate.");
            player.sendMessage("§7Your first step is waiting at the Warden Hall.");
        } else if (!hasRookieSword(player)) {
            giveStarterEquipment(player);
        }
    }

    public void ensureHuntMobs() {
        if (this.world == null) return;
        final int[] wolfCount = {0};
        final int[] beetleCount = {0};
        for (final Entity entity : this.world.getEntities()) {
            final String id = this.mobs.mobId(entity).map(AssetId::toString).orElse("");
            if (MOB_WOLF.equals(id) && !entity.isDead()) wolfCount[0]++;
            if (MOB_BEETLE.equals(id) && !entity.isDead()) beetleCount[0]++;
        }
        final int[][] wolves = {{96,175},{-100,205},{142,250},{-156,292},{72,350},{-70,395},{190,445},{-205,470}};
        final int[][] beetles = {{170,220},{-182,265},{120,335},{-115,385},{235,455},{-230,505}};
        for (int i = wolfCount[0]; i < wolves.length; i++) spawnAuthoredMob(MOB_WOLF, wolves[i][0], wolves[i][1]);
        for (int i = beetleCount[0]; i < beetles.length; i++) spawnAuthoredMob(MOB_BEETLE, beetles[i][0], beetles[i][1]);
    }

    public void ensureBoss() {
        if (this.world == null) return;
        for (final Entity entity : this.world.getEntities()) if (BOSS_WARDEN.equals(this.mobs.bossId(entity).map(AssetId::toString).orElse("")) && !entity.isDead()) return;
        final LivingEntity boss = this.mobs.spawnBoss(AssetId.parse(BOSS_WARDEN), location(0, terrainYAt(0, BOSS_Z) + 1, BOSS_Z)).orElse(null);
        if (boss != null) { boss.setPersistent(true); boss.setGlowing(true); boss.setRemoveWhenFarAway(false); boss.setCustomNameVisible(true); boss.playEffect(EntityEffect.HURT); }
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
                    player.sendMessage("§6Warden Lyra§f: The Tower has chosen you. Beyond the northern walls lie the Whispering Woods.");
                    player.sendMessage("§6Warden Lyra§f: Hunt §e5 Forest Wolves§f and §e3 Iron Beetles§f, then follow the old road north.");
                    player.sendMessage("§6Warden Lyra§f: Your journal will record every step.");
                } else player.sendMessage("§6Warden Lyra§f: The old road still waits for you. Do not underestimate what lives beyond the river.");
                this.journal.open(player);
                ensureHuntMobs();
            }
            case NPC_REN -> { player.sendMessage("§aMerchant Ren§f: Trophies from the wilds tell better stories than gold ever could."); player.sendMessage("§7The market opens toward the river. Take a look around Haven before you leave."); this.journal.open(player); }
            case "ascension:innkeeper_mara" -> player.sendMessage("§dInnkeeper Mara§f: Rest here when the road gets long. Haven is built for climbers who intend to return.");
            case "ascension:blacksmith_dain" -> player.sendMessage("§cBlacksmith Dain§f: That Rookie Sword is a beginning, not a destination.");
            case "ascension:guide_elian" -> { player.sendMessage("§bGuide Elian§f: Stay on the stone road. The First Gate stands far beyond the woods."); this.journal.open(player); }
            case "ascension:stablemaster_kael" -> player.sendMessage("§eStablemaster Kael§f: The valley narrows beyond the ruins. Keep your bearings.");
            default -> player.sendMessage("§7The villager watches you carefully.");
        }
    }

    @EventHandler
    public void onCombat(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (this.shuttingDown || this.world == null || !target.getWorld().equals(this.world)) return;
        if (this.mobs.mobId(target).isEmpty() && this.mobs.bossId(target).isEmpty()) return;
        event.setCancelled(true);
        if (player.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) { player.sendActionBar("§eEquip your Rookie Sword to attack."); return; }
        final var result = this.abilities.execute(new AbilityRequest(player.getUniqueId(), "ascension:quick_strike", target.getUniqueId()));
        if (result.status() == com.ascension.abilities.model.AbilityResult.Status.SUCCESS) { target.playEffect(EntityEffect.HURT); player.sendActionBar("§c✦ Quick Strike"); }
        else if (!result.reason().toLowerCase(Locale.ROOT).contains("cooldown")) player.sendActionBar("§7" + result.reason());
    }

    @EventHandler public void onTarget(final EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player) || this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        final String id = this.mobs.mobId(event.getEntity()).map(AssetId::toString).orElse("");
        if (!MOB_WOLF.equals(id) && !MOB_BEETLE.equals(id)) return;
        if (player.getLocation().distanceSquared(new Location(this.world, 0, 0, 100)) > 160000D) event.setCancelled(true);
    }

    @EventHandler public void onDeath(final EntityDeathEvent event) {
        if (this.world == null || !event.getEntity().getWorld().equals(this.world)) return;
        if (this.mobs.mobId(event.getEntity()).isPresent() || this.mobs.bossId(event.getEntity()).isPresent()) this.mobs.forget(event.getEntity().getUniqueId());
    }

    @EventHandler public void onMove(final PlayerMoveEvent event) {
        if (this.world == null || !event.getPlayer().getWorld().equals(this.world)) return;
        final Player player = event.getPlayer();
        if (player.getLocation().getZ() < GATE_Z || !this.quests.active(player.getUniqueId()).contains(QUEST_FIRST_GATE)) return;
        final QuestService.Result result = this.quests.reachLocation(player.getUniqueId(), "first_gate");
        if (result.status() == QuestService.Status.SUCCESS) { player.sendTitle("§cTHE FIRST GATE", "§7An ancient guardian awakens", 10, 70, 20); ensureBoss(); }
    }

    private void spawnAuthoredMob(final String id, final int x, final int z) { this.mobs.spawnMob(AssetId.parse(id), location(x, terrainYAt(x, z) + 1, z)); }

    private World loadWorld() {
        final World existing = Bukkit.getWorld(WORLD_NAME); if (existing != null) return existing;
        final WorldCreator creator = new WorldCreator(WORLD_NAME); creator.environment(World.Environment.NORMAL); creator.generateStructures(false); creator.generator(new FloorOneWorldGenerator()); return creator.createWorld();
    }

    private void configureWorld(final World target) {
        target.setDifficulty(Difficulty.NORMAL); target.setStorm(false); target.setThundering(false); target.setTime(1500L); target.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); target.setGameRule(GameRule.DO_WEATHER_CYCLE, false); target.setGameRule(GameRule.DO_MOB_SPAWNING, false); target.setGameRule(GameRule.DO_FIRE_TICK, false); target.getWorldBorder().setCenter(0, 0); target.getWorldBorder().setSize(10000); target.setSpawnLocation(0, terrainYAt(0,-70)+1, -70);
    }

    private void buildFloor(final World target) {
        buildHaven(target);
        buildNorthRoad(target);
        buildWoodsLandmarks(target);
        buildRiverCrossing(target);
        buildFirstGate(target);
        buildOldRoadRuins(target);
    }

    private void buildHaven(final World target) {
        final int y = 72;
        fill(target,-158,-138,158,95,y,Material.STONE_BRICKS);
        fill(target,-138,-121,138,76,y+1,Material.COBBLED_DEEPSLATE);
        buildPath(target,-160,-61,160,-61,7,Material.POLISHED_ANDESITE);
        buildPath(target,0,-145,0,190,7,Material.POLISHED_ANDESITE);
        buildPath(target,-125,-2,125,-2,4,Material.STONE_BRICKS);
        buildPath(target,-118,57,118,57,4,Material.STONE_BRICKS);
        fill(target,-43,-104,43,-30,y+2,Material.SMOOTH_STONE);
        buildFountain(target,0,y+4,-67);
        buildMonument(target,0,y+3,-38);
        buildLampRow(target,-35,-106,35,-106);
        buildLampRow(target,-35,-32,35,-32);

        buildBuilding(target,-78,y+3,-111,60,34,"WARDEN HALL",Material.STONE_BRICKS,Material.DEEPSLATE_TILES,Material.OAK_LOG);
        buildBuilding(target,18,y+3,-111,60,34,"REN'S MARKET",Material.SPRUCE_PLANKS,Material.DARK_OAK_SLAB,Material.SPRUCE_LOG);
        buildBuilding(target,-125,y+3,-54,43,31,"HAVEN INN",Material.OAK_PLANKS,Material.SPRUCE_SLAB,Material.OAK_LOG);
        buildBuilding(target,82,y+3,-54,43,31,"BLACKSMITH",Material.STONE_BRICKS,Material.POLISHED_DEEPSLATE,Material.DEEPSLATE);
        buildBuilding(target,-125,y+3,5,43,31,"GUILD HALL",Material.SPRUCE_PLANKS,Material.DARK_OAK_SLAB,Material.SPRUCE_LOG);
        buildBuilding(target,82,y+3,5,43,31,"STABLES",Material.OAK_PLANKS,Material.OAK_SLAB,Material.OAK_LOG);
        buildBuilding(target,-55,y+3,43,42,28,"FLETCHER'S YARD",Material.OAK_PLANKS,Material.SPRUCE_SLAB,Material.SPRUCE_LOG);
        buildBuilding(target,13,y+3,43,42,28,"FARMWARD",Material.OAK_PLANKS,Material.OAK_SLAB,Material.OAK_LOG);

        buildTownWall(target,y);
        buildGatehouse(target,0,y+1,-139, true);
        buildGatehouse(target,0,y+1,96, false);
        buildWatchtower(target,-145,y+3,-128);
        buildWatchtower(target,145,y+3,-128);
        buildWatchtower(target,-145,y+3,85);
        buildWatchtower(target,145,y+3,85);
        buildMarketStalls(target,y+6);
        buildFlowerBeds(target,y+6);
        buildStreetTrees(target);
        buildDocks(target,y+3,112,-17);
    }

    private void buildNorthRoad(final World target) {
        buildPath(target,0,92,0,GATE_Z-40,6,Material.POLISHED_ANDESITE);
        for(int z=110;z<=GATE_Z-40;z+=24){ final int y=terrainYAt(0,z)+1; target.getBlockAt(-9,y,z).setType(Material.LANTERN); target.getBlockAt(9,y,z).setType(Material.LANTERN); }
    }

    private void buildWoodsLandmarks(final World target) {
        final int[][] camps={{-55,180},{60,285},{-70,395},{65,515}};
        for(int[] p:camps) buildForestCamp(target,p[0],p[1]);
        buildBuilding(target,-28,terrainYAt(0,435)+1,430,30,22,"RANGER OUTPOST",Material.SPRUCE_PLANKS,Material.SPRUCE_SLAB,Material.SPRUCE_LOG);
        buildSign(target,0,terrainYAt(0,155)+5,155,"WHISPERING WOODS","First Hunt grounds");
    }

    private void buildRiverCrossing(final World target) {
        final int z=305; final int y=terrainYAt(0,z)+1;
        buildPath(target,-42,z,42,z,4,Material.SPRUCE_PLANKS);
        for(int x=-42;x<=42;x+=7){ target.getBlockAt(x,y-2,z).setType(Material.DARK_OAK_LOG); target.getBlockAt(x,y+2,z-4).setType(Material.SPRUCE_FENCE); target.getBlockAt(x,y+2,z+4).setType(Material.SPRUCE_FENCE); }
    }

    private void buildFirstGate(final World target) {
        final int y=terrainYAt(0,GATE_Z); 
        for(int x=-42;x<=42;x++) for(int h=1;h<=18;h++){ if(Math.abs(x)<=8&&h<10) continue; target.getBlockAt(x,y+h,GATE_Z).setType(Material.DEEPSLATE_BRICKS); }
        for(int side:new int[]{-1,1}){ buildWatchtower(target,side*38,y+1,GATE_Z-16); for(int xx=0;xx<20;xx++) for(int h=1;h<=24;h++) target.getBlockAt(side*(18+xx),y+h,GATE_Z).setType(Material.DEEPSLATE_BRICKS); }
        for(int x=-8;x<=8;x++) for(int h=10;h<=18;h++) target.getBlockAt(x,y+h,GATE_Z).setType(Material.IRON_BARS);
        for(int x=-8;x<=8;x+=2) target.getBlockAt(x,y+20,GATE_Z).setType(Material.SOUL_LANTERN);
        buildPath(target,0,GATE_Z-70,0,GATE_Z,5,Material.POLISHED_DEEPSLATE);
        buildPath(target,0,GATE_Z,0,GATE_Z+80,6,Material.BLACKSTONE);
        buildSign(target,0,y+23,GATE_Z-2,"THE FIRST GATE","Warden of the First Gate");
    }

    private void buildOldRoadRuins(final World target) {
        final int z=560; final int y=terrainYAt(0,z)+1;
        for(int i=-4;i<=4;i++){ final int x=i*13; for(int h=0;h<7-Math.abs(i)/2;h++) target.getBlockAt(x,y+h,z+6).setType(Material.MOSSY_STONE_BRICKS); }
        for(int x=-50;x<=50;x+=8) target.getBlockAt(x,y+1,z+16).setType(Material.CRACKED_STONE_BRICKS);
        buildSign(target,-10,y+7,z+15,"THE OLD ROAD","Ruins from before Haven");
    }

    private void buildForestCamp(final World target,final int x,final int z){ final int y=terrainYAt(x,z)+1; for(int dx=-5;dx<=5;dx++)for(int dz=-4;dz<=4;dz++)target.getBlockAt(x+dx,y,z+dz).setType(Material.COARSE_DIRT); for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)target.getBlockAt(x+dx,y+1,z+dz).setType(Material.CAMPFIRE); target.getBlockAt(x+4,y+1,z).setType(Material.CHEST); buildTree(target,x-5,z+5); buildTree(target,x+5,z+5); }

    private void buildTownWall(final World target,final int y){ for(int x=-160;x<=160;x++)for(int h=0;h<10;h++){target.getBlockAt(x,y+h,-138).setType(Material.STONE_BRICKS);target.getBlockAt(x,y+h,96).setType(Material.STONE_BRICKS);} for(int z=-138;z<=96;z++)for(int h=0;h<10;h++){target.getBlockAt(-160,y+h,z).setType(Material.STONE_BRICKS);target.getBlockAt(160,y+h,z).setType(Material.STONE_BRICKS);} }

    private void buildGatehouse(final World target,int x,int y,int z,boolean south){ for(int side:new int[]{-1,1})for(int dx=0;dx<12;dx++)for(int dz=0;dz<10;dz++)for(int h=0;h<13;h++)target.getBlockAt(x+side*(13+dx),y+h,z+(south?-dz:dz)).setType(Material.STONE_BRICKS); for(int side:new int[]{-1,1})for(int dx=0;dx<8;dx++)for(int dz=0;dz<8;dz++)for(int h=1;h<11;h++)target.getBlockAt(x+side*(15+dx),y+h,z+(south?-dz:dz)).setType(Material.AIR); }

    private void buildBuilding(final World target,int x,int y,int z,int width,int depth,String label,Material wall,Material roof,Material frame){ for(int px=x;px<x+width;px++)for(int pz=z;pz<z+depth;pz++)target.getBlockAt(px,y,pz).setType(wall); for(int px=x;px<x+width;px++)for(int h=1;h<=8;h++){target.getBlockAt(px,y+h,z).setType(frame);target.getBlockAt(px,y+h,z+depth-1).setType(frame);} for(int pz=z;pz<z+depth;pz++)for(int h=1;h<=8;h++){target.getBlockAt(x,y+h,pz).setType(frame);target.getBlockAt(x+width-1,y+h,pz).setType(frame);} for(int px=x+1;px<x+width-1;px++)for(int pz=z+1;pz<z+depth-1;pz++)for(int h=1;h<=7;h++)target.getBlockAt(px,y+h,pz).setType(Material.AIR); for(int layer=0;layer<3;layer++)for(int px=x+layer;px<x+width-layer;px++)for(int pz=z+layer;pz<z+depth-layer;pz++)target.getBlockAt(px,y+9+layer,pz).setType(roof); target.getBlockAt(x+width/2,y+1,z).setType(Material.AIR);target.getBlockAt(x+width/2,y+2,z).setType(Material.AIR); for(int wx=x+5;wx<x+width-3;wx+=8){target.getBlockAt(wx,y+4,z).setType(Material.GLASS_PANE);target.getBlockAt(wx,y+4,z+depth-1).setType(Material.GLASS_PANE);} buildSign(target,x+3,y+9,z-1,label,""); }

    private void buildWatchtower(final World target,int x,int y,int z){ for(int px=x-4;px<=x+4;px++)for(int pz=z-4;pz<=z+4;pz++)for(int h=0;h<=14;h++)target.getBlockAt(px,y+h,pz).setType(Material.STONE_BRICKS); for(int px=x-3;px<=x+3;px++)for(int pz=z-3;pz<=z+3;pz++)for(int h=1;h<=12;h++)target.getBlockAt(px,y+h,pz).setType(Material.AIR); for(int px=x-5;px<=x+5;px++)for(int pz=z-5;pz<=z+5;pz++)target.getBlockAt(px,y+15,pz).setType(Material.DEEPSLATE_TILES);target.getBlockAt(x,y+16,z).setType(Material.SOUL_LANTERN); }

    private void buildFountain(final World target,int x,int y,int z){ for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)target.getBlockAt(x+dx,y,z+dz).setType(Material.STONE_BRICKS); for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)target.getBlockAt(x+dx,y+1,z+dz).setType(Material.WATER); for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)target.getBlockAt(x+dx,y+2,z+dz).setType(Material.QUARTZ_BLOCK); for(int h=3;h<=8;h++)target.getBlockAt(x,y+h,z).setType(Material.QUARTZ_BLOCK);target.getBlockAt(x,y+9,z).setType(Material.WATER); }

    private void buildMonument(final World target,int x,int y,int z){ for(int h=0;h<12;h++)target.getBlockAt(x,y+h,z).setType(h==11?Material.GOLD_BLOCK:Material.QUARTZ_BLOCK); for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)target.getBlockAt(x+dx,y,z+dz).setType(Material.POLISHED_DIORITE); }

    private void buildLampRow(final World target,int x1,int z1,int x2,int z2){ final int sx=Integer.signum(x2-x1);for(int x=x1;x!=x2+sx;x+=sx){int y=terrainYAt(x,z1)+1;target.getBlockAt(x,y,z1).setType(Material.SPRUCE_FENCE);target.getBlockAt(x,y+1,z1).setType(Material.LANTERN);} }
    private void buildMarketStalls(final World target,int y){ final int[][] spots={{-42,-54},{-17,-54},{8,-54},{33,-54},{-42,-39},{-17,-39},{8,-39},{33,-39}};for(int[]p:spots){for(int dx=-3;dx<=3;dx++)target.getBlockAt(p[0]+dx,y,p[1]).setType(Material.SPRUCE_PLANKS);for(int dx=-3;dx<=3;dx++){target.getBlockAt(p[0]+dx,y+4,p[1]-1).setType(Material.WHITE_WOOL);target.getBlockAt(p[0]+dx,y+4,p[1]).setType(Material.RED_WOOL);}} }
    private void buildFlowerBeds(final World target,int y){for(int x=-95;x<=95;x+=14){target.getBlockAt(x,y,-31).setType(Material.MOSS_BLOCK);target.getBlockAt(x,y+1,-31).setType((x/14&1)==0?Material.POPPY:Material.BLUE_ORCHID);}}
    private void buildStreetTrees(final World target){for(int x=-115;x<=115;x+=28){buildTree(target,x,-118);buildTree(target,x,58);}}
    private void buildTree(final World target,int x,int z){int y=terrainYAt(x,z)+1;for(int h=0;h<5;h++)target.getBlockAt(x,y+h,z).setType(Material.OAK_LOG);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dh=0;dh<=2;dh++)if(Math.abs(dx)+Math.abs(dz)+dh<5)target.getBlockAt(x+dx,y+4+dh,z+dz).setType(Material.OAK_LEAVES);}
    private void buildDocks(final World target,int y,int x,int z){for(int dx=-20;dx<=20;dx++)for(int dz=-5;dz<=5;dz++)target.getBlockAt(x+dx,y,z+dz).setType(Material.SPRUCE_PLANKS);for(int dx=-18;dx<=18;dx+=6)target.getBlockAt(x+dx,y+1,z).setType(Material.OAK_FENCE);}
    private void buildPath(final World target,int x1,int z1,int x2,int z2,int radius,Material material){final int sx=Integer.signum(x2-x1),sz=Integer.signum(z2-z1);int steps=0;while(true){int y=terrainYAt(x1,z1);for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)target.getBlockAt(x1+dx,y,z1+dz).setType(material);if(x1==x2&&z1==z2)break;if(x1!=x2)x1+=sx;if(z1!=z2)z1+=sz;if(++steps>12000)break;}}
    private void fill(final World target,int x1,int z1,int x2,int z2,int y,Material material){for(int x=Math.min(x1,x2);x<=Math.max(x1,x2);x++)for(int z=Math.min(z1,z2);z<=Math.max(z1,z2);z++)target.getBlockAt(x,y,z).setType(material);}
    private static void buildSign(final World world,int x,int y,int z,String line1,String line2){final var block=world.getBlockAt(x,y,z);block.setType(Material.OAK_SIGN);if(block.getState() instanceof Sign sign){sign.line(1,net.kyori.adventure.text.Component.text(line1));if(!line2.isBlank())sign.line(2,net.kyori.adventure.text.Component.text(line2));sign.update(true,false);}}
    private void ensureNpcs(){ensureNpc(NPC_LYRA,"§6✦ Warden Lyra",location(-45,77,-90),Villager.Profession.ARMORER);ensureNpc(NPC_REN,"§a✦ Merchant Ren",location(45,77,-90),Villager.Profession.FLETCHER);ensureNpc("ascension:innkeeper_mara","§dInnkeeper Mara",location(-104,77,-36),Villager.Profession.LIBRARIAN);ensureNpc("ascension:blacksmith_dain","§cBlacksmith Dain",location(104,77,-36),Villager.Profession.TOOLSMITH);ensureNpc("ascension:guide_elian","§bGuide Elian",location(0,77,9),Villager.Profession.CARTOGRAPHER);ensureNpc("ascension:stablemaster_kael","§eStablemaster Kael",location(104,77,20),Villager.Profession.FARMER);}
    private void ensureNpc(String id,String name,Location loc,Villager.Profession profession){for(Entity entity:this.world.getNearbyEntities(loc,10,7,10))if(id.equals(entity.getPersistentDataContainer().get(this.npcKey,PersistentDataType.STRING)))return;final Villager villager=(Villager)this.world.spawnEntity(loc,EntityType.VILLAGER);villager.getPersistentDataContainer().set(this.npcKey,PersistentDataType.STRING,id);villager.setCustomName(name);villager.setCustomNameVisible(true);villager.setAI(false);villager.setInvulnerable(true);villager.setSilent(false);villager.setCollidable(false);villager.setPersistent(true);villager.setProfession(profession);}
    private void giveStarterEquipment(Player player){final var sword=createItem("ascension:rookie_sword");final var ring=createItem("ascension:rookie_iron_ring");if(sword!=null)player.getInventory().setItem(0,sword);if(ring!=null)player.getInventory().setItem(8,ring);player.getInventory().setHeldItemSlot(0);}
    private boolean hasRookieSword(Player player){for(var item:player.getInventory().getContents())if(item!=null&&item.getType()==Material.IRON_SWORD&&item.hasItemMeta()&&item.getItemMeta().hasDisplayName())return true;return false;}
    private org.bukkit.inventory.ItemStack createItem(String rawId){final AssetId id=AssetId.parse(rawId);final var definition=this.items.findDefinition(id).orElse(null);if(definition==null)return null;final Material material;try{material=Material.valueOf(definition.data().getString("material","WOODEN_SWORD").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){return null;}final var stack=new org.bukkit.inventory.ItemStack(material);final var meta=stack.getItemMeta();if(meta!=null){meta.displayName(net.kyori.adventure.text.Component.text(definition.descriptor().displayName()));stack.setItemMeta(meta);}this.itemEncoder.encode(this.items.create(id),stack);return stack;}
    private Location spawnLocation(){return location(0,terrainYAt(0,-70)+1,-70);}
    private Location location(double x,double y,double z){return new Location(this.world,x+0.5,y,z+0.5);}
    private static int terrainYAt(int x,int z){return Math.max(63,terrainHeightApprox(x,z));}
    private static int terrainHeightApprox(int x,int z){double h=72.0+Math.sin(x/310.0)*6.0+Math.sin(z/220.0)*5.0+Math.sin((x+z)/90.0)*2.0;h-=Math.max(0,1-Math.abs(z-(260+Math.sin(x/310.0)*180))/110.0)*18.0;double dx=x+620.0,dz=z-540.0;h-=Math.max(0,1-Math.sqrt(dx*dx+dz*dz)/110.0)*32.0;return (int)Math.round(h);}
    private boolean hasCurrentWorldBuild(){try{return Files.exists(this.buildMarker)&&WORLD_VERSION.equals(Files.readString(this.buildMarker).trim());}catch(IOException e){return false;}}
    private void markProvisioned(){try{Files.createDirectories(this.buildMarker.getParent());Files.writeString(this.buildMarker,WORLD_VERSION);}catch(IOException e){throw new IllegalStateException("Failed to mark Floor 1 world",e);}}
    private void migrateLegacyWorld(){if(hasCurrentWorldBuild())return;final World existing=Bukkit.getWorld(WORLD_NAME);if(existing!=null)Bukkit.unloadWorld(existing,false);final Path worldDir=this.plugin.getServer().getWorldContainer().toPath().resolve(WORLD_NAME);if(!Files.exists(worldDir))return;try(var stream=Files.walk(worldDir)){stream.sorted(Comparator.reverseOrder()).forEach(path->{try{Files.deleteIfExists(path);}catch(IOException ignored){}});}catch(IOException exception){this.plugin.getLogger().warning("Could not migrate old Floor 1 world: "+exception.getMessage());}}
}
