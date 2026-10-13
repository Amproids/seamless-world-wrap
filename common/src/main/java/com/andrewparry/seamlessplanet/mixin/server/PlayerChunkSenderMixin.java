package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamChunkSender;
import com.andrewparry.seamlessplanet.wrap.SeamPacketTranslator;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * In wrapped levels, pending chunks are alias (client-frame) positions, and each is sent as a chunk
 * packet already placed at that alias. Same batching and pacing as vanilla.
 */
@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin implements SeamChunkSender {

    @Shadow
    @Final
    private LongSet pendingChunks;

    @Shadow
    @Final
    private boolean memoryConnection;

    @Shadow
    private float desiredChunksPerTick;

    @Shadow
    private float batchQuota;

    @Shadow
    private int unacknowledgedBatches;

    @Shadow
    private int maxUnacknowledgedBatches;

    @Override
    public void seamless$markAlias(long aliasPos) {
        pendingChunks.add(aliasPos);
    }

    @Override
    public boolean seamless$isAliasPending(long aliasPos) {
        return pendingChunks.contains(aliasPos);
    }

    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    private void seamless$dropAlias(ServerPlayer player, ChunkPos alias, CallbackInfo ci) {
        if (SeamWrap.of(player.level()) == null) {
            return;
        }
        ci.cancel();
        if (!pendingChunks.remove(alias.toLong()) && player.isAlive()) {
            SeamPacketTranslator.sendExact(player.connection, new ClientboundForgetLevelChunkPacket(alias));
        }
    }

    @Inject(method = "sendNextChunks", at = @At("HEAD"), cancellable = true)
    private void seamless$sendAliases(ServerPlayer player, CallbackInfo ci) {
        WrapSettings wrap = SeamWrap.of(player.level());
        if (wrap == null) {
            return;
        }
        ci.cancel();
        if (unacknowledgedBatches >= maxUnacknowledgedBatches) {
            return;
        }
        float max = Math.max(1.0F, desiredChunksPerTick);
        batchQuota = Math.min(batchQuota + desiredChunksPerTick, max);
        if (batchQuota < 1.0F || pendingChunks.isEmpty()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        ChunkPos center = player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned positioned
                ? positioned.center() : player.chunkPosition();
        List<Long> order = new ArrayList<>(pendingChunks);
        order.sort(Comparator.comparingInt(pos -> center.distanceSquared(pos)));
        int limit = memoryConnection ? order.size() : Mth.floor(batchQuota);

        List<long[]> batch = new ArrayList<>();
        List<LevelChunk> chunks = new ArrayList<>();
        for (long alias : order) {
            if (batch.size() >= limit) {
                break;
            }
            LevelChunk chunk = chunkMap.getChunkToSend(alias);
            if (chunk != null) {
                batch.add(new long[]{alias});
                chunks.add(chunk);
            }
        }
        if (chunks.isEmpty()) {
            return;
        }

        unacknowledgedBatches++;
        player.connection.send(ClientboundChunkBatchStartPacket.INSTANCE);
        for (int i = 0; i < chunks.size(); i++) {
            long alias = batch.get(i)[0];
            LevelChunk chunk = chunks.get(i);
            pendingChunks.remove(alias);
            var packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            int dx = (ChunkPos.getX(alias) - chunk.getPos().x) * 16;
            int dz = (ChunkPos.getZ(alias) - chunk.getPos().z) * 16;
            SeamPacketTranslator.sendExact(player.connection, SeamPacketTranslator.offsetCopy(packet, dx, dz));
        }
        player.connection.send(new ClientboundChunkBatchFinishedPacket(chunks.size()));
        batchQuota -= chunks.size();
    }
}
