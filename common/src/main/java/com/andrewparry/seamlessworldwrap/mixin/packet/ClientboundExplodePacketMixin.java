package com.andrewparry.seamlessworldwrap.mixin.packet;

import com.andrewparry.seamlessworldwrap.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundExplodePacket.class)
public abstract class ClientboundExplodePacketMixin implements SeamShiftable, Cloneable {

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
    @Mutable
    private List<BlockPos> toBlow;

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
        toBlow = toBlow.stream().map(pos -> pos.offset(dx, 0, dz)).toList();
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
