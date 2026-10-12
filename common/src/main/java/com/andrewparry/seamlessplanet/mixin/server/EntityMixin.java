package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side entities that move past a seam are moved to the canonical copy of their position.
 * Players are handled separately (their client controls movement), see ServerGamePacketListenerImplMixin.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public double xo;

    @Shadow
    public double zo;

    @Shadow
    public double xOld;

    @Shadow
    public double zOld;

    @Shadow
    public abstract Level level();

    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    private void seamless$wrapPosition(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer) {
            return;
        }
        WrapSettings wrap = SeamWrap.of(level());
        if (wrap == null) {
            return;
        }
        double wx = wrap.wrapX(x);
        double wz = wrap.wrapZ(z);
        if (wx != x || wz != z) {
            double dx = wx - x;
            double dz = wz - z;
            xo += dx;
            xOld += dx;
            zo += dz;
            zOld += dz;
            ci.cancel();
            ((Entity) (Object) this).setPosRaw(wx, y, wz);
        }
    }
}
