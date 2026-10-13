package com.andrewparry.seamlessworldwrap.client;

import com.andrewparry.seamlessworldwrap.mixin.client.AbstractMinecartLerpAccessor;
import com.andrewparry.seamlessworldwrap.mixin.client.BoatLerpAccessor;
import com.andrewparry.seamlessworldwrap.mixin.client.LivingEntityLerpAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Moves a client-side entity by whole world sizes, together with everything that remembers its
 * position (previous position, interpolation target, last position the server sent), so nothing
 * visibly moves. Passengers come along, except the local player, whose own teleport moves it.
 */
public final class ClientEntityShift {

    private ClientEntityShift() {
    }

    public static void shift(Entity entity, double sx, double sz) {
        if (entity instanceof LocalPlayer) {
            return;
        }
        entity.setPos(entity.getX() + sx, entity.getY(), entity.getZ() + sz);
        entity.xo += sx;
        entity.xOld += sx;
        entity.zo += sz;
        entity.zOld += sz;
        VecDeltaCodec codec = entity.getPositionCodec();
        codec.setBase(codec.getBase().add(sx, 0, sz));
        if (entity instanceof LivingEntity) {
            LivingEntityLerpAccessor lerp = (LivingEntityLerpAccessor) entity;
            lerp.seamless$setLerpX(lerp.seamless$lerpX() + sx);
            lerp.seamless$setLerpZ(lerp.seamless$lerpZ() + sz);
        } else if (entity instanceof AbstractMinecart) {
            AbstractMinecartLerpAccessor lerp = (AbstractMinecartLerpAccessor) entity;
            lerp.seamless$setLerpX(lerp.seamless$lerpX() + sx);
            lerp.seamless$setLerpZ(lerp.seamless$lerpZ() + sz);
        } else if (entity instanceof Boat) {
            BoatLerpAccessor lerp = (BoatLerpAccessor) entity;
            lerp.seamless$setLerpX(lerp.seamless$lerpX() + sx);
            lerp.seamless$setLerpZ(lerp.seamless$lerpZ() + sz);
        }
        for (Entity passenger : entity.getPassengers()) {
            shift(passenger, sx, sz);
        }
    }

    /** The whole-world-size part of a move, if what's left is a normal step (at most {@code maxStep}); otherwise 0. */
    public static double seamShift(double delta, int size, double maxStep) {
        double worlds = Math.round(delta / size) * (double) size;
        return worlds != 0.0 && Math.abs(delta - worlds) <= maxStep ? worlds : 0.0;
    }
}
