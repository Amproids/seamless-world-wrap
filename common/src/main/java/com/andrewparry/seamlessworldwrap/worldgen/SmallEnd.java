package com.andrewparry.seamlessworldwrap.worldgen;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fits the dragon fight into Ends smaller than it: instead of vanilla's ten tall pillars on a ring
 * of radius 42, four short ones on a ring that fits; the dragon's flight waypoints (rings of 60, 40
 * and 20) are scaled down so they sit inside the world.
 */
public final class SmallEnd {

    /** Vanilla pillars need this many blocks from the center (ring 42 + widest pillar 5 + 1). */
    private static final int PILLARS_NEED = 48;
    private static final int WAYPOINTS_NEED = 62;

    private SmallEnd() {
    }

    /** Half the world size along its smaller wrapped axis, if this is a wrapped End; otherwise null. */
    @Nullable
    private static Integer half(@Nullable LevelAccessor level) {
        WrapSettings wrap = SeamWrap.of(level);
        Level real = level instanceof Level l ? l : level instanceof net.minecraft.server.level.WorldGenRegion r ? r.getLevel() : null;
        if (wrap == null || real == null || real.dimension() != Level.END) {
            return null;
        }
        int half = Integer.MAX_VALUE;
        if (wrap.wrapsX()) {
            half = Math.min(half, wrap.width() / 2);
        }
        if (wrap.wrapsZ()) {
            half = Math.min(half, wrap.height() / 2);
        }
        return half;
    }

    /** Small Ends get just four pillars, shorter than vanilla's (which reach y 76 to 103). */
    private static final int[] SMALL_HEIGHTS = {70, 74, 78, 82};

    /**
     * Four short pillars on a ring that fits inside the world, the tallest one caged like vanilla's
     * (null: the End is big enough for vanilla's ten).
     */
    @Nullable
    public static List<SpikeFeature.EndSpike> fitPillars(LevelAccessor level, List<SpikeFeature.EndSpike> vanilla) {
        Integer half = half(level);
        if (half == null || half >= PILLARS_NEED) {
            return null;
        }
        WrapSettings wrap = SeamWrap.of(level);
        double ring = Math.max(3, half - 2);
        int radius = Math.max(1, Math.min(3, (int) Math.round(ring / 10.0)));
        List<SpikeFeature.EndSpike> out = new ArrayList<>();
        for (int i = 0; i < SMALL_HEIGHTS.length; i++) {
            double angle = Math.PI / 4 + i * Math.PI / 2;
            int x = (int) Math.floor(ring * Math.cos(angle));
            int z = (int) Math.floor(ring * Math.sin(angle));
            boolean caged = i == SMALL_HEIGHTS.length - 1;
            out.add(new SpikeFeature.EndSpike(wrap.wrapX(x), wrap.wrapZ(z), radius, SMALL_HEIGHTS[i], caged));
        }
        return out;
    }

    /** Scale for the dragon's waypoint rings, or 1 if the End is big enough. */
    public static double waypointScale(Level level) {
        Integer half = half(level);
        if (half == null || half >= WAYPOINTS_NEED) {
            return 1.0;
        }
        return Math.max(2, half - 2) / 60.0;
    }
}
