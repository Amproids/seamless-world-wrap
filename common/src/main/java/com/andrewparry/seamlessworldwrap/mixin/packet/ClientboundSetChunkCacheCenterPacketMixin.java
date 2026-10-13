package com.andrewparry.seamlessworldwrap.mixin.packet;

import com.andrewparry.seamlessworldwrap.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundSetChunkCacheCenterPacket.class)
public abstract class ClientboundSetChunkCacheCenterPacketMixin implements SeamShiftable, Cloneable {

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
        return x * 16 + 8;
    }

    @Override
    public double seamless$refZ() {
        return z * 16 + 8;
    }

    @Override
    public void seamless$offset(int dx, int dz) {
        x += dx / 16;
        z += dz / 16;
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
