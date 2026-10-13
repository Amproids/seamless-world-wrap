package com.andrewparry.seamlessworldwrap.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * Vanilla only keeps chunks within the view distance of the player. A wrapping server also sends
 * the chunks around where the player will land after crossing a seam (one world size away); keep
 * those in a second store, and move them into the main one when the player arrives there.
 */
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {

    @Shadow
    volatile ClientChunkCache.Storage storage;

    @Shadow
    @Final
    ClientLevel level;

    @Unique
    private final Long2ObjectMap<LevelChunk> seamless$far = new Long2ObjectOpenHashMap<>();

    @Unique
    private ClientChunkStorageAccessor seamless$storage() {
        return (ClientChunkStorageAccessor) (Object) storage;
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"), cancellable = true)
    private void seamless$keepFarChunk(int x, int z, FriendlyByteBuf buffer, CompoundTag tag,
                                       Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                       CallbackInfoReturnable<LevelChunk> cir) {
        if (seamless$storage().seamless$inRange(x, z)) {
            return;
        }
        long key = ChunkPos.asLong(x, z);
        LevelChunk chunk;
        synchronized (seamless$far) {
            chunk = seamless$far.get(key);
            if (chunk == null) {
                chunk = new LevelChunk(level, new ChunkPos(x, z));
                seamless$far.put(key, chunk);
            }
        }
        chunk.replaceWithPacketData(buffer, tag, consumer);
        cir.setReturnValue(chunk);
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("HEAD"), cancellable = true)
    private void seamless$findFarChunk(int x, int z, ChunkStatus status, boolean requireChunk, CallbackInfoReturnable<LevelChunk> cir) {
        if (seamless$storage().seamless$inRange(x, z)) {
            return;
        }
        LevelChunk chunk;
        synchronized (seamless$far) {
            chunk = seamless$far.get(ChunkPos.asLong(x, z));
        }
        if (chunk != null) {
            cir.setReturnValue(chunk);
        }
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void seamless$dropFarChunk(ChunkPos pos, CallbackInfo ci) {
        LevelChunk chunk;
        synchronized (seamless$far) {
            chunk = seamless$far.remove(pos.toLong());
        }
        if (chunk != null) {
            level.unload(chunk);
        }
    }

    /** When the view center moves (e.g. after a seam teleport), swap chunks between the two stores. */
    @Inject(method = "updateViewCenter", at = @At("TAIL"))
    private void seamless$rebalance(int centerX, int centerZ, CallbackInfo ci) {
        ClientChunkStorageAccessor main = seamless$storage();
        AtomicReferenceArray<LevelChunk> slots = main.seamless$chunks();
        synchronized (seamless$far) {
            // Main-store chunks now out of range would be lost; keep them.
            for (int i = 0; i < slots.length(); i++) {
                LevelChunk chunk = slots.get(i);
                if (chunk != null && !main.seamless$inRange(chunk.getPos().x, chunk.getPos().z)
                        && slots.compareAndSet(i, chunk, null)) {
                    main.seamless$setChunkCount(main.seamless$chunkCount() - 1);
                    seamless$far.put(chunk.getPos().toLong(), chunk);
                }
            }
            // Far chunks now in range move into the main store.
            var it = seamless$far.long2ObjectEntrySet().iterator();
            while (it.hasNext()) {
                LevelChunk chunk = it.next().getValue();
                ChunkPos pos = chunk.getPos();
                if (main.seamless$inRange(pos.x, pos.z)) {
                    int index = main.seamless$getIndex(pos.x, pos.z);
                    if (slots.compareAndSet(index, null, chunk)) {
                        main.seamless$setChunkCount(main.seamless$chunkCount() + 1);
                        it.remove();
                        level.onChunkLoaded(pos);
                    }
                }
            }
        }
    }
}
