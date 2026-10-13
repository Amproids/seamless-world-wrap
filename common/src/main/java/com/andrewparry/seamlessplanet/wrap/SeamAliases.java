package com.andrewparry.seamlessplanet.wrap;

import com.andrewparry.seamlessplanet.WrapSettings;
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
        // First alias at or after the west/north edge of the view.
        int startX = cx - reach + Math.floorMod(chunkX - (cx - reach), w);
        for (int ax = startX; ax <= cx + reach; ax += w) {
            if (wrap.shape().wrapsZ()) {
                int startZ = cz - reach + Math.floorMod(chunkZ - (cz - reach), h);
                for (int az = startZ; az <= cz + reach; az += h) {
                    if (view.contains(ax, az)) {
                        action.accept(ax, az);
                    }
                }
            } else if (view.contains(ax, chunkZ)) {
                action.accept(ax, chunkZ);
            }
        }
    }
}
