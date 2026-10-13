package com.andrewparry.seamlessplanet.mixin.packet;

import com.andrewparry.seamlessplanet.wrap.SeamShiftable;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public abstract class ClientboundSectionBlocksUpdatePacketMixin implements SeamShiftable, Cloneable {

    @Shadow
    @Final
    @Mutable
    private SectionPos sectionPos;

    @Override
    public double seamless$refX() {
        return sectionPos.minBlockX() + 8;
    }

    @Override
    public double seamless$refZ() {
        return sectionPos.minBlockZ() + 8;
    }

    @Override
    public void seamless$offset(int dx, int dz) {
        sectionPos = SectionPos.of(sectionPos.x() + dx / 16, sectionPos.y(), sectionPos.z() + dz / 16);
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
