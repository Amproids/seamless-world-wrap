package com.andrewparry.seamlessplanet.mixin.packet;

import com.andrewparry.seamlessplanet.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundAddEntityPacket.class)
public abstract class ClientboundAddEntityPacketMixin implements SeamShiftable, Cloneable {

    @Shadow
    @Final
    @Mutable
    private double x;

    @Shadow
    @Final
    @Mutable
    private double z;

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
        x += dx;
        z += dz;
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
