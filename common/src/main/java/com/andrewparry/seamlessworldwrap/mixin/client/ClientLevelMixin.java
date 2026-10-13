package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.client.ClientEntityShift;
import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every entity lives at one spot on the client, and the other copies of it are only drawn. Keep that
 * one spot (its hitbox) on the copy nearest the player: whenever an entity is more than half a
 * world size away, move it by whole world sizes, together with everything that remembers its
 * position (previous position, interpolation target, last position the server sent), so nothing
 * visibly moves.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "tickEntities", at = @At("HEAD"))
    private void seamless$keepEntitiesNear(CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        WrapSettings wrap = ClientWrapState.forDimension(level.dimension());
        Player player = Minecraft.getInstance().player;
        if (wrap == null || player == null) {
            return;
        }
        Entity ownVehicle = player.getRootVehicle();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || entity == ownVehicle || entity.isPassenger()) {
                continue;
            }
            double sx = wrap.wrapsX() ? Math.round((player.getX() - entity.getX()) / wrap.width()) * (double) wrap.width() : 0.0;
            double sz = wrap.wrapsZ() ? Math.round((player.getZ() - entity.getZ()) / wrap.height()) * (double) wrap.height() : 0.0;
            if (sx != 0.0 || sz != 0.0) {
                ClientEntityShift.shift(entity, sx, sz);
            }
        }
    }
}
