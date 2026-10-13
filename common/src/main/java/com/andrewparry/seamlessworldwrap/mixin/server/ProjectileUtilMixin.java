package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A projectile's path this tick can stick out past a seam, where the entities it would hit are
 * stored on the other side. Test the path at each alias too (shifted by whole world sizes) and
 * take the nearest hit, so arrows hit things right across the edge.
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), cancellable = true)
    private static void seamless$hitAcrossSeams(Level level, Entity projectile, Vec3 start, Vec3 end, AABB box, Predicate<Entity> filter,
                                                float inflation, CallbackInfoReturnable<EntityHitResult> cir) {
        WrapSettings wrap = SeamWrap.of(level);
        if (wrap == null) {
            return;
        }
        int[] xs = wrap.wrapsX() ? new int[]{0, wrap.width(), -wrap.width()} : new int[]{0};
        int[] zs = wrap.wrapsZ() ? new int[]{0, wrap.height(), -wrap.height()} : new int[]{0};
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx : xs) {
            for (int dz : zs) {
                AABB shiftedBox = box.move(dx, 0, dz);
                if ((dx != 0 || dz != 0) && !seamless$touchesWorld(wrap, shiftedBox)) {
                    continue;
                }
                Vec3 s = start.add(dx, 0, dz);
                Vec3 e = end.add(dx, 0, dz);
                for (Entity target : level.getEntities(projectile, shiftedBox, filter)) {
                    Optional<Vec3> hit = target.getBoundingBox().inflate(inflation).clip(s, e);
                    if (hit.isPresent()) {
                        double d = s.distanceToSqr(hit.get());
                        if (d < bestDist) {
                            best = target;
                            bestDist = d;
                        }
                    }
                }
            }
        }
        cir.setReturnValue(best == null ? null : new EntityHitResult(best));
    }

    private static boolean seamless$touchesWorld(WrapSettings wrap, AABB box) {
        boolean x = !wrap.wrapsX() || box.maxX > wrap.minX() && box.minX < wrap.minX() + wrap.width();
        boolean z = !wrap.wrapsZ() || box.maxZ > wrap.minZ() && box.minZ < wrap.minZ() + wrap.height();
        return x && z;
    }
}
