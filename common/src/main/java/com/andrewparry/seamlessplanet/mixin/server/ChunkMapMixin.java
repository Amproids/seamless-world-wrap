package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamPlayer;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import com.andrewparry.seamlessplanet.wrap.SeamWrapHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunk holders exist only at canonical positions; lookups of aliases resolve to them. Each
 * player's chunk tracking view is centered in their client's frame, so it moves continuously
 * across the seam and nothing is unloaded or resent when they cross.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    public abstract DistanceManager getDistanceManager();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void seamless$initWrap(CallbackInfo ci) {
        ((SeamWrapHolder) getDistanceManager()).seamless$setWrap(SeamWrap.of(level));
    }

    @ModifyVariable(method = {"getUpdatingChunkIfPresent", "getVisibleChunkIfPresent", "acquireGeneration"}, at = @At("HEAD"), argsOnly = true)
    private long seamless$canonicalHolder(long chunkPos) {
        WrapSettings wrap = SeamWrap.of(level);
        return wrap == null ? chunkPos : SeamWrap.canonicalChunk(wrap, chunkPos);
    }

    /** Tracked if the alias of (x, z) nearest the player's view center is in view and already sent. */
    @Inject(method = "isChunkTracked", at = @At("HEAD"), cancellable = true)
    private void seamless$isChunkTracked(ServerPlayer player, int x, int z, CallbackInfoReturnable<Boolean> cir) {
        WrapSettings wrap = SeamWrap.of(level);
        if (wrap == null) {
            return;
        }
        ChunkPos center = seamless$clientChunk(player);
        int aliasX = x + SeamWrap.shiftX(wrap, x * 16 + 8, center.getMiddleBlockX()) / 16;
        int aliasZ = z + SeamWrap.shiftZ(wrap, z * 16 + 8, center.getMiddleBlockZ()) / 16;
        long canonical = ChunkPos.asLong(wrap.wrapChunkX(x), wrap.wrapChunkZ(z));
        cir.setReturnValue(player.getChunkTrackingView().contains(aliasX, aliasZ)
                && !player.connection.chunkSender.isPending(canonical));
    }

    @Redirect(method = "updateChunkTracking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos seamless$trackFromClientFrame(ServerPlayer player) {
        return SeamWrap.of(level) == null ? player.chunkPosition() : seamless$clientChunk(player);
    }

    private static ChunkPos seamless$clientChunk(ServerPlayer player) {
        SeamPlayer seam = (SeamPlayer) player;
        ChunkPos pos = player.chunkPosition();
        return new ChunkPos(pos.x + seam.seamless$offsetX() / 16, pos.z + seam.seamless$offsetZ() / 16);
    }
}
