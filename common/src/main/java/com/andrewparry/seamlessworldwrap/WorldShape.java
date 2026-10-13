package com.andrewparry.seamlessworldwrap;

/** How the edges of a Seamless World Wrap world connect. */
public enum WorldShape {
    /** East and west edges wrap; north and south edges are hard borders. */
    CYLINDER,
    /** All four edges wrap, Pac-Man style. */
    TORUS;

    public boolean wrapsZ() {
        return this == TORUS;
    }
}
