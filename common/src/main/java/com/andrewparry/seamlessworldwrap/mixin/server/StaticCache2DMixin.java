package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.SeamlessWorldWrap;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.util.StaticCache2D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Chunk generation works on a grid of neighbor holders around the chunk being generated. Near a
 * seam, a neighbor's canonical position isn't on that grid, but one of its aliases is. Vanilla
 * would throw for any out-of-range request, so only those requests are redirected to the alias.
 */
@Mixin(StaticCache2D.class)
public abstract class StaticCache2DMixin {

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

    @ModifyVariable(method = "get", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int seamless$aliasX(int x) {
        WrapSettings wrap = SeamlessWorldWrap.settings();
        return seamless$intoRange(x, minX, sizeX, wrap.widthChunks());
    }

    @ModifyVariable(method = "get", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int seamless$aliasZ(int z) {
        WrapSettings wrap = SeamlessWorldWrap.settings();
        return wrap.shape().wrapsZ() ? seamless$intoRange(z, minZ, sizeZ, wrap.heightChunks()) : z;
    }

    private static int seamless$intoRange(int value, int min, int size, int period) {
        if (value >= min && value < min + size) {
            return value;
        }
        int alias = min + Math.floorMod(value - min, period);
        return alias < min + size ? alias : value;
    }
}
