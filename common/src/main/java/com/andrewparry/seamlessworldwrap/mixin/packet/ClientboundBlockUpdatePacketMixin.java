package com.andrewparry.seamlessworldwrap.mixin.packet;

import com.andrewparry.seamlessworldwrap.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundBlockUpdatePacket.class)
public abstract class ClientboundBlockUpdatePacketMixin implements SeamShiftable, Cloneable {

    @Shadow
    @Final
    @Mutable
    private BlockPos pos;

    @Override
    public double seamless$refX() {
        return pos.getX();
    }

    @Override
    public double seamless$refZ() {
        return pos.getZ();
    }

    @Override
    public void seamless$offset(int dx, int dz) {
        pos = pos.offset(dx, 0, dz);
    }

    @Override
    public boolean seamless$allAliases() {
        return true;
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
