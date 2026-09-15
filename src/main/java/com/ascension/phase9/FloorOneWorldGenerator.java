package com.ascension.phase9;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

/** Stable Floor 1 terrain generator: a clean meadow with a defined surface and wilderness trees. */
public final class FloorOneWorldGenerator extends ChunkGenerator {
    public static final int SURFACE_Y = 72;

    @Override
    public ChunkData generateChunkData(final World world, final Random random, final int chunkX, final int chunkZ, final BiomeGrid biome) {
        final ChunkData data = createChunkData(world);
        final int minY = data.getMinHeight();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int wx = chunkX * 16 + localX;
                final int wz = chunkZ * 16 + localZ;
                final boolean river = Math.abs(wz - 315) <= 8 && Math.abs(wx) <= 520;
                for (int y = minY; y <= SURFACE_Y; y++) {
                    final Material material;
                    if (y == minY) material = Material.BEDROCK;
                    else if (y < SURFACE_Y - 4) material = Material.STONE;
                    else if (y < SURFACE_Y - 1) material = Material.DIRT;
                    else material = river ? Material.SAND : Material.GRASS_BLOCK;
                    data.setBlock(localX, y, localZ, material);
                }
                if (river) {
                    for (int y = SURFACE_Y + 1; y <= SURFACE_Y + 2; y++) data.setBlock(localX, y, localZ, Material.WATER);
                }
                if (treeSpot(wx, wz, river)) placeTree(data, localX, localZ, SURFACE_Y + 1);
            }
        }
        return data;
    }

    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }

    private static boolean treeSpot(final int x, final int z, final boolean river) {
        if (river || z < 105 || Math.abs(x) < 115) return false;
        return (hash(x, z) & 0xFFFFL) < 1800L;
    }

    private static void placeTree(final ChunkData data, final int x, final int z, final int baseY) {
        if (x < 2 || x > 13 || z < 2 || z > 13) return;
        for (int y = 0; y < 5; y++) data.setBlock(x, baseY + y, z, Material.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) + dy < 5 && baseY + 5 + dy < data.getMaxHeight()) {
                        data.setBlock(x + dx, baseY + 5 + dy, z + dz, Material.OAK_LEAVES);
                    }
                }
            }
        }
    }

    private static long hash(final int x, final int z) {
        long h = 0x9E3779B97F4A7C15L;
        h ^= (long) x * 0xBF58476D1CE4E5B9L;
        h = Long.rotateLeft(h, 27);
        h ^= (long) z * 0x94D049BB133111EBL;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
