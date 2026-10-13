package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A player's chunk view in a wrapped level: the normal circle around them, plus the same circle
 * around each spot they'd land on if they crossed a nearby seam (one world size over). Those chunks
 * are sent ahead of time, so when the player is teleported across the seam everything around their
 * landing spot is already loaded.
 *
 * @param regions centers of the circles: the player's chunk first, then the landing spots
 */
public record SeamTrackingView(List<ChunkPos> regions, int viewDistance) implements ChunkTrackingView {

    public static SeamTrackingView of(WrapSettings wrap, ChunkPos center, int viewDistance) {
        int reach = viewDistance + 1;
        List<Integer> dxs = mirrors(wrap.wrapsX(), center.x, wrap.minChunkX(), wrap.widthChunks(), reach);
        List<Integer> dzs = mirrors(wrap.wrapsZ(), center.z, wrap.minChunkZ(), wrap.heightChunks(), reach);
        List<ChunkPos> regions = new ArrayList<>();
        for (int dx : dxs) {
            for (int dz : dzs) {
                regions.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
        return new SeamTrackingView(List.copyOf(regions), viewDistance);
    }

    /** 0, plus a world size either way if a seam on that side is within reach. */
    private static List<Integer> mirrors(boolean wraps, int center, int min, int size, int reach) {
        List<Integer> out = new ArrayList<>(List.of(0));
        if (wraps) {
            if (center - min <= reach) {
                out.add(size);   // west/north seam nearby: crossing it lands you one world size east/south
            }
            if (min + size - 1 - center <= reach) {
                out.add(-size);
            }
        }
        return out;
    }

    public ChunkPos center() {
        return regions.get(0);
    }

    @Override
    public boolean contains(int x, int z, boolean includeOuterChunksAdjacentToViewBorder) {
        for (ChunkPos region : regions) {
            if (ChunkTrackingView.isWithinDistance(region.x, region.z, viewDistance, x, z, includeOuterChunksAdjacentToViewBorder)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEach(Consumer<ChunkPos> action) {
        LongOpenHashSet seen = new LongOpenHashSet();
        for (ChunkPos region : regions) {
            for (int x = region.x - viewDistance - 1; x <= region.x + viewDistance + 1; x++) {
                for (int z = region.z - viewDistance - 1; z <= region.z + viewDistance + 1; z++) {
                    if (ChunkTrackingView.isWithinDistance(region.x, region.z, viewDistance, x, z, true) && seen.add(ChunkPos.asLong(x, z))) {
                        action.accept(new ChunkPos(x, z));
                    }
                }
            }
        }
    }

    /** Chunks to drop are those only in the old view; chunks to send are those only in the new one. */
    public static void difference(ChunkTrackingView oldView, ChunkTrackingView newView, Consumer<ChunkPos> send, Consumer<ChunkPos> drop) {
        if (oldView.equals(newView)) {
            return;
        }
        oldView.forEach(pos -> {
            if (!newView.contains(pos)) {
                drop.accept(pos);
            }
        });
        newView.forEach(pos -> {
            if (!oldView.contains(pos)) {
                send.accept(pos);
            }
        });
    }

    /** The view's (first) center, or null for an empty view. */
    public static ChunkPos centerOf(ChunkTrackingView view) {
        if (view instanceof SeamTrackingView seam) {
            return seam.center();
        }
        return view instanceof ChunkTrackingView.Positioned positioned ? positioned.center() : null;
    }

    /** Every circle center of the view, with its radius (empty view: none). */
    public static List<ChunkPos> regionsOf(ChunkTrackingView view) {
        if (view instanceof SeamTrackingView seam) {
            return seam.regions();
        }
        return view instanceof ChunkTrackingView.Positioned positioned ? List.of(positioned.center()) : List.of();
    }

    public static int radiusOf(ChunkTrackingView view) {
        if (view instanceof SeamTrackingView seam) {
            return seam.viewDistance();
        }
        return view instanceof ChunkTrackingView.Positioned positioned ? positioned.viewDistance() : 0;
    }
}
