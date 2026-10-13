package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.mixin.packet.ServerboundMovePlayerPacketAccessor;
import com.andrewparry.seamlessworldwrap.mixin.packet.ServerboundPlayerActionPacketAccessor;
import com.andrewparry.seamlessworldwrap.mixin.packet.ServerboundSignUpdatePacketAccessor;
import com.andrewparry.seamlessworldwrap.mixin.packet.ServerboundUseItemOnPacketAccessor;
import com.andrewparry.seamlessworldwrap.wrap.SeamListener;
import com.andrewparry.seamlessworldwrap.wrap.SeamPlayer;
import com.andrewparry.seamlessworldwrap.wrap.SeamShifter;
import com.andrewparry.seamlessworldwrap.mixin.packet.ServerboundMoveVehiclePacketAccessor;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
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
 * After a player crosses a seam they're teleported by one world size back into the canonical range
 * (see SeamShifter). The client-frame offset translations below are kept for safety but the offset
 * is always 0 now: client coordinates are canonical.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements SeamListener {

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

    @Shadow
    private double vehicleFirstGoodX;

    @Shadow
    private double vehicleFirstGoodZ;

    @Shadow
    private double vehicleLastGoodX;

    @Shadow
    private double vehicleLastGoodZ;

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

    /** Absolute teleports (e.g. /tp) land on the canonical position of their target. */
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private void seamless$teleportCanonical(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relative, CallbackInfo ci) {
        WrapSettings wrap = SeamWrap.of(player.level());
        if (wrap == null) {
            return;
        }
        double cx = relative.contains(RelativeMovement.X) ? x : wrap.wrapX(x);
        double cz = relative.contains(RelativeMovement.Z) ? z : wrap.wrapZ(z);
        if (cx != x || cz != z) {
            ci.cancel();
            ((ServerGamePacketListenerImpl) (Object) this).teleport(cx, y, cz, yaw, pitch, relative);
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void seamless$canonicalizeAfterTeleport(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relative, CallbackInfo ci) {
        seamless$canonicalizePlayer();
    }

    /** If the player (or the vehicle they ride) is past a seam, shift it back without telling the client. */
    private void seamless$canonicalizePlayer() {
        WrapSettings wrap = SeamWrap.of(player.level());
        if (wrap != null) {
            SeamShifter.shiftIntoRange(player.getRootVehicle(), wrap);
        }
    }

    @Override
    public void seamless$shiftRemembered(int dx, int dz) {
        firstGoodX -= dx;
        lastGoodX -= dx;
        firstGoodZ -= dz;
        lastGoodZ -= dz;
        vehicleFirstGoodX -= dx;
        vehicleLastGoodX -= dx;
        vehicleFirstGoodZ -= dz;
        vehicleLastGoodZ -= dz;
        if (awaitingPositionFromClient != null) {
            awaitingPositionFromClient = awaitingPositionFromClient.add(-dx, 0, -dz);
        }
    }

    /** Boats and other client-driven vehicles report their position in the client's frame too. */
    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE", target = ENSURE_THREAD, shift = At.Shift.AFTER))
    private void seamless$vehicleFromClientFrame(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (SeamWrap.of(player.level()) != null) {
            SeamPlayer seam = (SeamPlayer) player;
            ServerboundMoveVehiclePacketAccessor access = (ServerboundMoveVehiclePacketAccessor) packet;
            access.seamless$setX(packet.getX() - seam.seamless$offsetX());
            access.seamless$setZ(packet.getZ() - seam.seamless$offsetZ());
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At("TAIL"))
    private void seamless$canonicalizeAfterVehicleMove(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        seamless$canonicalizePlayer();
    }
}
