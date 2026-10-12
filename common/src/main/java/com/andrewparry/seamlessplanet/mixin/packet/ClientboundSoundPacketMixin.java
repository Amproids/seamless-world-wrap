package com.andrewparry.seamlessplanet.mixin.packet;

import com.andrewparry.seamlessplanet.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundSoundPacket.class)
public abstract class ClientboundSoundPacketMixin implements SeamShiftable, Cloneable {

    @Shadow
    @Final
    @Mutable
    private int x;

    @Shadow
    @Final
    @Mutable
    private int z;

    @Override
    public double seamless$refX() {
        return x / 8.0;
    }

    @Override
    public double seamless$refZ() {
        return z / 8.0;
    }

    @Override
    public void seamless$offset(int dx, int dz) {
        x += dx * 8;
        z += dz * 8;
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
