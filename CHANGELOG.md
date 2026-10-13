# Changelog

## 0.1.0-alpha (Minecraft 1.21.1, Fabric + NeoForge)

First release.

- Worlds that wrap around: east-west, north-south, or both, from 1 x 1 chunk up to 2048 x 2048.
- Crossing an edge is seamless: the land on the other side is preloaded, and nothing reloads or
  flickers when you cross.
- Build across the edge; blocks, chests, redstone, minecarts, mobs and arrows all work across it.
- The terrain blends across the edges, so the world looks like it was always one piece.
- World Wrap tab on the Create World screen. Settings are saved per world; worlds made before
  the mod was installed never wrap.
- The Nether wraps at 1/8 scale so portals line up; the End has its own settings (same as the
  Overworld by default).
- Start in a Forest: the world is centered on forested dry land.
- Small worlds are beatable: an End portal room and a blaze spawner room are added when the world
  is too small for a stronghold or fortress, and the dragon fight is scaled to fit small Ends.
- With the mod on the client too: copies of you and every mob are drawn across the edges
  (a hall of mirrors in tiny worlds), and hitboxes stay on the nearest copy.

Known limitations:
- Mobs don't yet chase or pathfind across an edge.
- Light doesn't carry across an edge.
- An axis that doesn't wrap is endless (no wall at the edge yet).
- With Sodium or another renderer replacement, nearby chunks rebuild briefly after crossing.
- Not yet tested: multiplayer with several players, joining with an unmodded client.
