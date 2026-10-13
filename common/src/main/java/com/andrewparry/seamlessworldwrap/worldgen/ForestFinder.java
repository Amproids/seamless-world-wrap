package com.andrewparry.seamlessworldwrap.worldgen;

import com.andrewparry.seamlessworldwrap.SeamlessWorldWrap;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;
import java.util.Set;

/**
 * "Start in a Forest": finds a forested spot on dry land in the seed's normal (unwrapped) terrain,
 * so the world's terrain can be shifted to put that spot at the center. Matters most for tiny
 * worlds, which would otherwise often be all ocean.
 */
public final class ForestFinder {

    private static final Set<ResourceKey<Biome>> FORESTS = Set.of(
            Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);
    private static final int STEP = 96;
    private static final int MAX_RADIUS = 8000;
    /** Same threshold vanilla uses for the preliminary surface. */
    private static final double SURFACE_DENSITY = 0.390625;

    private ForestFinder() {
    }

    /** Terrain offset (blocks) that puts a forest at the world's center, or {0, 0} if none is found. */
    public static int[] find(RegistryAccess registries, long seed, WrapSettings overworld) {
        LevelStem stem = registries.registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (stem == null || !(stem.generator() instanceof NoiseBasedChunkGenerator generator)) {
            return new int[]{0, 0};
        }
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        RandomState random = RandomState.create(settings, registries.lookupOrThrow(Registries.NOISE), seed);
        BiomeSource biomes = generator.getBiomeSource();
        DensityFunction surface = random.router().initialDensityWithoutJaggedness();
        int seaLevel = settings.seaLevel();
        int minY = settings.noiseSettings().minY();
        int maxY = minY + settings.noiseSettings().height();

        int centerX = overworld.wrapsX() ? overworld.minX() + overworld.width() / 2 : 0;
        int centerZ = overworld.wrapsZ() ? overworld.minZ() + overworld.height() / 2 : 0;
        // Small worlds need the whole area dry; big ones just a good spawn spot.
        int halfX = Math.min(overworld.wrapsX() ? overworld.width() / 2 : 32, 32);
        int halfZ = Math.min(overworld.wrapsZ() ? overworld.height() / 2 : 32, 32);

        for (int r = 0; r <= MAX_RADIUS; r += STEP) {
            for (int[] spot : ring(r)) {
                int x = spot[0];
                int z = spot[1];
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(seaLevel), QuartPos.fromBlock(z), random.sampler());
                if (FORESTS.stream().noneMatch(biome::is)) {
                    continue;
                }
                if (dry(surface, x, z, halfX, halfZ, seaLevel, minY, maxY)) {
                    SeamlessWorldWrap.LOG.info("Start in a Forest: found {} at {}, {}", biome.unwrapKey().map(k -> k.location()).orElse(null), x, z);
                    return new int[]{x - centerX, z - centerZ};
                }
            }
        }
        SeamlessWorldWrap.LOG.info("Start in a Forest: no forest found within {} blocks; keeping the normal terrain", MAX_RADIUS);
        return new int[]{0, 0};
    }

    private static List<int[]> ring(int r) {
        if (r == 0) {
            return List.of(new int[]{0, 0});
        }
        java.util.ArrayList<int[]> out = new java.util.ArrayList<>();
        for (int d = -r; d < r; d += STEP) {
            out.add(new int[]{d, -r});
            out.add(new int[]{r, d});
            out.add(new int[]{-d, r});
            out.add(new int[]{-r, -d});
        }
        return out;
    }

    /** Ground above sea level at the center and around the area's edge. */
    private static boolean dry(DensityFunction surface, int x, int z, int halfX, int halfZ, int seaLevel, int minY, int maxY) {
        int[][] points = {{0, 0}, {-halfX, -halfZ}, {halfX, -halfZ}, {-halfX, halfZ}, {halfX, halfZ}, {0, halfZ}, {halfX, 0}};
        for (int[] p : points) {
            if (surfaceY(surface, x + p[0], z + p[1], minY, maxY) <= seaLevel + 1) {
                return false;
            }
        }
        return true;
    }

    private static int surfaceY(DensityFunction surface, int x, int z, int minY, int maxY) {
        for (int y = maxY - 1; y >= minY; y -= 4) {
            if (surface.compute(new DensityFunction.SinglePointContext(x, y, z)) > SURFACE_DENSITY) {
                return y;
            }
        }
        return minY;
    }
}
