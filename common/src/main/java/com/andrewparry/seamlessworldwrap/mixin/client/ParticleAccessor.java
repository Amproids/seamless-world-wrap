package com.andrewparry.seamlessworldwrap.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor("x")
    double seamless$x();

    @Accessor("y")
    double seamless$y();

    @Accessor("z")
    double seamless$z();

    @Accessor("xo")
    double seamless$xo();

    @Accessor("xo")
    void seamless$setXo(double xo);

    @Accessor("zo")
    double seamless$zo();

    @Accessor("zo")
    void seamless$setZo(double zo);
}
