package com.andrewparry.seamlessworldwrap.mixin.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface RenderSectionAccessor {

    @Accessor("buffers")
    Map<RenderType, VertexBuffer> seamless$buffers();

    @Accessor("dirty")
    boolean seamless$dirty();

    @Accessor("dirty")
    void seamless$setDirty(boolean dirty);
}
