package com.andrewparry.seamlessworldwrap.mixin.packet;

import com.andrewparry.seamlessworldwrap.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import java.util.Set;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundPlayerPositionPacket.class)
public abstract class ClientboundPlayerPositionPacketMixin implements SeamShiftable, Cloneable {

    @Shadow
    @Final
    @Mutable
    private double x;

    @Shadow
    @Final
    @Mutable
    private double z;

    @Shadow
    @Final
    private Set<RelativeMovement> relativeArguments;

    @Override
    public double seamless$refX() {
        return x;
    }

    @Override
    public double seamless$refZ() {
        return z;
    }

    @Override
    public void seamless$offset(int dx, int dz) {
        // Relative axes are deltas from wherever the client already is; only absolute ones move.
        if (!relativeArguments.contains(RelativeMovement.X)) {
            x += dx;
        }
        if (!relativeArguments.contains(RelativeMovement.Z)) {
            z += dz;
        }
    }

    @Override
    public Object seamless$copy() {
        try {
            return clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
