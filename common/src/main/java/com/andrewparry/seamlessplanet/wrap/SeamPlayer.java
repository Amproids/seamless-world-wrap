package com.andrewparry.seamlessplanet.wrap;

/**
 * Per-player offset between the server's canonical position and the position the player's client
 * sees: client = canonical + offset. It changes by one world-width each time the player crosses a
 * seam, so the client's coordinates stay continuous.
 */
public interface SeamPlayer {

    int seamless$offsetX();

    int seamless$offsetZ();

    void seamless$setOffset(int x, int z);
}
