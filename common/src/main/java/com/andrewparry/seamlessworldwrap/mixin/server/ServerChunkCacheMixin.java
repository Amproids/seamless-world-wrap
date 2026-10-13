package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Chunk lookups at alias coordinates return the canonical chunk. Block access then works across
 * the seam for free, because chunks index blocks by local (in-chunk) coordinates.
 */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {

    @Shadow
    @Final
    ServerLevel level;

    @ModifyVariable(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            "getChunkNow", "getChunkFuture", "hasChunk", "getChunkForLighting"},
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int seamless$canonicalChunkX(int x) {
        WrapSettings wrap = SeamWrap.of(level);
        return wrap == null ? x : wrap.wrapChunkX(x);
    }

    @ModifyVariable(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            "getChunkNow", "getChunkFuture", "hasChunk", "getChunkForLighting"},
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int seamless$canonicalChunkZ(int z) {
        WrapSettings wrap = SeamWrap.of(level);
        return wrap == null ? z : wrap.wrapChunkZ(z);
    }

    @ModifyVariable(method = "isPositionTicking", at = @At("HEAD"), argsOnly = true)
    private long seamless$canonicalTicking(long chunkPos) {
        WrapSettings wrap = SeamWrap.of(level);
        return wrap == null ? chunkPos : SeamWrap.canonicalChunk(wrap, chunkPos);
    }
}
