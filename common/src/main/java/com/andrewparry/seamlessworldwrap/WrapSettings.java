package com.andrewparry.seamlessworldwrap;

/**
 * How one dimension wraps. Each axis wraps or not on its own. The real (canonical) world is
 * centered on 0: X spans [minX, minX + width), e.g. -32 to 32 for a 64-block world, and the
 * same for Z. Everything outside a wrapped axis maps back into that range. Sizes are whole chunks
 * so seams fall on chunk boundaries. {@code blendWidth} is how many blocks before each seam the
 * terrain fades into the terrain past it.
 */
public record WrapSettings(boolean wrapsX, boolean wrapsZ, int widthChunks, int heightChunks, int blendWidth) {

    public static final int MIN_CHUNKS = 4;

    public WrapSettings {
        if (!wrapsX && !wrapsZ) {
            throw new IllegalArgumentException("At least one axis must wrap (use no settings for a normal world)");
        }
        if (widthChunks < MIN_CHUNKS || heightChunks < MIN_CHUNKS) {
            throw new IllegalArgumentException("World must be at least " + MIN_CHUNKS + " x " + MIN_CHUNKS + " chunks");
        }
        if (blendWidth < 1 || blendWidth > widthChunks * 8 || blendWidth > heightChunks * 8) {
            throw new IllegalArgumentException("Blend width must be between 1 block and half the world size");
        }
    }

    /** Settings with the default blend: a quarter of the smaller side, at most 256 blocks. */
    public static WrapSettings of(boolean wrapsX, boolean wrapsZ, int widthChunks, int heightChunks) {
        int blend = Math.max(1, Math.min(256, Math.min(widthChunks, heightChunks) * 16 / 4));
        return new WrapSettings(wrapsX, wrapsZ, widthChunks, heightChunks, blend);
    }

    /** The Nether: 1/8 the size, so portals line up with the Overworld (never below the minimum). */
    public WrapSettings nether() {
        return of(wrapsX, wrapsZ, Math.max(MIN_CHUNKS, widthChunks / 8), Math.max(MIN_CHUNKS, heightChunks / 8));
    }

    public int width() {
        return widthChunks * 16;
    }

    public int height() {
        return heightChunks * 16;
    }

    public int minChunkX() {
        return -(widthChunks / 2);
    }

    public int minChunkZ() {
        return -(heightChunks / 2);
    }

    public int minX() {
        return minChunkX() * 16;
    }

    public int minZ() {
        return minChunkZ() * 16;
    }

    /** Maps a block X coordinate into the canonical range; unchanged if X doesn't wrap. */
    public int wrapX(int x) {
        return wrapsX ? minX() + Math.floorMod(x - minX(), width()) : x;
    }

    public int wrapZ(int z) {
        return wrapsZ ? minZ() + Math.floorMod(z - minZ(), height()) : z;
    }

    public double wrapX(double x) {
        return wrapsX ? x - Math.floor((x - minX()) / width()) * width() : x;
    }

    public double wrapZ(double z) {
        return wrapsZ ? z - Math.floor((z - minZ()) / height()) * height() : z;
    }

    public int wrapChunkX(int chunkX) {
        return wrapsX ? minChunkX() + Math.floorMod(chunkX - minChunkX(), widthChunks) : chunkX;
    }

    public int wrapChunkZ(int chunkZ) {
        return wrapsZ ? minChunkZ() + Math.floorMod(chunkZ - minChunkZ(), heightChunks) : chunkZ;
    }

    /** Block shift (a multiple of the width) that brings x back into range; 0 if already in range. */
    public int shiftIntoRangeX(double x) {
        return wrapsX ? (int) Math.floor((x - minX()) / width()) * width() : 0;
    }

    public int shiftIntoRangeZ(double z) {
        return wrapsZ ? (int) Math.floor((z - minZ()) / height()) * height() : 0;
    }
}
