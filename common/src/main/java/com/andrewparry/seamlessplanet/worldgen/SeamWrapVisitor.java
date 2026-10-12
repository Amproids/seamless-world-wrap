package com.andrewparry.seamlessplanet.worldgen;

import com.andrewparry.seamlessplanet.WrapSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Walks a noise router and wraps every noise-sampling leaf in a {@link SeamBlendedFunction}.
 * Leaves are recognized by their registered density function type, so this also works on
 * routers from datapacks and worldgen mods that use the vanilla types.
 */
public class SeamWrapVisitor implements DensityFunction.Visitor {

    /** Density function types that read the context position to sample noise. */
    private static final Set<ResourceLocation> LEAF_TYPES = Set.of(
            ResourceLocation.withDefaultNamespace("noise"),
            ResourceLocation.withDefaultNamespace("shifted_noise"),
            ResourceLocation.withDefaultNamespace("shift"),
            ResourceLocation.withDefaultNamespace("shift_a"),
            ResourceLocation.withDefaultNamespace("shift_b"),
            ResourceLocation.withDefaultNamespace("old_blended_noise"),
            ResourceLocation.withDefaultNamespace("weird_scaled_sampler")
    );

    private final WrapSettings settings;
    private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

    public SeamWrapVisitor(WrapSettings settings) {
        this.settings = settings;
    }

    @Override
    public DensityFunction apply(DensityFunction function) {
        return wrapped.computeIfAbsent(function, this::wrapNew);
    }

    private DensityFunction wrapNew(DensityFunction function) {
        if (function instanceof SeamBlendedFunction) {
            return function;
        }
        ResourceLocation type = BuiltInRegistries.DENSITY_FUNCTION_TYPE.getKey(function.codec().codec());
        return type != null && LEAF_TYPES.contains(type) ? new SeamBlendedFunction(function, settings) : function;
    }
}
