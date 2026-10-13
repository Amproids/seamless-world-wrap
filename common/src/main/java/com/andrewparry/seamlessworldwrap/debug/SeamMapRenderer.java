package com.andrewparry.seamlessworldwrap.debug;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * Renders a top-down map straight from the world generator, without generating chunks.
 * Heights come from sampling the router directly, which is far cheaper than
 * {@code getBaseHeight} (that builds a whole NoiseChunk per column): the cheap preliminary
 * surface finds roughly where the ground is, then the final density is scanned near there.
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

    /** Same threshold vanilla uses for the preliminary surface level (NoiseChunk). */
    private static final double SURFACE_DENSITY = 0.390625;

    public static Result render(ServerLevel level, WrapSettings settings, int blocksPerPixel, Path outDir, IntConsumer progressPercent) throws IOException {
        long start = System.currentTimeMillis();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        BiomeSource biomeSource = generator.getBiomeSource();
        int seaLevel = generator.getSeaLevel();
        DensityFunction roughDensity = randomState.router().initialDensityWithoutJaggedness();
        DensityFunction finalDensity = randomState.router().finalDensity();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // Two world sizes along each wrapped axis, centered on the real world's edge, so both seams
        // and the repeat show; one world size along an axis that doesn't wrap.
        int spanX = settings.wrapsX() ? settings.width() * 2 : settings.width();
        int spanZ = settings.wrapsZ() ? settings.height() * 2 : settings.height();
        int minX = settings.wrapsX() ? settings.minX() - settings.width() / 2 : settings.minX();
        int minZ = settings.wrapsZ() ? settings.minZ() - settings.height() / 2 : settings.minZ();
        int w = spanX / blocksPerPixel;
        int h = spanZ / blocksPerPixel;

        int[] heights = new int[w * h];
        int[] biomeColors = new int[w * h];
        AtomicInteger rowsDone = new AtomicInteger();
        IntStream.range(0, h).parallel().forEach(py -> {
            int z = minZ + py * blocksPerPixel;
            for (int px = 0; px < w; px++) {
                int x = minX + px * blocksPerPixel;
                int y = surfaceHeight(roughDensity, finalDensity, x, z, minY, maxY);
                heights[py * w + px] = y;
                Holder<Biome> biome = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(y, seaLevel)), QuartPos.fromBlock(z), randomState.sampler());
                biomeColors[py * w + px] = colorFor(biome);
            }
            int done = rowsDone.incrementAndGet();
            if (done * 10 / h != (done - 1) * 10 / h) {
                progressPercent.accept(done * 100 / h);
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
            if (settings.wrapsX()) {
                markColumns(image, minX - settings.minX(), blocksPerPixel, settings.width(), settings.blendWidth());
            }
            if (settings.wrapsZ()) {
                markRows(image, minZ - settings.minZ(), blocksPerPixel, settings.height(), settings.blendWidth());
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

    /**
     * Rough surface from the preliminary density (16-block steps), then the real top solid block:
     * final density scanned down in 4-block steps from well above the rough surface, since
     * jaggedness (peaks) and 3D noise can raise the ground above the estimate.
     */
    private static int surfaceHeight(DensityFunction rough, DensityFunction exact, int x, int z, int minY, int maxY) {
        int roughY = minY;
        for (int y = maxY - 1; y >= minY; y -= 16) {
            if (density(rough, x, y, z) > SURFACE_DENSITY) {
                roughY = y;
                break;
            }
        }
        for (int y = Math.min(roughY + 80, maxY - 1); y >= minY; y -= 4) {
            if (density(exact, x, y, z) > 0.0) {
                for (int fine = Math.min(y + 3, maxY - 1); fine > y; fine--) {
                    if (density(exact, x, fine, z) > 0.0) {
                        return fine;
                    }
                }
                return y;
            }
        }
        return minY;
    }

    private static double density(DensityFunction function, int x, int y, int z) {
        return function.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    /** Seams every world width from the range start ({@code minX} is relative to it); band starts {@code blend} blocks before each. */
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
