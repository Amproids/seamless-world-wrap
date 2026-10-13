package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Reach is measured to the copy of the target nearest the player, so you can hit things across a seam. */
@Mixin(Player.class)
public abstract class PlayerReachMixin {

    @ModifyVariable(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z", at = @At("HEAD"), argsOnly = true)
    private AABB seamless$nearestCopy(AABB box) {
        Player self = (Player) (Object) this;
        WrapSettings wrap = SeamWrap.of(self.level());
        if (wrap == null) {
            return box;
        }
        double cx = (box.minX + box.maxX) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        int dx = SeamWrap.shiftX(wrap, cx, self.getX());
        int dz = SeamWrap.shiftZ(wrap, cz, self.getZ());
        return dx == 0 && dz == 0 ? box : box.move(dx, 0, dz);
    }
}
