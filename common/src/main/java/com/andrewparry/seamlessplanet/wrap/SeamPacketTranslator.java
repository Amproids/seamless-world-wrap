package com.andrewparry.seamlessplanet.wrap;

import com.andrewparry.seamlessplanet.WrapSettings;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/** Moves positions in outgoing packets to the alias nearest the receiving player's client position. */
public final class SeamPacketTranslator {

    /** Set while sending a packet that is already in the client's frame (chunks placed at an alias). */
    private static final ThreadLocal<Boolean> EXACT = ThreadLocal.withInitial(() -> false);

    private SeamPacketTranslator() {
    }

    public static void sendExact(ServerGamePacketListenerImpl connection, Packet<?> packet) {
        EXACT.set(true);
        try {
            connection.send(packet);
        } finally {
            EXACT.set(false);
        }
    }

    public static Packet<?> offsetCopy(Packet<?> packet, int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return packet;
        }
        SeamShiftable copy = (SeamShiftable) ((SeamShiftable) packet).seamless$copy();
        copy.seamless$offset(dx, dz);
        return (Packet<?>) copy;
    }

    /** A player's view of the wrapped world: where their client thinks they are. */
    public record Frame(WrapSettings wrap, double clientX, double clientZ, ChunkTrackingView view) {
        public int shiftX(double x) {
            return SeamWrap.shiftX(wrap, x, clientX);
        }

        public int shiftZ(double z) {
            return SeamWrap.shiftZ(wrap, z, clientZ);
        }
    }

    public static Frame frameOf(WrapSettings wrap, ServerPlayer player) {
        SeamPlayer seam = (SeamPlayer) player;
        return new Frame(wrap, player.getX() + seam.seamless$offsetX(), player.getZ() + seam.seamless$offsetZ(),
                player.getChunkTrackingView());
    }

    public static Packet<?> toClient(Packet<?> packet, ServerPlayer player) {
        WrapSettings wrap = SeamWrap.of(player.level());
        return wrap == null || EXACT.get() ? packet : translate(packet, frameOf(wrap, player));
    }

    @SuppressWarnings("unchecked")
    private static Packet<?> translate(Packet<?> packet, Frame frame) {
        if (packet instanceof SeamShiftable shiftable) {
            return shiftable.seamless$allAliases() ? toAllAliases(shiftable, frame) : shiftable.seamless$shifted(frame);
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

    /** One copy per alias in view, bundled; the nearest alias alone if only one is in view. */
    @SuppressWarnings("unchecked")
    private static Packet<?> toAllAliases(SeamShiftable packet, Frame frame) {
        int blockX = (int) Math.floor(packet.seamless$refX());
        int blockZ = (int) Math.floor(packet.seamless$refZ());
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        List<Packet<? super ClientGamePacketListener>> copies = new ArrayList<>();
        SeamAliases.forEachInView(frame.wrap(), frame.view(), frame.wrap().wrapChunkX(chunkX), frame.wrap().wrapChunkZ(chunkZ),
                (ax, az) -> copies.add((Packet<? super ClientGamePacketListener>) offsetCopy((Packet<?>) packet,
                        (ax - chunkX) * 16, (az - chunkZ) * 16)));
        if (copies.isEmpty()) {
            return packet.seamless$shifted(frame);
        }
        return copies.size() == 1 ? copies.get(0) : new ClientboundBundlePacket(copies);
    }

    private static ChunkPos shiftChunk(ChunkPos pos, Frame frame) {
        int dx = frame.shiftX(pos.getMiddleBlockX());
        int dz = frame.shiftZ(pos.getMiddleBlockZ());
        return dx == 0 && dz == 0 ? pos : new ChunkPos(pos.x + dx / 16, pos.z + dz / 16);
    }
}
