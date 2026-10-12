package com.andrewparry.seamlessplanet.wrap;

import net.minecraft.network.protocol.Packet;

/**
 * Added by mixin to clientbound packets that carry a world position. Packets can be shared between
 * players, so shifting always works on a copy.
 */
public interface SeamShiftable {

    /** Reference block X used to pick the alias (e.g. a chunk's center). */
    double seamless$refX();

    double seamless$refZ();

    /** Moves every position in this packet by (dx, dz) blocks. Only called on copies. */
    void seamless$offset(int dx, int dz);

    Object seamless$copy();

    default Packet<?> seamless$shifted(SeamPacketTranslator.Frame frame) {
        int dx = frame.shiftX(seamless$refX());
        int dz = frame.shiftZ(seamless$refZ());
        if (dx == 0 && dz == 0) {
            return (Packet<?>) this;
        }
        SeamShiftable copy = (SeamShiftable) seamless$copy();
        copy.seamless$offset(dx, dz);
        return (Packet<?>) copy;
    }
}
