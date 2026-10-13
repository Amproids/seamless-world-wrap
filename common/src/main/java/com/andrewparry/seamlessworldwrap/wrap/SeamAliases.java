package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.server.level.ChunkTrackingView;

/**
 * Enumerates the aliases of a chunk inside a player's (client-frame) view. A world narrower than
 * the view distance shows the same canonical chunk several times; a wide one, at most once.
 */
public final class SeamAliases {

    @FunctionalInterface
    public interface AliasConsumer {
        void accept(int aliasX, int aliasZ);
    }

    private SeamAliases() {
    }

    public static void forEachInView(WrapSettings wrap, ChunkTrackingView view, int chunkX, int chunkZ, AliasConsumer action) {
        if (!(view instanceof ChunkTrackingView.Positioned positioned)) {
            return;
        }
        int reach = positioned.viewDistance() + 2;
        int cx = positioned.center().x;
        int cz = positioned.center().z;
        int w = wrap.widthChunks();
        int h = wrap.heightChunks();
        // First alias at or after the west/north edge of the view; a non-wrapping axis has just one.
        int startX = wrap.wrapsX() ? cx - reach + Math.floorMod(chunkX - (cx - reach), w) : chunkX;
        int endX = wrap.wrapsX() ? cx + reach : chunkX;
        int startZ = wrap.wrapsZ() ? cz - reach + Math.floorMod(chunkZ - (cz - reach), h) : chunkZ;
        int endZ = wrap.wrapsZ() ? cz + reach : chunkZ;
        for (int ax = startX; ax <= endX; ax += w) {
            for (int az = startZ; az <= endZ; az += h) {
                if (view.contains(ax, az)) {
                    action.accept(ax, az);
                }
            }
        }
    }
}
