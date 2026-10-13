package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.level.ServerPlayer;

/** Entity tracking range is measured across the seam, so players see entities on the other side. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Redirect(method = "updatePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 seamless$wrappedDelta(Vec3 playerPos, Vec3 entityPos, ServerPlayer player) {
        WrapSettings wrap = SeamWrap.of(player.level());
        if (wrap == null) {
            return playerPos.subtract(entityPos);
        }
        return new Vec3(-SeamWrap.deltaX(wrap, playerPos.x, entityPos.x), playerPos.y - entityPos.y,
                -SeamWrap.deltaZ(wrap, playerPos.z, entityPos.z));
    }
}
