package com.andrewparry.seamlessworldwrap.mixin.server;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunk generation works on a grid of neighbor holders around the chunk being generated. Near a
 * seam the same canonical holder sits at several grid cells (its aliases), and generation code asks
 * for chunks by positions that may be off the grid. Vanilla would throw for any out-of-range
 * request, so only those are answered: the grid itself shows the world's period (how far apart
 * the cells holding the same chunk are), and the request is moved by whole periods onto the grid.
 */
@Mixin(StaticCache2D.class)
public abstract class StaticCache2DMixin<T> {

    @Shadow
    @Final
    private int minX;

    @Shadow
    @Final
    private int minZ;

    @Shadow
    @Final
    private int sizeX;

    @Shadow
    @Final
    private int sizeZ;

    @Shadow
    @Final
    private Object[] cache;

    @Unique
    private int seamless$periodX = -1;

    @Unique
    private int seamless$periodZ;

    @Shadow
    public abstract boolean contains(int x, int z);

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void seamless$findAlias(int x, int z, CallbackInfoReturnable<T> cir) {
        if (contains(x, z)) {
            return;
        }
        if (seamless$periodX < 0) {
            seamless$measurePeriods();
        }
        int gx = seamless$onto(x, minX, sizeX, seamless$periodX);
        int gz = seamless$onto(z, minZ, sizeZ, seamless$periodZ);
        if (contains(gx, gz)) {
            cir.setReturnValue((T) cache[(gx - minX) * sizeZ + (gz - minZ)]);
        }
    }

    /** The period along an axis is the gcd of every (cell position - holder position); 0 if it doesn't wrap here. */
    @Unique
    private void seamless$measurePeriods() {
        int px = 0;
        int pz = 0;
        for (int i = 0; i < cache.length; i++) {
            if (cache[i] instanceof GenerationChunkHolder holder) {
                int cellX = minX + i / sizeZ;
                int cellZ = minZ + i % sizeZ;
                px = seamless$gcd(px, Math.abs(cellX - holder.getPos().x));
                pz = seamless$gcd(pz, Math.abs(cellZ - holder.getPos().z));
            }
        }
        seamless$periodX = px;
        seamless$periodZ = pz;
    }

    @Unique
    private static int seamless$onto(int value, int min, int size, int period) {
        if (value >= min && value < min + size || period == 0) {
            return value;
        }
        return min + Math.floorMod(value - min, period);
    }

    @Unique
    private static int seamless$gcd(int a, int b) {
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}
