package com.andrewparry.seamlessworldwrap.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.entity.vehicle.Boat.class)
public interface BoatLerpAccessor {

    @Accessor("lerpX")
    double seamless$lerpX();

    @Accessor("lerpX")
    void seamless$setLerpX(double x);

    @Accessor("lerpZ")
    double seamless$lerpZ();

    @Accessor("lerpZ")
    void seamless$setLerpZ(double z);
}
