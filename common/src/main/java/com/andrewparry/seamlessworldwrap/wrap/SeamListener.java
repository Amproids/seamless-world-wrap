package com.andrewparry.seamlessworldwrap.wrap;

/** Added to ServerGamePacketListenerImpl: moves its remembered "last good" positions along with the player. */
public interface SeamListener {

    void seamless$shiftRemembered(int dx, int dz);
}
