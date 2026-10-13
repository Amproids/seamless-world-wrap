package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamWrapHolder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * An entity can be past a seam for part of a tick; it still belongs to the canonical section, so it
 * keeps ticking and stays visible instead of landing in a section of an unloaded alias chunk.
 */
@Mixin(EntitySectionStorage.class)
public abstract class EntitySectionStorageMixin implements SeamWrapHolder {

    @Unique
    private WrapSettings seamless$wrap;

    @Override
    public WrapSettings seamless$wrap() {
        return seamless$wrap;
    }

    @Override
    public void seamless$setWrap(WrapSettings wrap) {
        this.seamless$wrap = wrap;
    }

    @ModifyVariable(method = {"getOrCreateSection", "getSection", "remove"}, at = @At("HEAD"), argsOnly = true)
    private long seamless$canonicalSection(long sectionPos) {
        if (seamless$wrap == null) {
            return sectionPos;
        }
        int x = SectionPos.x(sectionPos);
        int z = SectionPos.z(sectionPos);
        int wx = seamless$wrap.wrapChunkX(x);
        int wz = seamless$wrap.wrapChunkZ(z);
        return wx == x && wz == z ? sectionPos : SectionPos.asLong(wx, SectionPos.y(sectionPos), wz);
    }
}
