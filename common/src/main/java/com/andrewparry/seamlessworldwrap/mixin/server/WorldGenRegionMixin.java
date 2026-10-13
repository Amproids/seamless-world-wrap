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

/**
 * "Is this close enough to the chunk being generated?" measured across seams, for writes and for
 * chunk lookups (structure pieces built at an alias read the blocks under them there).
 */
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

    @ModifyVariable(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            "hasChunk(II)Z"}, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int seamless$nearestChunkX(int chunkX) {
        WrapSettings wrap = SeamWrap.of(getLevel());
        return wrap == null || !wrap.wrapsX() ? chunkX
                : chunkX + SeamWrap.shiftX(wrap, chunkX * 16 + 8, getCenter().getMiddleBlockX()) / 16;
    }

    @ModifyVariable(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            "hasChunk(II)Z"}, at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int seamless$nearestChunkZ(int chunkZ) {
        WrapSettings wrap = SeamWrap.of(getLevel());
        return wrap == null || !wrap.wrapsZ() ? chunkZ
                : chunkZ + SeamWrap.shiftZ(wrap, chunkZ * 16 + 8, getCenter().getMiddleBlockZ()) / 16;
    }
}
