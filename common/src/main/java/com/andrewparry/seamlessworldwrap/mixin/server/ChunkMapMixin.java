package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamPlayer;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrapHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import com.andrewparry.seamlessworldwrap.wrap.SeamTrackingView;
import java.util.function.Consumer;
import net.minecraft.world.level.chunk.LevelChunk;
import com.andrewparry.seamlessworldwrap.wrap.SeamAliases;
import com.andrewparry.seamlessworldwrap.wrap.SeamChunkSender;
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
 * across the seam and nothing is unloaded or resent when they cross. Chunks are queued per alias:
 * in a world narrower than the view, one canonical chunk is sent to several places.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Final
    private PlayerMap playerMap;

    @Shadow
    public abstract DistanceManager getDistanceManager();

    @Shadow
    public abstract LevelChunk getChunkToSend(long chunkPos);

    /** The chunk map creates this level's noise; tell the noise which wrap it's for. */
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState seamless$noiseForThisLevel(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises,
                                                   long seed, Operation<RandomState> original) {
        SeamWrap.CREATING.set(SeamWrap.of(level));
        try {
            return original.call(settings, noises, seed);
        } finally {
            SeamWrap.CREATING.remove();
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void seamless$initWrap(CallbackInfo ci) {
        ((SeamWrapHolder) getDistanceManager()).seamless$setWrap(SeamWrap.of(level));
    }

    @ModifyVariable(method = {"getUpdatingChunkIfPresent", "getVisibleChunkIfPresent", "acquireGeneration"}, at = @At("HEAD"), argsOnly = true)
    private long seamless$canonicalHolder(long chunkPos) {
        WrapSettings wrap = SeamWrap.of(level);
        return wrap == null ? chunkPos : SeamWrap.canonicalChunk(wrap, chunkPos);
    }

    /** Tracked if any alias of (x, z) is in the player's view and has already been sent. */
    @Inject(method = "isChunkTracked", at = @At("HEAD"), cancellable = true)
    private void seamless$isChunkTracked(ServerPlayer player, int x, int z, CallbackInfoReturnable<Boolean> cir) {
        WrapSettings wrap = SeamWrap.of(level);
        if (wrap == null) {
            return;
        }
        SeamChunkSender sender = (SeamChunkSender) player.connection.chunkSender;
        boolean[] tracked = {false};
        SeamAliases.forEachInView(wrap, player.getChunkTrackingView(), wrap.wrapChunkX(x), wrap.wrapChunkZ(z), (ax, az) -> {
            if (!sender.seamless$isAliasPending(ChunkPos.asLong(ax, az))) {
                tracked[0] = true;
            }
        });
        cir.setReturnValue(tracked[0]);
    }

    /** The tracking view hands us alias positions; remember the alias, not the canonical chunk. */
    @Inject(method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)V",
            at = @At("HEAD"), cancellable = true)
    private void seamless$markAliasPending(ServerPlayer player, ChunkPos alias, CallbackInfo ci) {
        if (SeamWrap.of(level) == null) {
            return;
        }
        ci.cancel();
        if (getChunkToSend(alias.toLong()) != null) {
            ((SeamChunkSender) player.connection.chunkSender).seamless$markAlias(alias.toLong());
        }
    }

    /** A chunk finished loading: queue it at every alias in every player's view. */
    @Inject(method = "onChunkReadyToSend", at = @At("HEAD"), cancellable = true)
    private void seamless$readyAtAliases(LevelChunk chunk, CallbackInfo ci) {
        WrapSettings wrap = SeamWrap.of(level);
        if (wrap == null) {
            return;
        }
        ci.cancel();
        ChunkPos pos = chunk.getPos();
        for (ServerPlayer player : playerMap.getAllPlayers()) {
            SeamChunkSender sender = (SeamChunkSender) player.connection.chunkSender;
            SeamAliases.forEachInView(wrap, player.getChunkTrackingView(), pos.x, pos.z,
                    (ax, az) -> sender.seamless$markAlias(ChunkPos.asLong(ax, az)));
        }
    }

    /** In a wrapped level the view also covers the landing spots across nearby seams (preloading). */
    @Redirect(method = "updateChunkTracking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkTrackingView;of(Lnet/minecraft/world/level/ChunkPos;I)Lnet/minecraft/server/level/ChunkTrackingView;"))
    private ChunkTrackingView seamless$viewWithLandingSpots(ChunkPos center, int viewDistance) {
        WrapSettings wrap = SeamWrap.of(level);
        return wrap == null ? ChunkTrackingView.of(center, viewDistance) : SeamTrackingView.of(wrap, center, viewDistance);
    }

    /** Vanilla only knows how to diff (and announce the center of) its own circular views. */
    @Redirect(method = "applyChunkTrackingView",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkTrackingView;difference(Lnet/minecraft/server/level/ChunkTrackingView;Lnet/minecraft/server/level/ChunkTrackingView;Ljava/util/function/Consumer;Ljava/util/function/Consumer;)V"))
    private void seamless$diffViews(ChunkTrackingView oldView, ChunkTrackingView newView, Consumer<ChunkPos> send, Consumer<ChunkPos> drop,
                                    ServerPlayer player, ChunkTrackingView appliedView) {
        if (!(oldView instanceof SeamTrackingView) && !(newView instanceof SeamTrackingView)) {
            ChunkTrackingView.difference(oldView, newView, send, drop);
            return;
        }
        ChunkPos center = SeamTrackingView.centerOf(newView);
        if (newView instanceof SeamTrackingView && center != null && !center.equals(SeamTrackingView.centerOf(oldView))) {
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(center.x, center.z));
        }
        SeamTrackingView.difference(oldView, newView, send, drop);
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
