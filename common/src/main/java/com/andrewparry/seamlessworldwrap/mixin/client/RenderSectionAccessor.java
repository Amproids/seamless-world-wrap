package com.andrewparry.seamlessworldwrap.mixin.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface RenderSectionAccessor {

    @Accessor("origin")
    BlockPos.MutableBlockPos seamless$origin();

    @Accessor("bb")
    void seamless$setBoundingBox(AABB bb);

    @Accessor("relativeOrigins")
    BlockPos.MutableBlockPos[] seamless$relativeOrigins();
}
