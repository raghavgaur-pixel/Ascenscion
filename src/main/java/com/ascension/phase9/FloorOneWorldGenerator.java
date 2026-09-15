package com.ascension.phase9;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

/**
 * Deterministic procedural generator for Floor 1. The floor is intentionally large and
 * chunk-generated so the server does not need to pre-fill millions of blocks at startup.
 */
public final class FloorOneWorldGenerator extends ChunkGenerator {
    public static final int SEA_LEVEL = 62;
    private static final int HAVEN_RADIUS = 150;

    @Override
    public ChunkData generateChunkData(
        final World world,
        final Random random,
        final int chunkX,
        final int chunkZ,
        final BiomeGrid biome
    ) {
        final ChunkData data = createChunkData(world);
        final int minY = data.getMinHeight();
        final int maxY = Math.min(data.getMaxHeight() - 1, 180);

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int worldX = chunkX * 16 + localX;
                final int worldZ = chunkZ * 16 + localZ;
                final int height = terrainHeight(worldX, worldZ);
                final boolean river = riverDistance(worldX, worldZ) < 13.5D;
                final boolean lake = lakeDistance(worldX, worldZ) < 82.0D;
                final int surface = Math.max(SEA_LEVEL - 1, Math.min(height, maxY - 2));

                for (int y = minY; y <= surface; y++) {
                    final Material material;
                    if (y <= 0) material = Material.BEDROCK;
                    else if (y < surface - 4) material = Material.STONE;
                    else if (y < surface - 1) material = Material.DIRT;
                    else material = chooseSurface(worldX, worldZ, surface, river, lake);
                    data.setBlock(localX, y, localZ, material);
                }

                if (river || lake) {
                    for (int y = surface + 1; y <= SEA_LEVEL; y++) data.setBlock(localX, y, localZ, Material.WATER);
                }

                if (treeSpot(worldX, worldZ, surface, river, lake)) {
                    placeTree(data, localX, localZ, surface + 1, worldX, worldZ);
                }
            }
        }
        return data;
    }

    @Override
    public boolean shouldGenerateStructures() { return false; }

    @Override
    public boolean shouldGenerateDecorations() { return false; }

    @Override
    public boolean shouldGenerateMobs() { return false; }

    private static int terrainHeight(final int x, final int z) {
        final double dist = Math.sqrt((double) x * x + (double) z * z);
        if (dist < HAVEN_RADIUS) return 72;

        final double continental = smoothNoise(x / 520.0D, z / 520.0D);
        final double hills = smoothNoise(x / 180.0D, z / 180.0D);
        final double detail = smoothNoise(x / 62.0D, z / 62.0D);
        double height = 72.0D + continental * 26.0D + hills * 15.0D + detail * 4.0D;

        final double northMountains = clamp01(((-z) - 850.0D) / 900.0D);
        final double eastHighlands = clamp01((x - 900.0D) / 900.0D);
        height += northMountains * northMountains * 55.0D;
        height += eastHighlands * eastHighlands * 34.0D;

        final double river = riverDistance(x, z);
        height -= Math.max(0.0D, 1.0D - river / 110.0D) * 18.0D;
        final double lake = lakeDistance(x, z);
        if (lake < 110.0D) height -= (1.0D - lake / 110.0D) * 32.0D;

        return Math.max(SEA_LEVEL - 3, (int) Math.round(height));
    }

    private static Material chooseSurface(final int x, final int z, final int surface, final boolean river, final boolean lake) {
        if (river || lake) return surface <= SEA_LEVEL ? Material.SAND : Material.GRASS_BLOCK;
        if (surface >= 105) return Material.SNOW_BLOCK;
        if (surface >= 94) return Material.STONE;
        if (surface >= 84) return Material.COARSE_DIRT;
        return Material.GRASS_BLOCK;
    }

    private static boolean treeSpot(final int x, final int z, final int surface, final boolean river, final boolean lake) {
        if (river || lake || surface < 66 || surface > 92) return false;
        if (Math.sqrt((double) x * x + (double) z * z) < HAVEN_RADIUS + 18) return false;
        if (Math.abs(z - (int) (0.45D * Math.sin(x / 95.0D) * 90.0D)) < 18 && Math.abs(x) < 1100) return false;
        final long h = hash(x, z);
        return (h & 0xFFFFL) < 4200L;
    }

    private static void placeTree(final ChunkData data, final int x, final int z, final int baseY, final int worldX, final int worldZ) {
        if (x < 2 || x > 13 || z < 2 || z > 13) return;
        final int trunkHeight = 4 + (int) (Math.abs(hash(worldX + 7, worldZ - 5)) % 3);
        for (int y = 0; y < trunkHeight; y++) data.setBlock(x, baseY + y, z, Material.OAK_LOG);
        final int top = baseY + trunkHeight;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) + Math.max(0, dy) <= 4) {
                        final int yy = top + dy;
                        if (yy > baseY && yy < data.getMaxHeight()) data.setBlock(x + dx, yy, z + dz, Material.OAK_LEAVES);
                    }
                }
            }
        }
    }

    private static double riverDistance(final int x, final int z) {
        final double center = 260.0D + Math.sin(x / 310.0D) * 180.0D + Math.sin(x / 97.0D) * 34.0D;
        return Math.abs(z - center);
    }

    private static double lakeDistance(final int x, final int z) {
        final double dx = x + 620.0D;
        final double dz = z - 540.0D;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double smoothNoise(final double x, final double z) {
        final int x0 = fastFloor(x);
        final int z0 = fastFloor(z);
        final double tx = fade(x - x0);
        final double tz = fade(z - z0);
        final double a = valueNoise(x0, z0);
        final double b = valueNoise(x0 + 1, z0);
        final double c = valueNoise(x0, z0 + 1);
        final double d = valueNoise(x0 + 1, z0 + 1);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private static double valueNoise(final int x, final int z) {
        long h = hash(x, z);
        h ^= (h >>> 27);
        return ((h & 0xFFFFFFL) / 8388607.5D) - 1.0D;
    }

    private static long hash(final int x, final int z) {
        long h = 0x9E3779B97F4A7C15L;
        h ^= (long) x * 0xBF58476D1CE4E5B9L;
        h = Long.rotateLeft(h, 27);
        h ^= (long) z * 0x94D049BB133111EBL;
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static int fastFloor(final double value) { return (int) Math.floor(value); }
    private static double fade(final double t) { return t * t * (3.0D - 2.0D * t); }
    private static double lerp(final double a, final double b, final double t) { return a + (b - a) * t; }
    private static double clamp01(final double value) { return Math.max(0.0D, Math.min(1.0D, value)); }
}
