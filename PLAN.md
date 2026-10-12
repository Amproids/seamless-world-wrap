# Minecraft Mod — Plan

Started 2026-10-12.

## The idea

**A world with no edges.** A finite world (e.g. 1000 x 1000 blocks) where walking off any side
brings you back in somewhere else, with no wall, seam, or loading screen. The player picks how
each pair of edges behaves when they create the world.

**Scope:** the core mod is the *wrapping* plus *seamless world generation* that works with other
worldgen mods. Realistic Earth-like terrain (ice caps, climate bands) is a **separate add-on**
built on top of it, not part of the core mod.

### World options (set at world creation)

- **Width and height** in chunks (default 64 x 64 = 1024 x 1024 blocks).
- **Shape:**
  - **Cylinder:** east/west wrap; north/south are hard borders.
  - **Torus:** all four edges wrap, Pac-Man style.

Pole crossing (going over the top and coming down at x + width/2) was considered and **dropped**.
On a flat block grid it acts as a mirror: walking along the edge, the far side slides the opposite way.
It could come back later with the pole hidden in a whiteout zone.

### Core mechanics

1. **One real copy of each chunk.** Chunks past an edge are views of the real chunk they map to
   (aliased storage), not separate copies. That keeps them in sync with nothing to mirror.
2. **The Portal trick.** Near an edge you see the other side through those aliased chunks. Crossing
   moves you to the matching real spot, and since your surroundings are identical, the move is invisible.
3. **Everything crosses edges:** entities, lighting, fluids, redstone, pathfinding, multiplayer views.

### Seamless world generation

Plan: **blend the noise across each seam**. In a band near an edge, crossfade the noise at a point
with the noise at the spot that edge maps to (x - width on X; z - height on Z for a torus).

- Do it at the **density-function** level, inside Minecraft's data-driven generator. Worldgen mods
  that add or replace density functions and biome climate noise then get wrapped automatically.
  That's the compatibility win.
- Crossfading two unrelated noise fields **flattens the blend band**: the average of two random
  hills is a gentler hill. It needs a contrast correction, or a wide enough band that it isn't noticeable.
- Blending doesn't cover everything. Trees, ores, and structures such as villages are placed by
  position-based randomness, so their placement has to be wrapped too (wrapped chunk coordinates,
  structures allowed to straddle a seam).
- Mods that ship a completely custom chunk generator (not density functions) won't be covered
  automatically. A fallback is to blend the generated heightmap/blocks, which is uglier. Maybe
  add per-mod compatibility patches.

### Prior art to study (and stand out from)

- Looping World: world-border wrap teleport, server-side (1.19.2–1.21.1)
- Circumnavigate: tiles a finite world (alpha, 1.21.1)
- Toroidal World: wraps both axes, chosen at world creation (Fabric + NeoForge)
- Planet: border teleport, server-side
- RingWorld: finite ring Overworld (Fabric 26.1.2)

Ways to stand out: truly seamless (no visible teleport), player-chosen shape and size, and worldgen blending that works with other worldgen mods, on current versions.

### Still open

- Overworld only, or the Nether/End too?
- New worlds only, or convert existing ones? (Probably new worlds only.)

## Add-on (later): realistic planet generation

Earth-like terrain built on the core mod: ice caps (on a cylinder, at the N/S borders), climate bands by latitude,
continents and oceans. Ships as a separate mod that depends on the core.

## Target platform

**Minecraft 1.21.1, Fabric + NeoForge**, built from one codebase (switched from 26.3 on 2026-10-12).

Why 1.21.1: modded players are concentrated on 1.21.1 and 1.20.1, not the latest release. The biggest
current pack, All the Mods 10 (~22M downloads), is on 1.21.1 NeoForge, and Cobbleverse is on 1.21.1
Fabric. 1.20.1 Forge has the most packs overall (~47.8K vs ~18.8K). The worldgen mods we want to
be compatible with live on these versions, and worldgen code changes a lot between versions, so
porting later is expensive.

Layout (from jaredlll08/MultiLoader-Template, 1.21.1 branch):
- `common/`: all gameplay/worldgen code, vanilla Minecraft only (Mojang names + Parchment).
- `fabric/`, `neoforge/`: thin bootstrap per loader, plus loader-specific mixins/hooks.
- `platform/Services`: Java ServiceLoader bridge for anything common code needs from the loader.

Versions: Fabric Loader >= 0.16.9, Fabric API 0.116.17+1.21.1, NeoForge 21.1.259 (requires 21.1+),
Java 21. The build runs on JDK 21 (`gradle/gradle-daemon-jvm.properties`); Gradle 8.10 can't run on Java 25.

**Later:** back-port to 1.20.1 Forge if there's demand; forward-port to newer versions.

## Progress

- [x] Name: **Seamless Planet** (mod ID `seamless_planet`, package `com.andrewparry.seamlessplanet`). License: MIT (changeable).
- [x] Multi-loader project (1.21.1). `./gradlew build` produces `fabric/build/libs/seamless_planet-fabric-1.21.1-<version>.jar` and `neoforge/build/libs/seamless_planet-neoforge-1.21.1-<version>.jar`.
- [x] `WorldShape` / `WrapSettings`: shape, size, and coordinate-wrapping math.

## Next steps (milestones)

1. **Seamless worldgen:** blend density-function noise at the seams. Testable on its own: generate a
   world, then fly to x = 0 / x = width and check that the terrain on both sides matches.
2. **Wrapped chunk storage:** chunks past an edge alias the real chunk, so the world repeats forever.
3. **Invisible teleport:** move players and entities back into the real range when they cross.
4. **Everything across the seam:** lighting, fluids, redstone, entities rendered from the other side.
5. **World-creation screen:** shape and size options.
6. **Publish:** Modrinth + CurseForge page, icon, screenshots.
