package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.worldgen.SmallEnd;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.pathfinder.Node;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The dragon flies between 24 fixed waypoints on rings of 60, 40 and 20 blocks. In a smaller End,
 * scale the rings to fit and keep each waypoint inside the world, so the dragon can reach them.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {

    @WrapOperation(method = "findClosestNode()I", at = @At(value = "NEW", target = "(III)Lnet/minecraft/world/level/pathfinder/Node;"))
    private Node seamless$fitWaypoint(int x, int y, int z, Operation<Node> original) {
        EnderDragon self = (EnderDragon) (Object) this;
        WrapSettings wrap = SeamWrap.of(self.level());
        if (wrap == null) {
            return original.call(x, y, z);
        }
        double scale = SmallEnd.waypointScale(self.level());
        int sx = wrap.wrapX((int) Math.floor(x * scale));
        int sz = wrap.wrapZ((int) Math.floor(z * scale));
        return original.call(sx, y, sz);
    }
}
