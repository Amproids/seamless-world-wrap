package com.andrewparry.seamlessplanet.debug;

import com.andrewparry.seamlessplanet.WrapSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

/**
 * Renders a top-down map straight from the world generator, without generating chunks.
 * The map covers two world-widths centered on the seam at x = 0 (and two heights on a torus),
 * so both seams and the repeat are visible. Seams are marked red; blend band edges yellow.
 */
public final class SeamMapRenderer {

    private static final int SEAM_COLOR = 0xFF2020;
    private static final int BAND_COLOR = 0xFFD020;

    public record Result(Path terrain, Path biomes, int widthPx, int heightPx, long millis) {
    }

    private SeamMapRenderer() {
    }

    public static Result render(ServerLevel level, WrapSettings settings, int blocksPerPixel, Path outDir) throws IOException {
        long start = System.currentTimeMillis();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        BiomeSource biomeSource = generator.getBiomeSource();
        int seaLevel = generator.getSeaLevel();

        int minX = -settings.width() / 2;
        int spanX = settings.width() * 2;
        int minZ = settings.shape().wrapsZ() ? -settings.height() / 2 : 0;
        int spanZ = settings.shape().wrapsZ() ? settings.height() * 2 : settings.height();
        int w = spanX / blocksPerPixel;
        int h = spanZ / blocksPerPixel;

        int[] heights = new int[w * h];
        int[] biomeColors = new int[w * h];
        IntStream.range(0, h).parallel().forEach(py -> {
            int z = minZ + py * blocksPerPixel;
            for (int px = 0; px < w; px++) {
                int x = minX + px * blocksPerPixel;
                int y = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                heights[py * w + px] = y;
                Holder<Biome> biome = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(y, seaLevel)), QuartPos.fromBlock(z), randomState.sampler());
                biomeColors[py * w + px] = colorFor(biome);
            }
        });

        BufferedImage terrain = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage biomes = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int y = heights[py * w + px];
                // Light from the northwest so slopes read as relief.
                int north = heights[Math.max(py - 1, 0) * w + px];
                int west = heights[py * w + Math.max(px - 1, 0)];
                double shade = 1.0 + 0.04 * ((y - north) + (y - west)) / blocksPerPixel;
                terrain.setRGB(px, py, shadeRgb(heightColor(y, seaLevel), shade));
                biomes.setRGB(px, py, biomeColors[py * w + px]);
            }
        }

        for (BufferedImage image : new BufferedImage[]{terrain, biomes}) {
            markColumns(image, minX, blocksPerPixel, settings.width(), settings.blendWidth());
            if (settings.shape().wrapsZ()) {
                markRows(image, minZ, blocksPerPixel, settings.height(), settings.blendWidth());
            }
        }

        Files.createDirectories(outDir);
        String stamp = String.valueOf(System.currentTimeMillis());
        Path terrainFile = outDir.resolve("terrain-" + stamp + ".png");
        Path biomeFile = outDir.resolve("biomes-" + stamp + ".png");
        ImageIO.write(terrain, "png", terrainFile.toFile());
        ImageIO.write(biomes, "png", biomeFile.toFile());
        return new Result(terrainFile, biomeFile, w, h, System.currentTimeMillis() - start);
    }

    /** Seams at every multiple of the world width; band starts {@code blend} blocks before each. */
    private static void markColumns(BufferedImage image, int minX, int scale, int size, int blend) {
        for (int px = 0; px < image.getWidth(); px++) {
            int x = minX + px * scale;
            int pos = Math.floorMod(x, size);
            if (pos < scale) {
                tickLine(image, px, true, SEAM_COLOR, 1);
            } else if (Math.floorMod(x + blend, size) < scale) {
                tickLine(image, px, true, BAND_COLOR, 4);
            }
        }
    }

    private static void markRows(BufferedImage image, int minZ, int scale, int size, int blend) {
        for (int py = 0; py < image.getHeight(); py++) {
            int z = minZ + py * scale;
            if (Math.floorMod(z, size) < scale) {
                tickLine(image, py, false, SEAM_COLOR, 1);
            } else if (Math.floorMod(z + blend, size) < scale) {
                tickLine(image, py, false, BAND_COLOR, 4);
            }
        }
    }

    /** Draws a line, dashed when {@code dash} > 1, so the terrain under it stays visible. */
    private static void tickLine(BufferedImage image, int at, boolean vertical, int color, int dash) {
        int length = vertical ? image.getHeight() : image.getWidth();
        for (int i = 0; i < length; i++) {
            if (dash == 1 ? i % 2 == 0 : (i / dash) % 2 == 0) {
                if (vertical) {
                    image.setRGB(at, i, color);
                } else {
                    image.setRGB(i, at, color);
                }
            }
        }
    }

    private static int heightColor(int y, int seaLevel) {
        if (y < seaLevel) {
            double depth = Math.min((seaLevel - y) / 40.0, 1.0);
            return lerpRgb(0x4A90D9, 0x0B2A5C, depth);
        }
        double t = (y - seaLevel) / 120.0;
        if (t < 0.03) {
            return 0xD8C98A;
        }
        if (t < 0.35) {
            return lerpRgb(0x5DA042, 0x3D6B2A, t / 0.35);
        }
        if (t < 0.7) {
            return lerpRgb(0x3D6B2A, 0x8A7356, (t - 0.35) / 0.35);
        }
        return lerpRgb(0x8A7356, 0xF4F4F4, Math.min((t - 0.7) / 0.3, 1.0));
    }

    /** A stable, distinct color per biome, from its id. */
    private static int colorFor(Holder<Biome> biome) {
        String id = biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown");
        float hue = (id.hashCode() & 0xFFFF) / 65535f;
        float saturation = 0.45f + ((id.hashCode() >>> 16) & 0xFF) / 255f * 0.4f;
        return java.awt.Color.HSBtoRGB(hue, saturation, 0.85f) & 0xFFFFFF;
    }

    private static int lerpRgb(int a, int b, double t) {
        int r = (int) (((a >> 16) & 0xFF) + t * (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)));
        int g = (int) (((a >> 8) & 0xFF) + t * (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)));
        int bl = (int) ((a & 0xFF) + t * ((b & 0xFF) - (a & 0xFF)));
        return (r << 16) | (g << 8) | bl;
    }

    private static int shadeRgb(int rgb, double shade) {
        int r = (int) Math.max(0, Math.min(255, ((rgb >> 16) & 0xFF) * shade));
        int g = (int) Math.max(0, Math.min(255, ((rgb >> 8) & 0xFF) * shade));
        int b = (int) Math.max(0, Math.min(255, (rgb & 0xFF) * shade));
        return (r << 16) | (g << 8) | b;
    }
}
