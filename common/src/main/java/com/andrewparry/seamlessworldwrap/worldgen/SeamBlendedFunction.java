package com.andrewparry.seamlessworldwrap.worldgen;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Wraps a noise-sampling density function so it repeats every world width (and height, on a
 * torus) and joins up smoothly at the seams.
 *
 * <p>Positions are first wrapped into the real world. In the last {@code blendWidth} blocks before
 * a seam, the value fades from the function at that spot to the function at the same spot one
 * world-width earlier, which is the terrain that naturally continues into the start of the range.
 * So at the seam the two sides match exactly.
 *
 * <p>Only leaf functions (the ones that sample noise at the context position) are wrapped. Their
 * inputs, such as a shifted noise's offsets, may be evaluated at the shifted position too. That is
 * safe because NoiseChunk's caches fall back to direct computation for any context other than the
 * chunk itself.
 */
public record SeamBlendedFunction(DensityFunction inner, WrapSettings settings) implements DensityFunction {

    @Override
    public double compute(FunctionContext context) {
        int x = settings.wrapX(context.blockX());
        int z = settings.wrapZ(context.blockZ());
        double tx = settings.wrapsX() ? seamWeight(x - settings.minX(), settings.width()) : 0.0;
        double tz = settings.wrapsZ() ? seamWeight(z - settings.minZ(), settings.height()) : 0.0;

        if (tx == 0.0 && tz == 0.0) {
            return x == context.blockX() && z == context.blockZ() ? inner.compute(context) : sample(x, context.blockY(), z);
        }

        int y = context.blockY();
        int xPast = x - settings.width();
        int zPast = z - settings.height();
        if (tz == 0.0) {
            return lerp(tx, sample(x, y, z), sample(xPast, y, z));
        }
        if (tx == 0.0) {
            return lerp(tz, sample(x, y, z), sample(x, y, zPast));
        }
        return lerp(tz,
                lerp(tx, sample(x, y, z), sample(xPast, y, z)),
                lerp(tx, sample(x, y, zPast), sample(xPast, y, zPast)));
    }

    private double sample(int x, int y, int z) {
        return inner.compute(new SinglePointContext(x, y, z));
    }

    /** 0 outside the blend band; rises smoothly to 1 at the seam. {@code pos} is measured from the range start. */
    private double seamWeight(int pos, int size) {
        int bandStart = size - settings.blendWidth();
        if (pos < bandStart) {
            return 0.0;
        }
        double t = (pos - bandStart + 0.5) / settings.blendWidth();
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        DensityFunction mapped = inner.mapAll(visitor);
        if (mapped instanceof SeamBlendedFunction blended) {
            mapped = blended.inner;
        }
        return visitor.apply(new SeamBlendedFunction(mapped, settings));
    }

    @Override
    public double minValue() {
        return inner.minValue();
    }

    @Override
    public double maxValue() {
        return inner.maxValue();
    }

    /**
     * Only exists at runtime and is never written to data files, so it borrows the wrapped
     * function's codec type rather than registering its own.
     */
    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return inner.codec();
    }
}
