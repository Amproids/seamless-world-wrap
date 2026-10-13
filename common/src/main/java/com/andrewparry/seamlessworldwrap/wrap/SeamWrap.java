package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.SeamlessWorldWrap;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Coordinate helpers for wrapped (server-side) levels.
 *
 * <p>The server keeps one canonical copy of the world, x in [0, width) (and z in [0, height) on
 * a torus). Anything outside that range is an alias of a canonical position. Lookups canonicalize;
 * positions sent to a client are moved to the alias nearest that player (see SeamPacketTranslator),
 * so each client sees an ordinary endless world.
 */
public final class SeamWrap {

    private SeamWrap() {
    }

    /** The wrap for a level, or null if it doesn't wrap. Only the server overworld wraps for now. */
    @Nullable
    public static WrapSettings of(@Nullable Level level) {
        return level instanceof ServerLevel && level.dimension() == Level.OVERWORLD ? SeamlessWorldWrap.settings() : null;
    }

    public static BlockPos canonical(WrapSettings wrap, BlockPos pos) {
        int x = wrap.wrapX(pos.getX());
        int z = wrap.wrapZ(pos.getZ());
        return x == pos.getX() && z == pos.getZ() ? pos : new BlockPos(x, pos.getY(), z);
    }

    public static long canonicalChunk(WrapSettings wrap, long chunkPos) {
        int x = ChunkPos.getX(chunkPos);
        int z = ChunkPos.getZ(chunkPos);
        int wx = wrap.wrapChunkX(x);
        int wz = wrap.wrapChunkZ(z);
        return wx == x && wz == z ? chunkPos : ChunkPos.asLong(wx, wz);
    }

    public static ChunkPos canonicalChunk(WrapSettings wrap, ChunkPos pos) {
        int wx = wrap.wrapChunkX(pos.x);
        int wz = wrap.wrapChunkZ(pos.z);
        return wx == pos.x && wz == pos.z ? pos : new ChunkPos(wx, wz);
    }

    /** Block shift (a multiple of the width) that moves x to its alias nearest {@code nearX}. */
    public static int shiftX(WrapSettings wrap, double x, double nearX) {
        return (int) Math.round((nearX - x) / wrap.width()) * wrap.width();
    }

    /** Same as {@link #shiftX} for Z; always 0 unless the world is a torus. */
    public static int shiftZ(WrapSettings wrap, double z, double nearZ) {
        return wrap.shape().wrapsZ() ? (int) Math.round((nearZ - z) / wrap.height()) * wrap.height() : 0;
    }

    /** Shortest X distance from {@code from} to {@code to}, going across the seam if that's shorter. */
    public static double deltaX(WrapSettings wrap, double from, double to) {
        return to + shiftX(wrap, to, from) - from;
    }

    public static double deltaZ(WrapSettings wrap, double from, double to) {
        return to + shiftZ(wrap, to, from) - from;
    }
}
