# Modrinth page text

## Summary (under 256 characters)
Turn your world into a planet: walk off one edge and arrive on the other, with no wall, loading screen, or visible seam. Build right across the edge, loop east to west or all four edges, and pick the world size.

## Description
# Seamless World Wrap 🌍

**Walk off the edge of the world... and come back on the other side!**

Your world becomes a planet. Keep walking in one direction and you end up right back where you started. No walls, no loading screens, and no line where it joins.

## What you can do
- 🚶 **Walk, fly, or sail around the whole world** and come back home
- 🧱 **Build right across the edge**. Both sides are the same blocks!
- 🏹 **Shoot an arrow all the way around the planet** and hit yourself in the back
- 🚃 **Ride minecarts in a loop** around the world
- 🪞 **Tiny worlds look like a hall of mirrors**. You can see yourself and your mobs repeating in the distance!
- 🐉 **Beat the game in one chunk**. Small worlds get an End portal room, a blaze spawner, and a dragon fight that fits

## Pick your planet
Open the **World Wrap** tab when you create a world:
- **Wrap East-West** and **Wrap North-South**: loop one way (a cylinder) or both (like Pac-Man)
- **Width and length**: from a single chunk to 2,048 chunks
- **Start in a Forest**: start on dry land with trees
- **The End**: same as the Overworld, its own size, or normal

The Nether wraps at 1/8 scale, so your portals still line up. The terrain blends at the edges, so it looks like it was always one world.

## How to install
- Minecraft **1.21.1** with **Fabric** (plus Fabric API) or **NeoForge**
- Put it on the **server** (or in singleplayer, your game) to make the world loop
- Put it on your **client** too for the seamless crossing and the mirror copies of players and mobs

⚠️ **This is an early (alpha) version!** Back up any world you care about.

**Known limits:** mobs don't chase you across an edge yet, light doesn't carry across an edge, and a direction that doesn't loop goes on forever.

## Upload details
- Versions: `release/seamless_world_wrap-fabric-1.21.1-0.1.0-alpha.jar` (loader Fabric, needs Fabric API),
  `release/seamless_world_wrap-neoforge-1.21.1-0.1.0-alpha.jar` (loader NeoForge)
- Game version 1.21.1, release channel **Alpha**, version number 0.1.0-alpha
- Environment: server required, client optional
- Categories: worldgen, adventure
- License: MIT
- Icon: `art/icon-512.png`
- Gallery: `art/screenshots/` (tiny planet village, hall of mirrors portals, Nether copies of you, minecart loop)
- Changelog: see CHANGELOG.md
