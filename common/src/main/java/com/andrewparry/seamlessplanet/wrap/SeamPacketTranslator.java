package com.andrewparry.seamlessplanet.wrap;

import com.andrewparry.seamlessplanet.WrapSettings;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/** Moves positions in outgoing packets to the alias nearest the receiving player's client position. */
public final class SeamPacketTranslator {

    private SeamPacketTranslator() {
    }

    /** A player's view of the wrapped world: where their client thinks they are. */
    public record Frame(WrapSettings wrap, double clientX, double clientZ) {
        public int shiftX(double x) {
            return SeamWrap.shiftX(wrap, x, clientX);
        }

        public int shiftZ(double z) {
            return SeamWrap.shiftZ(wrap, z, clientZ);
        }
    }

    public static Frame frameOf(WrapSettings wrap, ServerPlayer player) {
        SeamPlayer seam = (SeamPlayer) player;
        return new Frame(wrap, player.getX() + seam.seamless$offsetX(), player.getZ() + seam.seamless$offsetZ());
    }

    public static Packet<?> toClient(Packet<?> packet, ServerPlayer player) {
        WrapSettings wrap = SeamWrap.of(player.level());
        return wrap == null ? packet : translate(packet, frameOf(wrap, player));
    }

    @SuppressWarnings("unchecked")
    private static Packet<?> translate(Packet<?> packet, Frame frame) {
        if (packet instanceof SeamShiftable shiftable) {
            return shiftable.seamless$shifted(frame);
        }
        if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>();
            boolean changed = false;
            for (Packet<? super ClientGamePacketListener> sub : bundle.subPackets()) {
                Packet<?> moved = translate(sub, frame);
                changed |= moved != sub;
                out.add((Packet<? super ClientGamePacketListener>) moved);
            }
            return changed ? new ClientboundBundlePacket(out) : packet;
        }
        if (packet instanceof ClientboundForgetLevelChunkPacket forget) {
            ChunkPos moved = shiftChunk(forget.pos(), frame);
            return moved == forget.pos() ? packet : new ClientboundForgetLevelChunkPacket(moved);
        }
        if (packet instanceof ClientboundChunksBiomesPacket biomes) {
            List<ClientboundChunksBiomesPacket.ChunkBiomeData> out = new ArrayList<>();
            for (ClientboundChunksBiomesPacket.ChunkBiomeData data : biomes.chunkBiomeData()) {
                out.add(new ClientboundChunksBiomesPacket.ChunkBiomeData(shiftChunk(data.pos(), frame), data.buffer()));
            }
            return new ClientboundChunksBiomesPacket(out);
        }
        return packet;
    }

    private static ChunkPos shiftChunk(ChunkPos pos, Frame frame) {
        int dx = frame.shiftX(pos.getMiddleBlockX());
        int dz = frame.shiftZ(pos.getMiddleBlockZ());
        return dx == 0 && dz == 0 ? pos : new ChunkPos(pos.x + dx / 16, pos.z + dz / 16);
    }
}
