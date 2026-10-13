package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrapHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scheduled block/fluid ticks are stored by chunk, so their positions must be canonical. */
@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin<T> implements SeamWrapHolder {

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

    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> seamless$canonicalTick(ScheduledTick<T> tick) {
        if (seamless$wrap == null) {
            return tick;
        }
        BlockPos pos = SeamWrap.canonical(seamless$wrap, tick.pos());
        return pos == tick.pos() ? tick : new ScheduledTick<>(tick.type(), pos, tick.triggerTick(), tick.priority(), tick.subTickOrder());
    }

    @ModifyVariable(method = {"hasScheduledTick", "willTickThisTick"}, at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$canonicalQuery(BlockPos pos) {
        return seamless$wrap == null ? pos : SeamWrap.canonical(seamless$wrap, pos);
    }
}
