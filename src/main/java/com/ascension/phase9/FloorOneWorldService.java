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
        this.shuttingDown = false; migrateLegacyWorld(); this.world = loadWorld(); configureWorld(this.world);
        if (!hasCurrentBuild()) { buildFloor(this.world); markBuild(); }
        ensureNpcs(); register(); plugin.getLogger().info("Ascension Floor 1 loaded: " + WORLD_VERSION);
    }
    public void stop() { this.shuttingDown = true; if (world == null) return; for (Entity e : world.getEntities()) if (mobs.mobId(e).isPresent() || mobs.bossId(e).isPresent()) { mobs.forget(e.getUniqueId()); e.remove(); } }

    public void preparePlayer(final Player player) {
        if (world == null || !player.isOnline()) return;
        quests.grantStarterItems(player.getUniqueId());
        Location spawn = havenSpawn();
        if (!player.getWorld().equals(world) || player.getLocation().distanceSquared(spawn) > 250000D) {
            player.teleport(spawn); player.setRespawnLocation(spawn, true);
            player.sendTitle("§6ASCENSION", "§fHaven · Floor 1", 10, 70, 20);
            player.sendMessage("§8Welcome to §6Haven§8, the first safe settlement beneath the Tower.");
        }
        ensureHuntMobs();
    }

    public void ensureHuntMobs() {
        if (world == null) return;
        int wolves = 0, beetles = 0;
        for (Entity e : world.getEntities()) {
            String id = mobs.mobId(e).map(AssetId::toString).orElse("");
            if (QuestService.WOLF.equals(id) && !e.isDead()) wolves++;
            if (QuestService.BEETLE.equals(id) && !e.isDead()) beetles++;
        }
        int[][] wolf = {{-34,132},{35,150},{-72,188},{76,205},{-112,248},{108,270},{-48,318},{58,345},{-130,372},{125,400}};
        int[][] beetle = {{18,170},{-18,228},{45,292},{-55,350},{-82,420},{82,448}};
        for (int i=wolves;i<wolf.length;i++) spawnAuthoredMob(QuestService.WOLF,wolf[i][0],wolf[i][1]);
        for (int i=beetles;i<beetle.length;i++) spawnAuthoredMob(QuestService.BEETLE,beetle[i][0],beetle[i][1]);
    }

    public void ensureBoss() {
        if (world == null) return;
        for (Entity e:world.getEntities()) if (mobs.bossId(e).map(AssetId::toString).orElse("").equals(QuestService.BOSS) && !e.isDead()) return;
        mobs.spawnBoss(AssetId.parse(QuestService.BOSS),location(0,terrainYAt(0,BOSS_Z)+1,BOSS_Z)).ifPresent(b->{b.setGlowing(true);b.setPersistent(true);b.setRemoveWhenFarAway(false);b.setCustomNameVisible(true);});
    }

    @EventHandler public void onNpcInteract(final PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity) || world == null || !entity.getWorld().equals(world)) return;
        String id = entity.getPersistentDataContainer().get(npcKey,PersistentDataType.STRING); if (id == null) return; event.setCancelled(true);
        Player p=event.getPlayer(); String key=p.getUniqueId()+":"+id; UUID k=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)); int stage=dialogue.merge(k,1,Integer::sum)-1;
        switch(id){
            case QuestService.LYRA -> { QuestService.Result r=quests.talkToNpc(p.getUniqueId(),QuestService.LYRA); if(stage==0){p.sendMessage("§6Warden Lyra§f: So, the Tower finally chose another climber.");p.sendMessage("§6Warden Lyra§f: Welcome to Haven. Your ascent begins here.");p.sendMessage("§6Warden Lyra§f: Take your Rookie Sword and learn the First Field.");}else if(stage==1){p.sendMessage("§6Warden Lyra§f: Hunt §e5 Forest Wolves§f and §e3 Iron Beetles§f beyond the river.");p.sendMessage("§6Warden Lyra§f: Your journal will track every kill.");}else p.sendMessage("§6Warden Lyra§f: The First Gate is where the Tower starts demanding more than courage."); journal.open(p); if(r.status()==QuestService.Status.REJECTED)p.sendMessage("§7Your Arrival record is already complete."); }
            case QuestService.NPC_REN -> { if(stage%2==0){p.sendMessage("§aMerchant Ren§f: Fresh from the Tower? Welcome to Haven's market.");p.sendMessage("§aMerchant Ren§f: Trophies from the wilds have real value here.");}else p.sendMessage("§aMerchant Ren§f: Better steel and rarer finds wait higher in the Tower."); journal.open(p); }
            case "ascension:innkeeper_mara" -> p.sendMessage("§dInnkeeper Mara§f: Rest here when the road gets long. Haven welcomes climbers who return.");
            case "ascension:blacksmith_dain" -> p.sendMessage("§cBlacksmith Dain§f: Your Rookie Sword is honest steel. Earn something better.");
            case "ascension:guide_elian" -> {p.sendMessage("§bGuide Elian§f: North road. River bridge. Old ruins. First Gate.");journal.open(p);}
            case "ascension:stablemaster_kael" -> p.sendMessage("§eStablemaster Kael§f: The valley is wider than it looks. Keep the road in sight.");
            case "ascension:captain_soren" -> p.sendMessage("§9Captain Soren§f: The First Gate is the first true test of a climber.");
            default -> p.sendMessage("§7The resident nods to you.");
        }
    }

    @EventHandler public void onCombat(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (shuttingDown || world == null || !target.getWorld().equals(world)) return;
        if (mobs.mobId(target).isEmpty() && mobs.bossId(target).isEmpty()) return;
        event.setCancelled(true);
        if (player.getInventory().getItemInMainHand().getType()!=Material.IRON_SWORD){player.sendActionBar("§eEquip your Rookie Sword.");return;}
        var result=abilities.execute(new AbilityRequest(player.getUniqueId(),"ascension:quick_strike",target.getUniqueId()));
        if(result.status()==com.ascension.abilities.model.AbilityResult.Status.SUCCESS){target.playEffect(EntityEffect.HURT);player.sendActionBar("§c✦ Quick Strike");}
    }
    @EventHandler public void onTarget(final EntityTargetEvent event){ }
    @EventHandler public void onDeath(final EntityDeathEvent event){if(world!=null&&event.getEntity().getWorld().equals(world)&&(mobs.mobId(event.getEntity()).isPresent()||mobs.bossId(event.getEntity()).isPresent()))mobs.forget(event.getEntity().getUniqueId());}
    @EventHandler public void onMove(final PlayerMoveEvent event){if(world==null||!event.getPlayer().getWorld().equals(world)||event.getPlayer().getLocation().getZ()<GATE_Z||!quests.active(event.getPlayer().getUniqueId()).contains(QuestService.FIRST_GATE))return;if(quests.reachLocation(event.getPlayer().getUniqueId(),"first_gate").status()==QuestService.Status.SUCCESS){event.getPlayer().sendTitle("§cTHE FIRST GATE","§7The guardian awakens",5,60,15);ensureBoss();}}

    private World loadWorld(){World existing=Bukkit.getWorld(WORLD_NAME);if(existing!=null)return existing;WorldCreator c=new WorldCreator(WORLD_NAME);c.environment(World.Environment.NORMAL);c.generateStructures(false);c.generator(new FloorOneWorldGenerator());return c.createWorld();}
    private void configureWorld(World w){w.setDifficulty(Difficulty.NORMAL);w.setStorm(false);w.setThundering(false);w.setTime(1000L);w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);w.setGameRule(GameRule.DO_WEATHER_CYCLE,false);w.setGameRule(GameRule.DO_MOB_SPAWNING,false);w.setGameRule(GameRule.DO_FIRE_TICK,false);w.getWorldBorder().setCenter(0,0);w.getWorldBorder().setSize(10000);w.setSpawnLocation(0,terrainYAt(0,-90)+2,-90);}

    private void buildFloor(World t){buildHaven(t);buildNorthRoad(t);buildWilderness(t);buildFirstGate(t);buildCitadel(t);}
    private void buildHaven(World t){
        buildCobbleSquare(t,0,-38,82); buildPath(t,0,-128,0,GATE_Z-80,6,Material.POLISHED_ANDESITE); buildPath(t,-125,-62,125,-62,6,Material.STONE_BRICKS); buildPath(t,-105,8,105,8,4,Material.STONE_BRICKS); buildPath(t,-95,52,95,52,4,Material.POLISHED_ANDESITE);
        buildFountain(t,0,-46); buildGreatHall(t,-62,-106);
        buildBuilding(t,16,75,-106,45,32,"REN'S MARKET",Material.SPRUCE_PLANKS,Material.DARK_OAK_PLANKS,Material.SPRUCE_LOG);
        buildBuilding(t,-122,75,-50,34,28,"HAVEN INN",Material.OAK_PLANKS,Material.SPRUCE_PLANKS,Material.OAK_LOG);
        buildBuilding(t,88,75,-50,34,28,"BLACKSMITH",Material.STONE_BRICKS,Material.DEEPSLATE_TILES,Material.DEEPSLATE);
        buildBuilding(t,-122,75,12,34,28,"GUILD HALL",Material.DARK_OAK_PLANKS,Material.DEEPSLATE_TILES,Material.DARK_OAK_LOG);
        buildBuilding(t,88,75,12,34,28,"STABLES",Material.OAK_PLANKS,Material.OAK_SLAB,Material.OAK_LOG);
        buildBuilding(t,-54,75,42,36,24,"FLETCHER'S YARD",Material.SPRUCE_PLANKS,Material.SPRUCE_SLAB,Material.SPRUCE_LOG);
        buildBuilding(t,12,75,42,36,24,"FARMWARD",Material.OAK_PLANKS,Material.OAK_SLAB,Material.OAK_LOG);
        buildBuilding(t,50,75,-12,38,28,"TOWER ARCHIVE",Material.STONE_BRICKS,Material.POLISHED_DEEPSLATE,Material.OAK_LOG);
        buildBuilding(t,-88,75,-12,30,28,"HERBALIST",Material.MOSS_BLOCK,Material.OAK_SLAB,Material.OAK_LOG);
        buildTownWall(t); buildGatehouse(t,0,-132,true); buildGatehouse(t,0,76,false); buildWatchtower(t,-150,-127);buildWatchtower(t,150,-127);buildWatchtower(t,-150,70);buildWatchtower(t,150,70); buildMarketStalls(t);buildStreetTrees(t);buildHavenLanterns(t);buildSign(t,0,85,-94,"HAVEN","Safe ground beneath the Tower");
    }
    private void buildGreatHall(World t,int x,int z){buildBuilding(t,x,75,z,76,34,"WARDEN HALL",Material.STONE_BRICKS,Material.DEEPSLATE_TILES,Material.OAK_LOG);buildSpire(t,x-8,76,z+14);buildSpire(t,x+76,76,z+14);buildSpire(t,x+38,80,z+17);}
    private void buildNorthRoad(World t){for(int z=110;z<GATE_Z-60;z+=24){int y=terrainYAt(0,z)+1;t.getBlockAt(-10,y,z).setType(Material.SPRUCE_FENCE);t.getBlockAt(-10,y+1,z).setType(Material.LANTERN);t.getBlockAt(10,y,z).setType(Material.SPRUCE_FENCE);t.getBlockAt(10,y+1,z).setType(Material.LANTERN);}}
    private void buildWilderness(World t){for(int[]p:new int[][]{{-55,180},{65,255},{-78,360},{80,470}})buildForestCamp(t,p[0],p[1]);buildRiverBridge(t,0,315);buildRuins(t,-28,565);buildRuins(t,35,635);buildWatchtower(t,-50,500);buildWatchtower(t,50,540);buildSign(t,0,terrainYAt(0,155)+5,155,"WHISPERING WOODS","Hunt grounds");buildSign(t,0,terrainYAt(0,565)+7,565,"THE OLD ROAD","Ruins from before Haven");}
    private void buildFirstGate(World t){int y=terrainYAt(0,GATE_Z);buildPath(t,0,GATE_Z-90,0,GATE_Z+16,8,Material.POLISHED_DEEPSLATE);for(int s:new int[]{-1,1}){int x=s*50;buildWatchtower(t,x,GATE_Z-20);buildSpire(t,x,y+1,GATE_Z-20);}for(int x=-38;x<=38;x++)for(int h=1;h<=23;h++){if(Math.abs(x)<=9&&h<=12)continue;t.getBlockAt(x,y+h,GATE_Z).setType(h%6==0?Material.POLISHED_DEEPSLATE:Material.DEEPSLATE_BRICKS);}for(int x=-9;x<=9;x++)for(int h=13;h<=23;h++)t.getBlockAt(x,y+h,GATE_Z).setType(Material.IRON_BARS);buildSign(t,0,y+27,GATE_Z-4,"THE FIRST GATE","Warden of the First Gate");}
    private void buildCitadel(World t){int z=960,y=terrainYAt(0,z)+1;buildSpire(t,0,y,z);buildSpire(t,-70,y,z+10);buildSpire(t,70,y,z+10);buildSign(t,0,y+24,z-6,"THE ASCENSION CITADEL","The Tower remembers the worthy");}
    private void buildCobbleSquare(World t,int cx,int cz,int r){for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(dx*dx+dz*dz<=r*r)t.getBlockAt(cx+dx,73,cz+dz).setType((Math.abs(dx)+Math.abs(dz))%9==0?Material.STONE_BRICKS:Material.COBBLESTONE);}
    private void buildTownWall(World t){for(int x=-158;x<=158;x++)for(int h=73;h<=81;h++){t.getBlockAt(x,h,-138).setType(Material.STONE_BRICKS);t.getBlockAt(x,h,76).setType(Material.STONE_BRICKS);}for(int z=-138;z<=76;z++)for(int h=73;h<=81;h++){t.getBlockAt(-158,h,z).setType(Material.STONE_BRICKS);t.getBlockAt(158,h,z).setType(Material.STONE_BRICKS);}}
    private void buildGatehouse(World t,int x,int z,boolean south){int y=73;for(int s:new int[]{-1,1})for(int dx=0;dx<13;dx++)for(int dz=0;dz<11;dz++)for(int h=0;h<15;h++)t.getBlockAt(x+s*(14+dx),y+h,z+(south?-dz:dz)).setType(Material.STONE_BRICKS);for(int s:new int[]{-1,1})for(int dx=0;dx<10;dx++)for(int dz=0;dz<8;dz++)for(int h=1;h<13;h++)t.getBlockAt(x+s*(16+dx),y+h,z+(south?-dz:dz)).setType(Material.AIR);}
    private void buildBuilding(World t,int x,int y,int z,int width,int depth,String label,Material wall,Material roof,Material frame){for(int px=x;px<x+width;px++)for(int pz=z;pz<z+depth;pz++)t.getBlockAt(px,y,pz).setType(wall);for(int px=x;px<x+width;px++)for(int h=1;h<=8;h++){t.getBlockAt(px,y+h,z).setType(frame);t.getBlockAt(px,y+h,z+depth-1).setType(frame);}for(int pz=z;pz<z+depth;pz++)for(int h=1;h<=8;h++){t.getBlockAt(x,y+h,pz).setType(frame);t.getBlockAt(x+width-1,y+h,pz).setType(frame);}for(int px=x+1;px<x+width-1;px++)for(int pz=z+1;pz<z+depth-1;pz++)for(int h=1;h<=7;h++)t.getBlockAt(px,y+h,pz).setType(Material.AIR);for(int layer=0;layer<3;layer++)for(int px=x+layer;px<x+width-layer;px++)for(int pz=z+layer;pz<z+depth-layer;pz++)t.getBlockAt(px,y+9+layer,pz).setType(roof);t.getBlockAt(x+width/2,y+1,z).setType(Material.AIR);t.getBlockAt(x+width/2,y+2,z).setType(Material.AIR);buildSign(t,x+2,y+11,z-1,label,"");}
    private void buildWatchtower(World t,int x,int z){buildSpire(t,x,terrainYAt(x,z)+1,z);}
    private void buildSpire(World t,int x,int y,int z){for(int px=x-5;px<=x+5;px++)for(int pz=z-5;pz<=z+5;pz++)for(int h=0;h<=13;h++)t.getBlockAt(px,y+h,pz).setType(Material.STONE_BRICKS);for(int px=x-3;px<=x+3;px++)for(int pz=z-3;pz<=z+3;pz++)for(int h=1;h<=11;h++)t.getBlockAt(px,y+h,pz).setType(Material.AIR);for(int l=0;l<5;l++)for(int px=x-l;px<=x+l;px++)for(int pz=z-l;pz<=z+l;pz++)t.getBlockAt(px,y+14+l,pz).setType(Material.DEEPSLATE_TILES);t.getBlockAt(x,y+20,z).setType(Material.LANTERN);}
    private void buildFountain(World t,int x,int z){for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++)if(dx*dx+dz*dz<=100)t.getBlockAt(x+dx,75,z+dz).setType(Material.STONE_BRICKS);for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)t.getBlockAt(x+dx,76,z+dz).setType(Material.WATER);for(int h=77;h<=82;h++)t.getBlockAt(x,h,z).setType(Material.QUARTZ_BLOCK);}
    private void buildMarketStalls(World t){for(int[]p:new int[][]{{-40,-38},{-16,-38},{10,-38},{36,-38},{-40,-22},{-16,-22},{10,-22},{36,-22}}){for(int dx=-3;dx<=3;dx++)t.getBlockAt(p[0]+dx,77,p[1]).setType(Material.SPRUCE_PLANKS);for(int dx=-3;dx<=3;dx++){t.getBlockAt(p[0]+dx,81,p[1]-1).setType(Material.RED_WOOL);t.getBlockAt(p[0]+dx,81,p[1]).setType(Material.WHITE_WOOL);}}}
    private void buildStreetTrees(World t){for(int x=-132;x<=132;x+=24){buildTree(t,x,-125);buildTree(t,x,62);}}
    private void buildTree(World t,int x,int z){int y=terrainYAt(x,z)+1;for(int h=0;h<5;h++)t.getBlockAt(x,y+h,z).setType(Material.OAK_LOG);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<=2;dy++)if(Math.abs(dx)+Math.abs(dz)+dy<5)t.getBlockAt(x+dx,y+5+dy,z+dz).setType(Material.OAK_LEAVES);}
    private void buildHavenLanterns(World t){for(int z=-120;z<=70;z+=14){int y=terrainYAt(0,z)+1;t.getBlockAt(-8,y,z).setType(Material.SPRUCE_FENCE);t.getBlockAt(-8,y+1,z).setType(Material.LANTERN);t.getBlockAt(8,y,z).setType(Material.SPRUCE_FENCE);t.getBlockAt(8,y+1,z).setType(Material.LANTERN);}}
    private void buildForestCamp(World t,int x,int z){int y=terrainYAt(x,z)+1;for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=5;dz++)t.getBlockAt(x+dx,y,z+dz).setType(Material.COARSE_DIRT);t.getBlockAt(x,y+1,z).setType(Material.CAMPFIRE);buildTree(t,x-7,z+6);buildTree(t,x+7,z+6);}
    private void buildRiverBridge(World t,int x,int z){int y=terrainYAt(x,z)+1;buildPath(t,x-55,z,x+55,z,4,Material.SPRUCE_PLANKS);for(int px=x-50;px<=x+50;px+=10){t.getBlockAt(px,y-2,z).setType(Material.DARK_OAK_LOG);t.getBlockAt(px,y+2,z-4).setType(Material.SPRUCE_FENCE);t.getBlockAt(px,y+2,z+4).setType(Material.SPRUCE_FENCE);}}
    private void buildRuins(World t,int x,int z){int y=terrainYAt(x,z)+1;for(int i=-4;i<=4;i++){int px=x+i*9;for(int h=0;h<6+(Math.abs(i)%3);h++)t.getBlockAt(px,y+h,z+5).setType(h%3==0?Material.MOSSY_STONE_BRICKS:Material.STONE_BRICKS);}buildTree(t,x-10,z+7);buildTree(t,x+11,z+4);}
    private void buildSign(World t,int x,int y,int z,String title,String sub){t.getBlockAt(x,y,z).setType(Material.OAK_SIGN);if(t.getBlockAt(x,y,z).getState() instanceof org.bukkit.block.Sign s){s.line(0,net.kyori.adventure.text.Component.text(title));s.line(1,net.kyori.adventure.text.Component.text(sub));s.update();}}
    private void buildPath(World t,int x1,int z1,int x2,int z2,int radius,Material material){int sx=Integer.signum(x2-x1),sz=Integer.signum(z2-z1),steps=0,x=x1,z=z1;while(true){int y=terrainYAt(x,z);for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)t.getBlockAt(x+dx,y,z+dz).setType(material);if(x==x2&&z==z2)break;if(x!=x2)x+=sx;if(z!=z2)z+=sz;if(++steps>12000)break;}}
    private void ensureNpcs(){ensureNpc(QuestService.LYRA,"§6✦ Warden Lyra",location(-26,77,-91),Villager.Profession.ARMORER);ensureNpc(QuestService.NPC_REN,"§a✦ Merchant Ren",location(42,77,-91),Villager.Profession.FLETCHER);ensureNpc("ascension:innkeeper_mara","§dInnkeeper Mara",location(-105,78,-39),Villager.Profession.LIBRARIAN);ensureNpc("ascension:blacksmith_dain","§cBlacksmith Dain",location(105,78,-39),Villager.Profession.TOOLSMITH);ensureNpc("ascension:guide_elian","§bGuide Elian",location(0,78,22),Villager.Profession.CARTOGRAPHER);ensureNpc("ascension:stablemaster_kael","§eStablemaster Kael",location(105,78,18),Villager.Profession.FARMER);ensureNpc("ascension:captain_soren","§9Captain Soren",location(0,79,61),Villager.Profession.LIBRARIAN);}
    private void ensureNpc(final String id,final String name,final Location loc,final Villager.Profession profession){for(Entity e:world.getNearbyEntities(loc,12,8,12))if(id.equals(e.getPersistentDataContainer().get(npcKey,PersistentDataType.STRING)))return;Villager v=(Villager)world.spawnEntity(loc,EntityType.VILLAGER);v.getPersistentDataContainer().set(npcKey,PersistentDataType.STRING,id);v.setCustomName(name);v.setCustomNameVisible(true);v.setAI(false);v.setInvulnerable(true);v.setSilent(true);v.setCollidable(false);v.setPersistent(true);v.setProfession(profession);}
    private void spawnAuthoredMob(final String id,final int x,final int z){mobs.spawnMob(AssetId.parse(id),location(x,terrainYAt(x,z)+1,z));}
    private Location havenSpawn(){return location(0,terrainYAt(0,-90)+2,-90);}
    private Location location(int x,int y,int z){return new Location(world,x,y,z);}
    private int terrainYAt(int x,int z){double n=72.0+smoothNoise(x/520.0,z/520.0)*26.0+smoothNoise(x/180.0,z/180.0)*15.0+smoothNoise(x/62.0,z/62.0)*4.0;if(Math.sqrt((double)x*x+(double)z*z)<150)n=72.0;double north=Math.max(0,Math.min(1,(-z-850.0)/900.0));n+=north*north*55.0;double river=Math.abs(z-(260.0+Math.sin(x/310.0)*180.0+Math.sin(x/97.0)*34.0));n-=Math.max(0,1-river/110.0)*18;return Math.max(59,(int)Math.round(n));}
    private static double smoothNoise(double x,double z){int x0=(int)Math.floor(x),z0=(int)Math.floor(z);double tx=x-x0,tz=z-z0;tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);return lerp(lerp(valueNoise(x0,z0),valueNoise(x0+1,z0),tx),lerp(valueNoise(x0,z0+1),valueNoise(x0+1,z0+1),tx),tz);}
    private static double valueNoise(int x,int z){long h=0x9E3779B97F4A7C15L;h^=(long)x*0xBF58476D1CE4E5B9L;h=Long.rotateLeft(h,27);h^=(long)z*0x94D049BB133111EBL;h^=h>>>30;h*=0xBF58476D1CE4E5B9L;h^=h>>>27;h*=0x94D049BB133111EBL;return ((h^(h>>>31))&0xFFFFFFL)/8388607.5D-1;}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private void register(){plugin.getServer().getPluginManager().registerEvents(this,plugin);}
    private boolean hasCurrentBuild(){try{return Files.exists(buildMarker)&&WORLD_VERSION.equals(Files.readString(buildMarker).trim());}catch(IOException e){return false;}}
    private void markBuild(){try{Files.createDirectories(buildMarker.getParent());Files.writeString(buildMarker,WORLD_VERSION);}catch(IOException e){throw new IllegalStateException("Could not mark Floor 1 build",e);}}
    private void migrateLegacyWorld(){if(hasCurrentBuild())return;World existing=Bukkit.getWorld(WORLD_NAME);if(existing!=null)Bukkit.unloadWorld(existing,false);Path dir=plugin.getServer().getWorldContainer().toPath().resolve(WORLD_NAME);if(!Files.exists(dir))return;try(var s=Files.walk(dir)){s.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}catch(IOException e){plugin.getLogger().warning("Floor 1 migration incomplete: "+e.getMessage());}}
}
