package com.andrewparry.seamlessworldwrap.worldgen;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

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
        // Holder references have no codec (calling it throws); the function they point to is visited separately.
        if (function instanceof SeamBlendedFunction || function instanceof DensityFunctions.HolderHolder) {
            return function;
        }
        ResourceLocation type = typeOf(function);
        return type != null && LEAF_TYPES.contains(type) ? new SeamBlendedFunction(function, settings) : function;
    }

    /** Registered type id, or null for runtime-only functions (from vanilla or other mods) that have none. */
    private static ResourceLocation typeOf(DensityFunction function) {
        try {
            return BuiltInRegistries.DENSITY_FUNCTION_TYPE.getKey(function.codec().codec());
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
