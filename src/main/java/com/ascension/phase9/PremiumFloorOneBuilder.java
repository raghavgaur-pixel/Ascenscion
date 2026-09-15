package com.ascension.phase9;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Stairs;

/**
 * Premium authored architecture kit for Floor 1. Uses layered silhouettes,
 * pitched roofs, buttresses, framed windows, landscaping and multiple city
 * districts rather than primitive box structures.
 */
public final class PremiumFloorOneBuilder {
    public static final String PREMIUM_VERSION = "floor_001_premium_v1";
    private static final int GROUND = 72;
    private static final int MIN_X = -156;
    private static final int MAX_X = 156;
    private static final int MIN_Z = -156;
    private static final int MAX_Z = 118;
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final Path marker;

    public PremiumFloorOneBuilder(final org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
        this.marker = plugin.getDataFolder().toPath().resolve(".floor_001_premium");
    }

    public void prepareFreshWorld() {
        if (isCurrent()) return;
        final World existing = Bukkit.getWorld(FloorOneWorldService.WORLD_NAME);
        if (existing != null) Bukkit.unloadWorld(existing, false);
        deleteTree(plugin.getServer().getWorldContainer().toPath().resolve(FloorOneWorldService.WORLD_NAME));
        final Path legacy = plugin.getDataFolder().toPath().resolve(".floor_001_provisioned");
        try {
            Files.createDirectories(legacy.getParent());
            // Prevent the legacy rectangular builder from running after we replace the world.
            Files.writeString(legacy, "floor_001_world_v8");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare Floor 1 build markers", exception);
        }
    }

    public void build() {
        if (isCurrent()) return;
        final World world = Bukkit.getWorld(FloorOneWorldService.WORLD_NAME);
        if (world == null) throw new IllegalStateException("Floor 1 world is not loaded");
        buildHaven(world);
        buildWildernessLandmarks(world);
        buildFirstGate(world);
        buildCitadel(world);
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, PREMIUM_VERSION);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist premium Floor 1 marker", exception);
        }
    }

    private boolean isCurrent() {
        try { return Files.exists(marker) && PREMIUM_VERSION.equals(Files.readString(marker).trim()); }
        catch (IOException ignored) { return false; }
    }

    private void buildHaven(final World w) {
        pave(w, MIN_X + 8, MIN_Z + 8, MAX_X - 8, MAX_Z - 8, Material.GRASS_BLOCK);
        buildOuterWall(w);
        buildGatehouse(w, 0, -150, true);
        buildGatehouse(w, 0, 112, false);
        buildMainBoulevard(w);
        buildPlaza(w, 0, -28, 34);

        // Civic spine and skyline landmarks.
        buildCastle(w, -68, 37);
        buildGuildHall(w, -120, -57);
        buildInn(w, -28, 57);
        buildBlacksmith(w, 63, 57);
        buildMarketHall(w, 24, -88);
        buildArchive(w, -120, 8);
        buildStable(w, 90, 8);

        final int[][] homes = {
            {-122,-7,24,22,0},{-91,-7,25,23,1},{-59,-7,23,22,2},
            {69,-7,25,23,3},{100,-7,24,22,4},{-123,24,24,22,5},
            {-92,26,22,24,0},{70,27,23,23,1},{101,29,24,24,2},
            {-122,55,24,22,3},{-92,59,23,22,4},{104,59,24,22,5},
            {-8,17,22,20,2},{23,19,25,22,4},{-8,88,24,23,1},
            {21,88,23,24,3},{-51,91,25,22,0},{52,91,26,23,5}
        };
        for (int[] home : homes) buildHouse(w, home[0], home[1], home[2], home[3], home[4]);

        buildMarketStalls(w);
        buildGardens(w);
        buildTreesAndLandscaping(w);
        buildLanternPosts(w);
        buildTownSignage(w);
    }

    private void buildMainBoulevard(final World w) {
        path(w, 0, -139, 0, 105, 7, Material.POLISHED_ANDESITE);
        path(w, -139, -28, 139, -28, 5, Material.STONE_BRICKS);
        path(w, -95, 10, 95, 10, 4, Material.COBBLESTONE);
        path(w, -72, 66, 72, 66, 4, Material.STONE_BRICKS);
        path(w, -127, 39, -76, 39, 3, Material.POLISHED_ANDESITE);
        path(w, 76, 39, 127, 39, 3, Material.POLISHED_ANDESITE);
        path(w, -128, 78, -10, 78, 3, Material.GRAVEL);
        path(w, 10, 78, 128, 78, 3, Material.GRAVEL);
    }

    private void buildPlaza(final World w, final int cx, final int cz, final int r) {
        for (int dx=-r;dx<=r;dx++) for (int dz=-r;dz<=r;dz++) if (dx*dx+dz*dz<=r*r) {
            Material m = ((dx*dx + dz*dz) % 17 == 0) ? Material.POLISHED_ANDESITE : Material.STONE_BRICKS;
            w.getBlockAt(cx+dx, GROUND+1, cz+dz).setType(m);
        }
        ring(w,cx,cz,31,GROUND+2,Material.CHISELED_STONE_BRICKS);
        buildFountain(w,cx,cz);
        for(int a=0;a<10;a++){
            double ang=a*Math.PI*2/10.0;
            buildLamp(w,cx+(int)Math.round(Math.cos(ang)*27),cz+(int)Math.round(Math.sin(ang)*27),6);
        }
    }

    private void buildFountain(final World w,int x,int z){
        ring(w,x,z,10,GROUND+2,Material.STONE_BRICKS);
        ring(w,x,z,8,GROUND+3,Material.POLISHED_ANDESITE);
        for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++)if(dx*dx+dz*dz<=49)w.getBlockAt(x+dx,GROUND+4,z+dz).setType(Material.WATER);
        ring(w,x,z,4,GROUND+5,Material.QUARTZ_BLOCK);
        for(int h=5;h<=11;h++)w.getBlockAt(x,h+GROUND,z).setType(Material.QUARTZ_BLOCK);
        w.getBlockAt(x,GROUND+11,z).setType(Material.WATER);
        ring(w,x,z,13,GROUND+2,Material.OAK_LEAVES);
    }

    private void buildOuterWall(final World w){
        for(int x=MIN_X;x<=MAX_X;x++){
            wallColumn(w,x,MIN_Z,11); wallColumn(w,x,MAX_Z,11);
        }
        for(int z=MIN_Z;z<=MAX_Z;z++){
            wallColumn(w,MIN_X,z,11); wallColumn(w,MAX_X,z,11);
        }
        int[][] t={{MIN_X,MIN_Z},{MAX_X,MIN_Z},{MIN_X,MAX_Z},{MAX_X,MAX_Z},{0,MIN_Z},{0,MAX_Z}};
        for(int[] p:t)buildWallTower(w,p[0],p[1]);
    }

    private void wallColumn(final World w,int x,int z,int height){
        for(int y=GROUND+1;y<=GROUND+height;y++){
            w.getBlockAt(x,y,z).setType(y==GROUND+height?Material.STONE_BRICK_SLAB:(y%4==0?Material.POLISHED_ANDESITE:Material.STONE_BRICKS));
        }
        for(int y=GROUND+height-2;y<=GROUND+height;y++)w.getBlockAt(x+(x<0?1:-1),y,z).setType(Material.COBBLESTONE_WALL);
    }

    private void buildGatehouse(final World w,int x,int z,boolean south){
        int dz=south?1:-1;
        buildWallTower(w,x-18,z); buildWallTower(w,x+18,z);
        for(int px=x-16;px<=x+16;px++)for(int y=GROUND+1;y<=GROUND+12;y++){
            if(Math.abs(px-x)<=6 && y<GROUND+9)continue;
            w.getBlockAt(px,y,z+dz).setType(Material.DEEPSLATE_BRICKS);
        }
        for(int px=x-6;px<=x+6;px++)for(int y=GROUND+1;y<GROUND+9;y++)w.getBlockAt(px,y,z+dz).setType(Material.AIR);
        for(int px=x-8;px<=x+8;px++)w.getBlockAt(px,GROUND+10,z+dz).setType(Material.DEEPSLATE_TILE_SLAB);
        banner(w,x-10,GROUND+7,z+dz,Material.BLUE_WOOL); banner(w,x+10,GROUND+7,z+dz,Material.GOLD_BLOCK);
    }

    private void buildWallTower(final World w,int x,int z){
        final int r=7;
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(dx*dx+dz*dz<=r*r)for(int y=GROUND+1;y<=GROUND+16;y++)w.getBlockAt(x+dx,y,z+dz).setType(y%3==0?Material.POLISHED_DEEPSLATE:Material.STONE_BRICKS);
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(dx*dx+dz*dz<=16)for(int y=GROUND+2;y<=GROUND+14;y++)w.getBlockAt(x+dx,y,z+dz).setType(Material.AIR);
        for(int s=0;s<7;s++)ring(w,x,z,Math.max(1,6-s),GROUND+17+s,Material.DEEPSLATE_TILES);
        w.getBlockAt(x,GROUND+24,z).setType(Material.SOUL_LANTERN);
    }

    private void buildCastle(final World w,int x,int z){
        buildStoneManor(w,x,z,87,49);
        buildRoundTower(w,x+4,z+4,13,27);
        buildRoundTower(w,x+83,z+4,13,27);
        buildRoundTower(w,x+4,z+45,13,27);
        buildRoundTower(w,x+83,z+45,13,27);
        buildKeep(w,x+25,z+9,37,31);
        buildArchedEntrance(w,x+43,GROUND+1,z-1);
        for(int i=0;i<6;i++)banner(w,x+14+i*14,GROUND+14,z-1,i%2==0?Material.BLUE_WOOL:Material.PURPLE_WOOL);
        buildTerraceGarden(w,x+12,z+55);
    }

    private void buildStoneManor(final World w,int x,int z,int width,int depth){
        final int y=GROUND+1;
        shell(w,x,z,width,depth,12,Material.STONE_BRICKS,Material.CHISELED_STONE_BRICKS);
        for(int px=x+6;px<x+width-5;px+=11)buttress(w,px,z,y+11);
        pitchedRoof(w,x,z,width,depth,y+12,7,Material.DEEPSLATE_TILE_STAIRS);
        for(int px=x+9;px<x+width-8;px+=15){window(w,px,y+5,z-1,Material.LIGHT_BLUE_STAINED_GLASS_PANE,4);window(w,px,y+5,z+depth,Material.LIGHT_BLUE_STAINED_GLASS_PANE,4);}
    }

    private void buildKeep(final World w,int x,int z,int width,int depth){
        final int y=GROUND+1;
        shell(w,x,z,width,depth,18,Material.DEEPSLATE_BRICKS,Material.POLISHED_DEEPSLATE);
        pitchedRoof(w,x,z,width,depth,y+18,10,Material.DEEPSLATE_TILE_STAIRS);
        for(int px=x+6;px<x+width-6;px+=9)window(w,px,y+7,z-1,Material.WHITE_STAINED_GLASS_PANE,5);
        for(int py=7;py<=15;py+=4){w.getBlockAt(x+width/2,y+py,z-1).setType(Material.IRON_BARS);}
    }

    private void shell(final World w,int x,int z,int width,int depth,int height,Material wall,Material frame){
        final int y=GROUND+1;
        for(int px=x;px<x+width;px++)for(int pz=z;pz<z+depth;pz++)w.getBlockAt(px,y,pz).setType(wall);
        for(int h=1;h<=height;h++){
            for(int px=x;px<x+width;px++){w.getBlockAt(px,y+h,z).setType(frame);w.getBlockAt(px,y+h,z+depth-1).setType(frame);}
            for(int pz=z;pz<z+depth;pz++){w.getBlockAt(x,y+h,pz).setType(frame);w.getBlockAt(x+width-1,y+h,pz).setType(frame);}
        }
        for(int px=x+1;px<x+width-1;px++)for(int pz=z+1;pz<z+depth-1;pz++)for(int h=1;h<height;h++)w.getBlockAt(px,y+h,pz).setType(Material.AIR);
    }

    private void buildRoundTower(final World w,int x,int z,int radius,int height){
        final int y=GROUND+1;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)if(dx*dx+dz*dz<=radius*radius){
            for(int h=0;h<height;h++)w.getBlockAt(x+dx,y+h,z+dz).setType(h%4==0?Material.POLISHED_ANDESITE:Material.STONE_BRICKS);
            if(dx*dx+dz*dz<(radius-3)*(radius-3))for(int h=1;h<height-2;h++)w.getBlockAt(x+dx,y+h,z+dz).setType(Material.AIR);
        }
        for(int r=0;r<radius;r++)ring(w,x,z,Math.max(1,radius-1-r),y+height+r,Material.DEEPSLATE_TILES);
        w.getBlockAt(x,y+height+radius+2,z).setType(Material.LANTERN);
    }

    private void buildGuildHall(World w,int x,int z){buildHouse(w,x,z,36,31,4);buildSign(w,x+2,GROUND+11,z-1,"GUILD HALL","Adventurers assemble");}
    private void buildInn(World w,int x,int z){buildHouse(w,x,z,37,30,1);buildSign(w,x+2,GROUND+11,z-1,"HAVEN INN","Rooms • Food • Rest");}
    private void buildBlacksmith(World w,int x,int z){buildHouse(w,x,z,35,29,3);buildSign(w,x+2,GROUND+11,z-1,"BLACKSMITH","Steel • Repairs • Upgrades");}
    private void buildMarketHall(World w,int x,int z){buildHouse(w,x,z,42,33,5);buildSign(w,x+2,GROUND+11,z-1,"REN'S MARKET","Trade • Supplies • Rumors");}
    private void buildArchive(World w,int x,int z){buildHouse(w,x,z,31,27,0);buildSign(w,x+2,GROUND+11,z-1,"TOWER ARCHIVE","Maps • Lore • Records");}
    private void buildStable(World w,int x,int z){buildHouse(w,x,z,35,29,2);buildSign(w,x+2,GROUND+11,z-1,"HAVEN STABLES","Mounts • Caravans");}

    private void buildHouse(final World w,int x,int z,int width,int depth,int palette){
        final int y=GROUND+1;
        Material wall=switch(palette%6){case 0->Material.OAK_PLANKS;case 1->Material.SPRUCE_PLANKS;case 2->Material.DARK_OAK_PLANKS;case 3->Material.BRICKS;case 4->Material.CALCITE;default->Material.COBBLED_DEEPSLATE;};
        Material roof=switch(palette%6){case 0->Material.SPRUCE_STAIRS;case 1->Material.DARK_OAK_STAIRS;case 2->Material.PURPUR_STAIRS;case 3->Material.BRICK_STAIRS;case 4->Material.WARPED_STAIRS;default->Material.DEEPSLATE_TILE_STAIRS;};
        Material frame=(palette%3==0)?Material.DARK_OAK_LOG:Material.SPRUCE_LOG;
        Material glass=(palette%2==0)?Material.LIGHT_BLUE_STAINED_GLASS_PANE:Material.WHITE_STAINED_GLASS_PANE;
        for(int px=x-1;px<x+width+1;px++)for(int pz=z-1;pz<z+depth+1;pz++)w.getBlockAt(px,y-1,pz).setType(Material.STONE_BRICKS);
        shell(w,x,z,width,depth,7,wall,frame);
        for(int px=x+4;px<x+width-4;px+=7){window(w,px,y+4,z-1,glass,3);window(w,px,y+4,z+depth,glass,3);}
        for(int pz=z+5;pz<z+depth-5;pz+=7){window(w,x-1,y+4,pz,glass,3);window(w,x+width,y+4,pz,glass,3);}
        doorway(w,x+width/2,y,z-1,BlockFace.SOUTH);
        for(int dx=-2;dx<=2;dx++){w.getBlockAt(x+width/2+dx,y+3,z-1).setType(Material.SPRUCE_SLAB);}
        w.getBlockAt(x+width/2-1,y+4,z-1).setType(Material.LANTERN);
        planter(w,x+width/2-3,y+1,z-1);planter(w,x+width/2+3,y+1,z-1);
        pitchedRoof(w,x,z,width,depth,y+7,6,roof);
        chimney(w,x+3,z+depth/2,y+7);
        if(width>=23)pitchedDormer(w,x+width/2,y+8,z+depth/2,roof);
        for(int px=x+4;px<x+width-4;px+=10)buttress(w,px,z,y+6);
    }

    private void pitchedRoof(final World w,int x,int z,int width,int depth,int baseY,int rise,Material mat){
        final int half=Math.max(2,depth/2);
        for(int offset=0;offset<half;offset++){
            int yy=baseY+Math.min(offset,rise);
            int f=z-1+offset,b=z+depth-offset;
            for(int px=x-1;px<=x+width;px++){stair(w,px,yy,f,mat,BlockFace.SOUTH);stair(w,px,yy,b,mat,BlockFace.NORTH);}
        }
        for(int px=x-1;px<=x+width;px++)w.getBlockAt(px,baseY+Math.min(half-1,rise)+1,z+depth/2).setType(mat);
    }

    private void pitchedDormer(final World w,int x,int y,int z,Material roof){
        for(int dx=-3;dx<=3;dx++)for(int h=0;h<=3;h++)w.getBlockAt(x+dx,y+h,z).setType(Material.DARK_OAK_LOG);
        for(int dx=-2;dx<=2;dx++)w.getBlockAt(x+dx,y+1,z-1).setType(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        stair(w,x-3,y+3,z,roof,BlockFace.SOUTH);stair(w,x+3,y+3,z,roof,BlockFace.SOUTH);
        for(int dx=-2;dx<=2;dx++)w.getBlockAt(x+dx,y+4,z).setType(roof);
    }

    private void buildMarketStalls(final World w){
        stall(w,-42,-45,Material.RED_WOOL);stall(w,-14,-52,Material.ORANGE_WOOL);stall(w,14,-45,Material.YELLOW_WOOL);stall(w,43,-52,Material.GREEN_WOOL);
        stall(w,-42,-15,Material.BLUE_WOOL);stall(w,-14,-7,Material.PURPLE_WOOL);stall(w,14,-15,Material.WHITE_WOOL);stall(w,43,-7,Material.RED_WOOL);
    }

    private void stall(final World w,int x,int z,Material canopy){
        for(int dx=-3;dx<=3;dx++){w.getBlockAt(x+dx,GROUND+1,z).setType(Material.SPRUCE_PLANKS);w.getBlockAt(x+dx,GROUND+4,z-1).setType(canopy);}
        for(int dz=-1;dz<=1;dz++)for(int h=1;h<=4;h++){w.getBlockAt(x-3,GROUND+h,z+dz).setType(Material.SPRUCE_FENCE);w.getBlockAt(x+3,GROUND+h,z+dz).setType(Material.SPRUCE_FENCE);}
        w.getBlockAt(x,GROUND+2,z).setType(Material.BARREL);w.getBlockAt(x,GROUND+3,z-1).setType(Material.LANTERN);
    }

    private void buildGardens(final World w){
        for(int[] g:new int[][]{{-51,31},{51,31},{-55,77},{55,77},{-113,-96},{113,-96}}){
            for(int dx=-7;dx<=7;dx++)for(int dz=-6;dz<=6;dz++)if(dx*dx+dz*dz<55)w.getBlockAt(g[0]+dx,GROUND+1,g[1]+dz).setType(Material.GRASS_BLOCK);
            for(int i=-4;i<=4;i+=2){w.getBlockAt(g[0]+i,GROUND+2,g[1]).setType(Material.SWEET_BERRY_BUSH);w.getBlockAt(g[0],GROUND+2,g[1]+i).setType(Material.FLOWERING_AZALEA);}
            for(int i=-6;i<=6;i+=3){w.getBlockAt(g[0]+i,GROUND+2,g[1]-5).setType(Material.OAK_FENCE);w.getBlockAt(g[0]+i,GROUND+2,g[1]+5).setType(Material.OAK_FENCE);}
        }
    }

    private void buildTreesAndLandscaping(final World w){
        Random random=new Random(910204L);
        for(int i=0;i<110;i++){
            int x=MIN_X+8+random.nextInt(MAX_X-MIN_X-15),z=MIN_Z+8+random.nextInt(MAX_Z-MIN_Z-15);
            if(Math.abs(x)<76 && Math.abs(z+28)<76)continue;
            tree(w,x,z,5+random.nextInt(3),random.nextBoolean()?Material.OAK_LOG:Material.SPRUCE_LOG);
        }
        for(int x=MIN_X+8;x<=MAX_X-8;x+=22)for(int dz=-2;dz<=2;dz++)w.getBlockAt(x,GROUND+2,95+dz).setType(Material.OAK_LEAVES);
        for(int z=MIN_Z+8;z<=MAX_Z-8;z+=18){w.getBlockAt(-145,GROUND+2,z).setType(Material.OAK_LEAVES);w.getBlockAt(145,GROUND+2,z).setType(Material.OAK_LEAVES);}
    }

    private void buildLanternPosts(final World w){
        for(int z=MIN_Z+12;z<=MAX_Z-12;z+=14){lampPost(w,-8,z);lampPost(w,8,z);}
        for(int x=-126;x<=126;x+=18)lampPost(w,x,-28);
    }

    private void buildTownSignage(final World w){
        buildSign(w,0,GROUND+17,-126,"HAVEN","The first safe settlement beneath the Tower");
        buildSign(w,0,GROUND+15,104,"NORTH ROAD","The First Gate • 760 blocks");
        buildSign(w,-143,GROUND+12,-28,"MARKET DISTRICT","Trade • Supplies • Rumors");
        buildSign(w,143,GROUND+12,-28,"WARD DISTRICT","Guild • Archive • Services");
    }

    private void buildWildernessLandmarks(final World w){
        path(w,0,108,0,720,4,Material.COARSE_DIRT);
        for(int z=170;z<=650;z+=120){buildCamp(w,-62,z);buildCamp(w,62,z+42);buildWaystone(w,0,z);}
        buildRiverBridge(w,0,315);buildRuins(w,-36,545);buildRuins(w,44,635);
        buildSign(w,0,GROUND+5,128,"WHISPERING WOODS","First Hunt grounds");
        buildSign(w,0,GROUND+5,548,"THE OLD ROAD","Ruins of a forgotten ascent");
    }

    private void buildCamp(final World w,int x,int z){int y=highest(w,x,z)+1;for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=5;dz++)w.getBlockAt(x+dx,y,z+dz).setType(Material.COARSE_DIRT);w.getBlockAt(x,y+1,z).setType(Material.CAMPFIRE);w.getBlockAt(x+4,y+1,z).setType(Material.CHEST);}
    private void buildWaystone(final World w,int x,int z){int y=highest(w,x,z)+1;for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)w.getBlockAt(x+dx,y,z+dz).setType(Material.POLISHED_DEEPSLATE);for(int h=1;h<=6;h++)w.getBlockAt(x,y+h,z).setType(h==6?Material.AMETHYST_BLOCK:Material.POLISHED_DEEPSLATE);}
    private void buildRiverBridge(final World w,int x,int z){int y=highest(w,x,z)+1;for(int px=x-20;px<=x+20;px++){for(int dz=-2;dz<=2;dz++)w.getBlockAt(px,y,z+dz).setType(Material.SPRUCE_PLANKS);for(int dz=-3;dz<=3;dz++)w.getBlockAt(px,y+3,z+dz).setType(Material.SPRUCE_FENCE);}for(int px=x-18;px<=x+18;px+=6)w.getBlockAt(px,y-2,z).setType(Material.DARK_OAK_LOG);}
    private void buildRuins(final World w,int x,int z){int y=highest(w,x,z)+1;for(int i=-5;i<=5;i++){int h=3+Math.floorMod(i*i,6);for(int yy=0;yy<h;yy++)w.getBlockAt(x+i*5,y+yy,z).setType(yy%2==0?Material.MOSSY_STONE_BRICKS:Material.STONE_BRICKS);}}

    private void buildFirstGate(final World w){int y=highest(w,0,760)+1;path(w,0,675,0,830,8,Material.POLISHED_DEEPSLATE);buildGateTowerAt(w,-44,760,y);buildGateTowerAt(w,44,760,y);for(int x=-38;x<=38;x++)for(int h=0;h<=24;h++){if(Math.abs(x)<=9&&h<13)continue;w.getBlockAt(x,y+h,760).setType(h%5==0?Material.POLISHED_DEEPSLATE:Material.DEEPSLATE_BRICKS);}for(int x=-9;x<=9;x++)for(int h=1;h<13;h++)w.getBlockAt(x,y+h,760).setType(Material.AIR);for(int x=-12;x<=12;x++)w.getBlockAt(x,y+14,760).setType(Material.IRON_BARS);buildSign(w,0,y+27,752,"THE FIRST GATE","The road to the first ascent");}
    private void buildGateTowerAt(final World w,int x,int z,int base){for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++)if(dx*dx+dz*dz<=49)for(int h=0;h<=18;h++)w.getBlockAt(x+dx,base+h,z+dz).setType(h%4==0?Material.POLISHED_DEEPSLATE:Material.DEEPSLATE_BRICKS);for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(dx*dx+dz*dz<=16)for(int h=1;h<=16;h++)w.getBlockAt(x+dx,base+h,z+dz).setType(Material.AIR);for(int r=0;r<7;r++)ring(w,x,z,Math.max(1,6-r),base+19+r,Material.DEEPSLATE_TILES);w.getBlockAt(x,base+27,z).setType(Material.SOUL_LANTERN);}
    private void buildCitadel(final World w){final int z=960,y=highest(w,0,z)+1;buildStoneManor(w,-78,z,156,54);buildRoundTower(w,-70,z+5,15,34);buildRoundTower(w,70,z+5,15,34);buildRoundTower(w,-70,z+45,15,34);buildRoundTower(w,70,z+45,15,34);buildSign(w,0,y+38,z-4,"THE ASCENSION CITADEL","The Tower remembers the worthy");}

    private void doorway(final World w,int x,int y,int z,BlockFace facing){Door d=(Door)Bukkit.createBlockData(Material.DARK_OAK_DOOR);d.setFacing(facing);d.setHalf(Bisected.Half.BOTTOM);w.getBlockAt(x,y,z).setBlockData(d);d=(Door)Bukkit.createBlockData(Material.DARK_OAK_DOOR);d.setFacing(facing);d.setHalf(Bisected.Half.TOP);w.getBlockAt(x,y+1,z).setBlockData(d);}
    private void window(final World w,int x,int y,int z,Material glass,int height){for(int h=0;h<height;h++)w.getBlockAt(x,y+h,z).setType(glass);for(int h=0;h<height;h++){w.getBlockAt(x-1,y+h,z).setType(Material.SPRUCE_TRAPDOOR);w.getBlockAt(x+1,y+h,z).setType(Material.SPRUCE_TRAPDOOR);}}
    private void stair(final World w,int x,int y,int z,Material mat,BlockFace facing){Stairs s=(Stairs)Bukkit.createBlockData(mat);s.setFacing(facing);s.setShape(Stairs.Shape.STRAIGHT);s.setHalf(Bisected.Half.BOTTOM);w.getBlockAt(x,y,z).setBlockData(s);}
    private void buttress(final World w,int x,int z,int y){w.getBlockAt(x,y,z).setType(Material.POLISHED_ANDESITE);w.getBlockAt(x,y+1,z).setType(Material.POLISHED_ANDESITE);w.getBlockAt(x,y+2,z).setType(Material.STONE_BRICK_SLAB);}
    private void planter(final World w,int x,int y,int z){w.getBlockAt(x,y,z).setType(Material.BARREL);w.getBlockAt(x,y+1,z).setType(Material.FLOWERING_AZALEA);}
    private void chimney(final World w,int x,int z,int y){for(int yy=y;yy<=y+5;yy++)w.getBlockAt(x,yy,z).setType(Material.BRICKS);w.getBlockAt(x,y+5,z).setType(Material.CAMPFIRE);}
    private void tree(final World w,int x,int z,int height,Material log){int y=highest(w,x,z)+1;for(int h=0;h<height;h++)w.getBlockAt(x,y+h,z).setType(log);for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=height-2;dy<=height+2;dy++)if(Math.abs(dx)+Math.abs(dz)+Math.max(0,dy-(height-2))<=6)w.getBlockAt(x+dx,y+dy,z+dz).setType(Material.OAK_LEAVES);}
    private void banner(final World w,int x,int y,int z,Material wool){w.getBlockAt(x,y,z).setType(wool);w.getBlockAt(x,y+1,z).setType(Material.WHITE_BANNER);}
    private void buildArchedEntrance(final World w,int x,int y,int z){for(int h=0;h<7;h++)for(int dx=-5;dx<=5;dx++)if(Math.abs(dx)>Math.max(0,4-h/2))w.getBlockAt(x+dx,y+h,z).setType(Material.STONE_BRICKS);}
    private void lampPost(final World w,int x,int z){for(int y=GROUND+2;y<=GROUND+5;y++)w.getBlockAt(x,y,z).setType(Material.SPRUCE_FENCE);w.getBlockAt(x,GROUND+6,z).setType(Material.LANTERN);}
    private void buildLamp(final World w,int x,int z,int height){for(int y=GROUND+2;y<=GROUND+height;y++)w.getBlockAt(x,y,z).setType(Material.SPRUCE_FENCE);w.getBlockAt(x,GROUND+height+1,z).setType(Material.LANTERN);}
    private void ring(final World w,int cx,int cz,int r,int y,Material m){for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){int d=dx*dx+dz*dz;if(d<=r*r&&d>Math.max(0,(r-1)*(r-1)))w.getBlockAt(cx+dx,y,cz+dz).setType(m);}}
    private void path(final World w,int x1,int z1,int x2,int z2,int r,Material m){int sx=Integer.signum(x2-x1),sz=Integer.signum(z2-z1),x=x1,z=z1;while(true){for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(dx*dx+dz*dz<=r*r)w.getBlockAt(x+dx,GROUND+1,z+dz).setType(m);if(x==x2&&z==z2)break;if(x!=x2)x+=sx;if(z!=z2)z+=sz;}}
    private void pave(final World w,int x1,int z1,int x2,int z2,Material m){for(int x=Math.min(x1,x2);x<=Math.max(x1,x2);x++)for(int z=Math.min(z1,z2);z<=Math.max(z1,z2);z++)w.getBlockAt(x,GROUND,z).setType(m);}
    private int highest(final World w,int x,int z){return w.getHighestBlockYAt(x,z);}
    private void buildTerraceGarden(final World w,int x,int z){for(int dx=-12;dx<=12;dx++)for(int dz=-6;dz<=6;dz++)w.getBlockAt(x+dx,GROUND+1,z+dz).setType(Material.GRASS_BLOCK);for(int i=-9;i<=9;i+=3)tree(w,x+i,z,4,Material.OAK_LOG);}
    private void buildSign(final World w,int x,int y,int z,String title,String subtitle){w.getBlockAt(x,y,z).setType(Material.OAK_SIGN);if(w.getBlockAt(x,y,z).getState() instanceof org.bukkit.block.Sign s){s.line(0,net.kyori.adventure.text.Component.text(title));s.line(1,net.kyori.adventure.text.Component.text(subtitle));s.update();}}
    private void deleteTree(final Path root){if(!Files.exists(root))return;try(var s=Files.walk(root)){s.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}catch(IOException e){throw new IllegalStateException("Could not remove legacy Floor 1 world",e);}}
}
