package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cheap hack: draw every entity again at each of its aliases within render distance, so in a world
 * smaller than the view you see mobs, other players and yourself repeated like the terrain.
 * Copies are visual only. The client learns the world size from the server (WrapSyncPayload), so
 * this needs the mod on both sides.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ClientLevel level;

    @Shadow
    public abstract boolean isSectionCompiled(BlockPos pos);

    @Shadow
    protected abstract void renderEntity(Entity entity, double camX, double camY, double camZ, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource);

    @Unique
    private boolean seamless$renderingCopy;

    @Inject(method = "renderEntity", at = @At("TAIL"))
    private void seamless$renderCopies(Entity entity, double camX, double camY, double camZ, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (!seamless$renderingCopy) {
            seamless$copiesOf(entity, camX, camY, camZ, partialTick, poseStack, bufferSource);
        }
    }

    /** Vanilla never draws the camera entity in first person, but its copies are someone else's view. */
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", ordinal = 0))
    private void seamless$renderSelfCopies(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
                                           net.minecraft.client.renderer.GameRenderer gameRenderer,
                                           net.minecraft.client.renderer.LightTexture lightTexture,
                                           org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                           CallbackInfo ci, @Local PoseStack poseStack,
                                           @Local MultiBufferSource.BufferSource bufferSource) {
        Entity self = camera.getEntity();
        if (self == null || camera.isDetached()) {
            return; // in third person vanilla already drew it, and renderEntity drew its copies
        }
        Vec3 cam = camera.getPosition();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        seamless$copiesOf(self, cam.x, cam.y, cam.z, partialTick, poseStack, bufferSource);
    }

    /** Draw an entity (and so its copies) if the original or any copy is on screen. */
    @Redirect(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private <E extends Entity> boolean seamless$anyCopyVisible(EntityRenderDispatcher dispatcher, E entity, Frustum frustum,
                                                              double camX, double camY, double camZ) {
        if (dispatcher.shouldRender(entity, frustum, camX, camY, camZ)) {
            return true;
        }
        WrapSettings wrap = seamless$wrap();
        if (wrap == null) {
            return false;
        }
        boolean[] visible = {false};
        seamless$forEachCopy(entity, wrap, camX, camZ, (dx, dz) -> {
            if (!visible[0] && frustum.isVisible(entity.getBoundingBoxForCulling().move(dx, 0, dz))) {
                visible[0] = true;
            }
        });
        return visible[0];
    }

    /** The original's chunk section may not be built while a copy's is; don't skip the entity for that. */
    @Redirect(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean seamless$sectionCompiled(LevelRenderer renderer, BlockPos pos) {
        return seamless$wrap() != null || isSectionCompiled(pos);
    }

    @Unique
    private WrapSettings seamless$wrap() {
        return level == null ? null : ClientWrapState.forDimension(level.dimension());
    }

    @Unique
    private interface CopyConsumer {
        void accept(double dx, double dz);
    }

    /** Offsets of every copy of {@code entity} within render distance of the camera (not the original). */
    @Unique
    private void seamless$forEachCopy(Entity entity, WrapSettings wrap, double camX, double camZ, CopyConsumer action) {
        double reach = minecraft.options.getEffectiveRenderDistance() * 16.0;
        double ex = entity.getX();
        double ez = entity.getZ();
        int w = wrap.width();
        int h = wrap.height();
        int kMin = wrap.wrapsX() ? (int) Math.ceil((camX - reach - ex) / w) : 0;
        int kMax = wrap.wrapsX() ? (int) Math.floor((camX + reach - ex) / w) : 0;
        int mMin = wrap.wrapsZ() ? (int) Math.ceil((camZ - reach - ez) / h) : 0;
        int mMax = wrap.wrapsZ() ? (int) Math.floor((camZ + reach - ez) / h) : 0;
        double reachSq = reach * reach;
        for (int k = kMin; k <= kMax; k++) {
            for (int m = mMin; m <= mMax; m++) {
                if (k == 0 && m == 0) {
                    continue;
                }
                double dx = (double) k * w;
                double dz = (double) m * h;
                double rx = ex + dx - camX;
                double rz = ez + dz - camZ;
                if (rx * rx + rz * rz <= reachSq) {
                    action.accept(dx, dz);
                }
            }
        }
    }

    @Unique
    private void seamless$copiesOf(Entity entity, double camX, double camY, double camZ, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource) {
        WrapSettings wrap = seamless$wrap();
        if (wrap == null) {
            return;
        }
        seamless$renderingCopy = true;
        try {
            // Moving the camera the other way draws the entity shifted by (dx, dz).
            seamless$forEachCopy(entity, wrap, camX, camZ, (dx, dz) ->
                    renderEntity(entity, camX - dx, camY, camZ - dz, partialTick, poseStack, bufferSource));
        } finally {
            seamless$renderingCopy = false;
        }
    }
}
