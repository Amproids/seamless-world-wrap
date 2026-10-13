package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

/**
 * Enumerates the aliases of a chunk inside a player's view. A world narrower than the view shows
 * the same canonical chunk several times, and near a seam the view also covers the landing spot
 * across it (see SeamTrackingView).
 */
public final class SeamAliases {

    @FunctionalInterface
    public interface AliasConsumer {
        void accept(int aliasX, int aliasZ);
    }

    private SeamAliases() {
    }

    public static void forEachInView(WrapSettings wrap, ChunkTrackingView view, int chunkX, int chunkZ, AliasConsumer action) {
        int radius = SeamTrackingView.radiusOf(view);
        LongOpenHashSet seen = new LongOpenHashSet();
        for (ChunkPos region : SeamTrackingView.regionsOf(view)) {
            int reach = radius + 2;
            int w = wrap.widthChunks();
            int h = wrap.heightChunks();
            // First alias at or after the west/north edge of this circle; a non-wrapping axis has just one.
            int startX = wrap.wrapsX() ? region.x - reach + Math.floorMod(chunkX - (region.x - reach), w) : chunkX;
            int endX = wrap.wrapsX() ? region.x + reach : chunkX;
            int startZ = wrap.wrapsZ() ? region.z - reach + Math.floorMod(chunkZ - (region.z - reach), h) : chunkZ;
            int endZ = wrap.wrapsZ() ? region.z + reach : chunkZ;
            for (int ax = startX; ax <= endX; ax += w) {
                for (int az = startZ; az <= endZ; az += h) {
                    if (view.contains(ax, az) && seen.add(ChunkPos.asLong(ax, az))) {
                        action.accept(ax, az);
                    }
                }
            }
        }
    }
}
