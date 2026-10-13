# Modrinth page text

## Summary (under 256 characters)
Walk off one edge of the world and arrive on the other, with no wall, loading screen, or visible seam. Build right across the edge, loop east to west or all four edges, and pick the world size.

## Description
# Seamless World Wrap 🌍

**Walk off the edge of the world... and come back on the other side.**

Your world becomes finite, with its edges stitched together. Keep walking in one direction and you end up right back where you started. No walls, no loading screens, and no visible seam.

## What you can do
- **Walk, fly, or sail around the whole world** and come back home
- **Build right across the edge**. There's only one copy of the world, so both sides really are the same blocks
- **Shoot an arrow all the way around the world** and hit yourself in the back
- **Ride minecarts in a loop** around the world
- **Tiny worlds turn into a hall of mirrors**. You can see yourself and every mob repeating in the distance
- **Beat the game in one chunk**. Small worlds get an End portal room, a blaze spawner, and a dragon fight scaled to fit

## Under the hood
- **One real world, wrapped coordinates.** Everything past an edge maps back into the world with a modulus, so nothing is duplicated, and the chunks on the other side are the actual chunks.
- **Seamless crossing.** The chunks around your landing spot are preloaded before you reach the edge. When you cross, you're teleported by exactly one world size, and the chunks you already see are reused instead of rebuilt.
- **Seam-blended terrain.** The terrain noise is blended across each edge, so mountains, rivers, and biomes flow straight through it. This works on Minecraft's own terrain system, so most worldgen mods that build on it should blend too.
- **Cross-seam gameplay.** Block updates, redstone, structures, projectiles, and entity hitboxes all resolve across the edge.

## Shape your world
Open the **World Wrap** tab when you create a world:
- **Wrap East-West** and **Wrap North-South**: loop one axis (a cylinder) or both (a torus, like Pac-Man)
- **Width and length**: from a single chunk up to 2,048 chunks
- **Start in a Forest**: shifts the terrain so the world is centered on dry, forested land
- **The End**: same as the Overworld, its own size, or normal

The Nether wraps at 1/8 scale, so portal coordinates still line up. Settings are saved per world, and worlds made before the mod was installed never wrap.

## How to install
- Minecraft **1.21.1** with **Fabric** (plus Fabric API) or **NeoForge**
- Install on **both** the server and the client. In singleplayer, that's just your game

**Alpha release!** Back up any world you care about.

**Known limits:** mobs don't track you across an edge yet, light doesn't propagate across an edge, an axis that doesn't wrap is unbounded, and with Sodium nearby chunks rebuild briefly after crossing.

## Upload details
- Versions: `release/seamless_world_wrap-fabric-1.21.1-0.1.0-alpha.jar` (loader Fabric, needs Fabric API),
  `release/seamless_world_wrap-neoforge-1.21.1-0.1.0-alpha.jar` (loader NeoForge)
- Game version 1.21.1, release channel **Alpha**, version number 0.1.0-alpha
- Environment: Client and server, Required on both
- Categories: worldgen, adventure
- License: MIT
- Icon: `art/icon-512.png`
- Gallery: `art/screenshots/` (tiny world village, hall of mirrors portals, Nether copies of you, minecart loop)
- Changelog: see CHANGELOG.md
