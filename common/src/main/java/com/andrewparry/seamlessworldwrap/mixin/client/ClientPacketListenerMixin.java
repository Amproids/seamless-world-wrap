package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seam teleports: if an entity's new position is its old one moved by a whole number of world
 * sizes (plus a normal step), it crossed a seam. Move its position and previous position by that
 * many world sizes first, so vanilla only animates the normal step. The world looks the same one
 * world size over, so nothing visibly moves.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    /** Anything that moves further than this in one update is a real teleport, not a seam crossing. */
    private static final double MAX_STEP = 16.0;

    @Shadow
    private ClientLevel level;

    @Inject(method = "handleTeleportEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;syncPacketPositionCodec(DDD)V"))
    private void seamless$seamTeleport(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        WrapSettings wrap = level == null ? null : ClientWrapState.forDimension(level.dimension());
        if (wrap == null) {
            return;
        }
        Entity entity = level.getEntity(packet.getId());
        if (entity == null) {
            return;
        }
        double sx = wrap.wrapsX() ? seamless$seamShift(packet.getX() - entity.getX(), wrap.width()) : 0.0;
        double sz = wrap.wrapsZ() ? seamless$seamShift(packet.getZ() - entity.getZ(), wrap.height()) : 0.0;
        if (sx == 0.0 && sz == 0.0) {
            return;
        }
        entity.setPos(entity.getX() + sx, entity.getY(), entity.getZ() + sz);
        entity.xo += sx;
        entity.xOld += sx;
        entity.zo += sz;
        entity.zOld += sz;
    }

    /** A new server: forget the last one's wrap until this one says otherwise. */
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void seamless$resetWrap(ClientboundLoginPacket packet, CallbackInfo ci) {
        ClientWrapState.clear();
    }

    /** The whole-world part of a move, if the rest of it is a normal step; otherwise 0. */
    private static double seamless$seamShift(double delta, int size) {
        double worlds = Math.round(delta / size) * (double) size;
        return worlds != 0.0 && Math.abs(delta - worlds) <= MAX_STEP ? worlds : 0.0;
    }
}
