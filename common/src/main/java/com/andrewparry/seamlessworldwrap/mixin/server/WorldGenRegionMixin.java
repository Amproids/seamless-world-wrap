package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** "Is this write close enough to the chunk being generated?" measured across seams. */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {

    @Shadow
    public abstract ServerLevel getLevel();

    @Shadow
    public abstract ChunkPos getCenter();

    @ModifyVariable(method = "ensureCanWrite", at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$nearestAlias(BlockPos pos) {
        WrapSettings wrap = SeamWrap.of(getLevel());
        if (wrap == null) {
            return pos;
        }
        ChunkPos center = getCenter();
        int dx = SeamWrap.shiftX(wrap, pos.getX(), center.getMiddleBlockX());
        int dz = SeamWrap.shiftZ(wrap, pos.getZ(), center.getMiddleBlockZ());
        return dx == 0 && dz == 0 ? pos : pos.offset(dx, 0, dz);
    }
}
