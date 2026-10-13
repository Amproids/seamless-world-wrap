package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.client.ClientEntityShift;
import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * After a seam teleport the camera jumps by exactly one world size, and vanilla would throw away
 * every built chunk mesh and rebuild them all. But the blocks one world size over are the same
 * blocks, so each section's existing mesh is handed to the section that now sits one world size
 * away, and nothing needs rebuilding. (GPU buffers move between sections; none are copied or lost.)
 */
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {

    @Shadow
    public SectionRenderDispatcher.RenderSection[] sections;

    @Shadow
    @org.spongepowered.asm.mixin.Final
    protected Level level;

    @Unique
    private double seamless$lastCamX = Double.NaN;

    @Unique
    private double seamless$lastCamZ = Double.NaN;

    @Unique
    private record Saved(SectionRenderDispatcher.CompiledSection compiled, Map<RenderType, VertexBuffer> buffers, boolean dirty) {
    }

    @Unique
    private Long2ObjectOpenHashMap<Saved> seamless$saved;

    @Unique
    private long seamless$shiftX;

    @Unique
    private long seamless$shiftZ;

    @Inject(method = "repositionCamera", at = @At("HEAD"))
    private void seamless$saveMeshes(double camX, double camZ, CallbackInfo ci) {
        seamless$saved = null;
        double lastX = seamless$lastCamX;
        double lastZ = seamless$lastCamZ;
        seamless$lastCamX = camX;
        seamless$lastCamZ = camZ;
        WrapSettings wrap = level == null ? null : ClientWrapState.forDimension(level.dimension());
        if (wrap == null || Double.isNaN(lastX)) {
            return;
        }
        // How far the old picture is from the new one: whole world sizes only, else it's not a seam jump.
        double sx = wrap.wrapsX() ? ClientEntityShift.seamShift(lastX - camX, wrap.width(), 0.5) : 0.0;
        double sz = wrap.wrapsZ() ? ClientEntityShift.seamShift(lastZ - camZ, wrap.height(), 0.5) : 0.0;
        if (sx == 0.0 && sz == 0.0) {
            return;
        }
        seamless$shiftX = (long) sx;
        seamless$shiftZ = (long) sz;
        seamless$saved = new Long2ObjectOpenHashMap<>(sections.length);
        for (SectionRenderDispatcher.RenderSection section : sections) {
            RenderSectionAccessor access = (RenderSectionAccessor) section;
            seamless$saved.put(section.getOrigin().asLong(),
                    new Saved(section.getCompiled(), new HashMap<>(access.seamless$buffers()), access.seamless$dirty()));
        }
    }

    @Inject(method = "repositionCamera", at = @At("TAIL"))
    private void seamless$restoreMeshes(double camX, double camZ, CallbackInfo ci) {
        Long2ObjectOpenHashMap<Saved> saved = seamless$saved;
        seamless$saved = null;
        if (saved == null) {
            return;
        }
        for (SectionRenderDispatcher.RenderSection section : sections) {
            BlockPos origin = section.getOrigin();
            Saved old = saved.get(BlockPos.asLong(origin.getX() + (int) seamless$shiftX, origin.getY(), origin.getZ() + (int) seamless$shiftZ));
            if (old == null) {
                continue;
            }
            RenderSectionAccessor access = (RenderSectionAccessor) section;
            access.seamless$buffers().putAll(old.buffers());
            section.compiled.set(old.compiled());
            access.seamless$setDirty(old.dirty());
        }
    }
}
