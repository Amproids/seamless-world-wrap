package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrapHolder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements SeamWrapHolder {

    @Unique
    private WrapSettings seamless$wrap;

    @Override
    public WrapSettings seamless$wrap() {
        return seamless$wrap;
    }

    /** Also hands the wrap to every ticket tracker (their classes are package-private, so found reflectively). */
    @Override
    public void seamless$setWrap(WrapSettings wrap) {
        this.seamless$wrap = wrap;
        for (Field field : DistanceManager.class.getDeclaredFields()) {
            if (SeamWrapHolder.class.isAssignableFrom(field.getType()) || net.minecraft.server.level.ChunkTracker.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(this) instanceof SeamWrapHolder holder) {
                        holder.seamless$setWrap(wrap);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Can't set wrap on " + field, e);
                }
            }
        }
    }

    /**
     * The player's section is briefly past the seam while a move is processed (before the player
     * is moved back), so player tickets are always filed under the canonical section.
     */
    @ModifyVariable(method = {"addPlayer", "removePlayer"}, at = @At("HEAD"), argsOnly = true)
    private SectionPos seamless$canonicalPlayerSection(SectionPos section) {
        if (seamless$wrap == null) {
            return section;
        }
        int x = seamless$wrap.wrapChunkX(section.x());
        int z = seamless$wrap.wrapChunkZ(section.z());
        return x == section.x() && z == section.z() ? section : SectionPos.of(x, section.y(), z);
    }

    @ModifyVariable(method = {"addTicket(JLnet/minecraft/server/level/Ticket;)V", "removeTicket(JLnet/minecraft/server/level/Ticket;)V"},
            at = @At("HEAD"), argsOnly = true)
    private long seamless$canonicalTicket(long chunkPos) {
        return seamless$wrap == null ? chunkPos : SeamWrap.canonicalChunk(seamless$wrap, chunkPos);
    }
}
