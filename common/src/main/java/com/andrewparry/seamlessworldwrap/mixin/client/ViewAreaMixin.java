package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.client.ClientEntityShift;
import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a seam teleport the camera jumps by exactly one world size, and vanilla would give every
 * render section a new position and rebuild its mesh. The blocks one world size over are the same
 * blocks, so instead every section object moves by that same world size, keeping its mesh, and
 * goes into the slot for its new position. Everything that refers to sections (including the
 * visible-section list, which is recomputed in the background a frame or two later) stays valid,
 * because the whole picture moved together with the camera.
 */
@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {

    @Shadow
    public SectionRenderDispatcher.RenderSection[] sections;

    @Shadow
    @Final
    protected Level level;

    @Shadow
    protected int sectionGridSizeX;

    @Shadow
    protected int sectionGridSizeY;

    @Shadow
    protected int sectionGridSizeZ;

    @Unique
    private double seamless$lastCamX = Double.NaN;

    @Unique
    private double seamless$lastCamZ = Double.NaN;

    @Inject(method = "repositionCamera", at = @At("HEAD"))
    private void seamless$moveSectionsWithSeamJump(double camX, double camZ, CallbackInfo ci) {
        double lastX = seamless$lastCamX;
        double lastZ = seamless$lastCamZ;
        seamless$lastCamX = camX;
        seamless$lastCamZ = camZ;
        WrapSettings wrap = level == null ? null : ClientWrapState.forDimension(level.dimension());
        if (wrap == null || Double.isNaN(lastX)) {
            return;
        }
        // The camera moved by whole world sizes (plus a normal step): that's a seam jump.
        int dx = (int) (wrap.wrapsX() ? ClientEntityShift.seamShift(camX - lastX, wrap.width(), 16.0) : 0.0);
        int dz = (int) (wrap.wrapsZ() ? ClientEntityShift.seamShift(camZ - lastZ, wrap.height(), 16.0) : 0.0);
        if (dx == 0 && dz == 0) {
            return;
        }
        SectionRenderDispatcher.RenderSection[] moved = new SectionRenderDispatcher.RenderSection[sections.length];
        int minY = level.getMinBuildHeight();
        for (SectionRenderDispatcher.RenderSection section : sections) {
            BlockPos origin = section.getOrigin();
            int x = origin.getX() + dx;
            int y = origin.getY();
            int z = origin.getZ() + dz;
            int slot = (Math.floorMod(z >> 4, sectionGridSizeZ) * sectionGridSizeY + ((y - minY) >> 4)) * sectionGridSizeX
                    + Math.floorMod(x >> 4, sectionGridSizeX);
            if (slot < 0 || slot >= moved.length || moved[slot] != null) {
                return; // unexpected layout: let vanilla reposition (and rebuild) as usual
            }
            moved[slot] = section;
        }
        for (SectionRenderDispatcher.RenderSection section : moved) {
            seamless$move(section, dx, dz);
        }
        sections = moved;
    }

    /**
     * Like setOrigin, but keeps the built mesh (the blocks there are the same). A rebuild queued for
     * the old position was prepared with the old neighborhood, so cancel it (as setOrigin does) and
     * rebuild at the new position instead.
     */
    @Unique
    private static void seamless$move(SectionRenderDispatcher.RenderSection section, int dx, int dz) {
        RenderSectionAccessor access = (RenderSectionAccessor) section;
        if (access.seamless$cancelTasks()) {
            section.setDirty(false);
        }
        BlockPos.MutableBlockPos origin = access.seamless$origin();
        origin.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
        access.seamless$setBoundingBox(new AABB(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 16, origin.getY() + 16, origin.getZ() + 16));
        BlockPos.MutableBlockPos[] relative = access.seamless$relativeOrigins();
        for (Direction direction : Direction.values()) {
            relative[direction.ordinal()].set(origin).move(direction, 16);
        }
    }
}
