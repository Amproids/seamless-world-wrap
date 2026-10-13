package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ore placement locks each chunk section it touches, keyed by section position. Two aliases of
 * one section (always the case in a world one chunk wide) would lock the same section twice, and
 * the second lock waits forever. Keying by canonical position makes the second lookup reuse the first.
 */
@Mixin(BulkSectionAccess.class)
public abstract class BulkSectionAccessMixin {

    @Shadow
    @Final
    private LevelAccessor level;

    @Redirect(method = "getSection", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;asLong(Lnet/minecraft/core/BlockPos;)J"))
    private long seamless$canonicalSection(BlockPos pos) {
        WrapSettings wrap = SeamWrap.of(level);
        return SectionPos.asLong(wrap == null ? pos : SeamWrap.canonical(wrap, pos));
    }
}
