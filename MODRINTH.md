# Modrinth page text

## Summary (under 256 characters)
Makes the world a fixed size and connects opposite edges, so walking off one side puts you on the other. No wall and no loading screen. Works east-west, north-south, or both.

## Description
# Seamless World Wrap 🌍

Seamless World Wrap gives a world a fixed size and connects its opposite edges. Walk off the east edge and you come in from the west. There's no wall, no loading screen and no visible seam.

## Features
- Wrap east-west, north-south, or both.
- World size from 1 x 1 chunk up to 2048 x 2048 chunks.
- The Nether wraps at 1/8 of the Overworld size, so portals still line up.
- The End can match the Overworld, use its own size, or not wrap.
- Terrain is blended at the edges so it continues across them.
- Blocks, redstone, structures, mobs, minecarts and projectiles work across the edges.
- In small worlds you can see repeated copies of the world, yourself and mobs in the distance.
- "Start in a Forest" moves the terrain so the world is centered on dry land with trees.
- Worlds too small for a stronghold or fortress get an End portal room and a blaze spawner, and the dragon fight is scaled down to fit small Ends.

## How it works
There is only one copy of the world. Anything past an edge maps back into it, so both sides of an edge are the same blocks. Chunks near your landing point are sent before you reach the edge, and crossing moves you by exactly one world size without reloading what's on screen. Terrain blending uses Minecraft's own terrain system, so world generation mods built on it should blend at the edges too.

## Setup
Choose the settings on the World Wrap tab when creating a world. Settings are saved with each world.

Existing worlds are never changed. New worlds wrap by default; turn it off on the World Wrap tab. A wrapped world needs the mod to keep looping.

## Requirements
- Minecraft 1.21.1
- Fabric with Fabric API, or NeoForge
- Install on both the client and the server (in singleplayer, just your game)

## Known issues
This is an alpha. Back up worlds you care about.
- Mobs don't follow or pathfind across an edge yet.
- Light doesn't spread across an edge.
- An axis that doesn't wrap has no border.
- With Sodium, chunks near you rebuild briefly after crossing an edge.

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
