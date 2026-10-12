package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.mixin.packet.ServerboundMovePlayerPacketAccessor;
import com.andrewparry.seamlessplanet.mixin.packet.ServerboundPlayerActionPacketAccessor;
import com.andrewparry.seamlessplanet.mixin.packet.ServerboundSignUpdatePacketAccessor;
import com.andrewparry.seamlessplanet.mixin.packet.ServerboundUseItemOnPacketAccessor;
import com.andrewparry.seamlessplanet.wrap.SeamPlayer;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Converts the client's (continuous, unwrapped) coordinates to canonical ones on the way in, and
 * moves the player back into the canonical range after they cross a seam, without telling the
 * client: its coordinates stay continuous and only the player's offset changes.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private double firstGoodX;

    @Shadow
    private double firstGoodZ;

    @Shadow
    private double lastGoodX;

    @Shadow
    private double lastGoodZ;

    @Shadow
    private Vec3 awaitingPositionFromClient;

    private static final String ENSURE_THREAD =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    // Each handler first re-queues itself onto the server thread; injecting after that call means
    // the packet is translated exactly once.

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = ENSURE_THREAD, shift = At.Shift.AFTER))
    private void seamless$moveFromClientFrame(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (packet.hasPosition() && SeamWrap.of(player.level()) != null) {
            SeamPlayer seam = (SeamPlayer) player;
            ServerboundMovePlayerPacketAccessor access = (ServerboundMovePlayerPacketAccessor) packet;
            access.seamless$setX(packet.getX(0) - seam.seamless$offsetX());
            access.seamless$setZ(packet.getZ(0) - seam.seamless$offsetZ());
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void seamless$canonicalizeAfterMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        seamless$canonicalizePlayer();
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = ENSURE_THREAD, shift = At.Shift.AFTER))
    private void seamless$useItemOnFromClientFrame(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        SeamPlayer seam = (SeamPlayer) player;
        if (SeamWrap.of(player.level()) != null && (seam.seamless$offsetX() != 0 || seam.seamless$offsetZ() != 0)) {
            BlockHitResult hit = packet.getHitResult();
            int dx = -seam.seamless$offsetX();
            int dz = -seam.seamless$offsetZ();
            BlockHitResult moved = new BlockHitResult(hit.getLocation().add(dx, 0, dz), hit.getDirection(),
                    hit.getBlockPos().offset(dx, 0, dz), hit.isInside());
            ((ServerboundUseItemOnPacketAccessor) packet).seamless$setBlockHit(moved);
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = ENSURE_THREAD, shift = At.Shift.AFTER))
    private void seamless$actionFromClientFrame(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        SeamPlayer seam = (SeamPlayer) player;
        if (SeamWrap.of(player.level()) != null && (seam.seamless$offsetX() != 0 || seam.seamless$offsetZ() != 0)) {
            ((ServerboundPlayerActionPacketAccessor) packet).seamless$setPos(
                    packet.getPos().offset(-seam.seamless$offsetX(), 0, -seam.seamless$offsetZ()));
        }
    }

    @Inject(method = "handleSignUpdate", at = @At("HEAD"))
    private void seamless$signFromClientFrame(ServerboundSignUpdatePacket packet, CallbackInfo ci) {
        // Not re-queued onto the server thread (text is filtered first), so HEAD runs exactly once.
        SeamPlayer seam = (SeamPlayer) player;
        if (SeamWrap.of(player.level()) != null && (seam.seamless$offsetX() != 0 || seam.seamless$offsetZ() != 0)) {
            ((ServerboundSignUpdatePacketAccessor) packet).seamless$setPos(
                    packet.getPos().offset(-seam.seamless$offsetX(), 0, -seam.seamless$offsetZ()));
        }
    }

    /**
     * Absolute teleports land on the canonical position, with the offset chosen so the client ends
     * up at the alias nearest where it was. Relative teleports keep the offset; the player is
     * canonicalized afterwards like after any other move.
     */
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private void seamless$teleportCanonical(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relative, CallbackInfo ci) {
        WrapSettings wrap = SeamWrap.of(player.level());
        if (wrap == null) {
            return;
        }
        SeamPlayer seam = (SeamPlayer) player;
        double cx = relative.contains(RelativeMovement.X) ? x : wrap.wrapX(x);
        double cz = relative.contains(RelativeMovement.Z) ? z : wrap.wrapZ(z);
        int offsetX = seam.seamless$offsetX();
        int offsetZ = seam.seamless$offsetZ();
        if (!relative.contains(RelativeMovement.X)) {
            double clientX = player.getX() + offsetX;
            offsetX = (int) Math.round((clientX - cx) / wrap.width()) * wrap.width();
        }
        if (!relative.contains(RelativeMovement.Z) && wrap.shape().wrapsZ()) {
            double clientZ = player.getZ() + offsetZ;
            offsetZ = (int) Math.round((clientZ - cz) / wrap.height()) * wrap.height();
        }
        seam.seamless$setOffset(offsetX, offsetZ);
        if (cx != x || cz != z) {
            ci.cancel();
            ((ServerGamePacketListenerImpl) (Object) this).teleport(cx, y, cz, yaw, pitch, relative);
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void seamless$canonicalizeAfterTeleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relative, CallbackInfo ci) {
        seamless$canonicalizePlayer();
    }

    /** If the player is past a seam, shift them (and everything that remembers their position) back. */
    private void seamless$canonicalizePlayer() {
        WrapSettings wrap = SeamWrap.of(player.level());
        if (wrap == null) {
            return;
        }
        int shiftX = (int) Math.floor(player.getX() / wrap.width()) * wrap.width();
        int shiftZ = wrap.shape().wrapsZ() ? (int) Math.floor(player.getZ() / wrap.height()) * wrap.height() : 0;
        if (shiftX == 0 && shiftZ == 0) {
            return;
        }
        player.setPos(player.getX() - shiftX, player.getY(), player.getZ() - shiftZ);
        player.xo -= shiftX;
        player.xOld -= shiftX;
        player.zo -= shiftZ;
        player.zOld -= shiftZ;
        firstGoodX -= shiftX;
        lastGoodX -= shiftX;
        firstGoodZ -= shiftZ;
        lastGoodZ -= shiftZ;
        if (awaitingPositionFromClient != null) {
            awaitingPositionFromClient = awaitingPositionFromClient.add(-shiftX, 0, -shiftZ);
        }
        SeamPlayer seam = (SeamPlayer) player;
        seam.seamless$setOffset(seam.seamless$offsetX() + shiftX, seam.seamless$offsetZ() + shiftZ);
        player.serverLevel().getChunkSource().move(player);
    }
}
