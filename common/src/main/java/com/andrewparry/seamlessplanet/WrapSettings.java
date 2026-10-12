package com.andrewparry.seamlessplanet;

/**
 * The size and shape of a wrapped world. The real world spans block coordinates
 * [0, width) on X and [0, height) on Z; everything outside maps back into that range.
 * Sizes are whole chunks so the seam always falls on a chunk boundary.
 */
public record WrapSettings(WorldShape shape, int widthChunks, int heightChunks) {
    public static final WrapSettings DEFAULT = new WrapSettings(WorldShape.CYLINDER, 64, 64);

    public WrapSettings {
        if (widthChunks < 4 || heightChunks < 4) {
            throw new IllegalArgumentException("World must be at least 4 x 4 chunks");
        }
    }

    public int width() {
        return widthChunks * 16;
    }

    public int height() {
        return heightChunks * 16;
    }

    /** Maps a block X coordinate into [0, width). */
    public int wrapX(int x) {
        return Math.floorMod(x, width());
    }

    /** Maps a block Z coordinate into [0, height) on a torus; unchanged on a cylinder. */
    public int wrapZ(int z) {
        return shape.wrapsZ() ? Math.floorMod(z, height()) : z;
    }

    public double wrapX(double x) {
        return x - Math.floor(x / width()) * width();
    }

    public double wrapZ(double z) {
        return shape.wrapsZ() ? z - Math.floor(z / height()) * height() : z;
    }

    public int wrapChunkX(int chunkX) {
        return Math.floorMod(chunkX, widthChunks);
    }

    public int wrapChunkZ(int chunkZ) {
        return shape.wrapsZ() ? Math.floorMod(chunkZ, heightChunks) : chunkZ;
    }

    /** True if Z is past a hard border (cylinder only). */
    public boolean outsideZ(int z) {
        return !shape.wrapsZ() && (z < 0 || z >= height());
    }
}
