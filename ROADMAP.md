# Seamless World Wrap: roadmap

Released: **0.1.0-alpha** (2026-10-13), Minecraft 1.21.1, Fabric + NeoForge.
Order below is Andrew's priority: a few bugs first, then world generation, then compatibility,
then other Minecraft versions.

## 1. Bugs (next patch)
- Mobs don't see, chase or pathfind across an edge (wrap-aware distances for AI targeting and
  path nodes; Andrew's earlier goal).
- Light doesn't carry across an edge (light storage lookups by canonical section).
- An axis that doesn't wrap is endless: add a hard border (world border or wall) at its edges.
- Entity collisions / melee hits right at an edge within one tick (wrap-aware entity queries,
  like the projectile fix).
- Still untested: multiplayer with several players; an unmodded client joining (if it works, switch
  Modrinth to "Optional on client"); a village generating across an edge in game.
- Seen once, not reproduced: a brief freeze/stall in the Fabric test world (01:37, 2026-10-13).

## 2. World generation
- **Polar ice caps**: frozen biomes and ice at the north/south edges of a cylinder world.
- **Realistic continents**: Earth-like oceans and land masses; climate bands by latitude.
- **Bigger size limits**: beyond 2048 x 2048 chunks (up to the vanilla 30M-block border).
  Example target: 2048 x 1024 chunks is Earth at about 1 block = 1 km (2:1, equirectangular).
- Work with continental/terrain worldgen mods (blending is built on vanilla density functions,
  so most should mostly work; test and patch the ones that don't).

## 3. Compatibility (in this order)
1. **Optimization mods**: Sodium (needs its own seam-jump renderer fix; vanilla's is done),
   Lithium, C2ME (parallel chunk generation vs. our generation hooks), FerriteCore, ModernFix.
2. **Immersive Portals** (the "famous portal mod").
3. **Distant Horizons**: far-away LODs should show the wrapped world.
4. **Shaders**: Iris / Oculus.

## 4. Other Minecraft versions
- Newer versions (26.x), where Exotic Worlds and RingWorld are.
- 1.20.1 Forge, for the big modpacks still on it.

## Ideas parked along the way
- Pole crossing ("over the top" lands you half the world away) with a polar whiteout to hide
  the mirroring (see PLAN.md).
- Show the real (wrapped) position somewhere for players.
- `/worldwrap map` is a dev tool; maybe a nicer in-game map of the whole planet.
