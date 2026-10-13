# Release checklist: Seamless World Wrap 0.1.0-alpha

## Testing (Claude runs the game, Andrew plays)
- [ ] **NeoForge launches** and a wrapped world loads (World Wrap tab, crossing an edge, portal room)
- [ ] **Big world** (64 x 64 chunks): cross an edge; no missing chunks or reloading where you land
- [ ] **Multiplayer**: local server, two players see each other across an edge; settings reach the client
- [ ] **Unmodded client** joins the wrapped server and can walk across an edge (no copies/smooth crossing expected)
- [ ] **Existing world** from before the mod: loads and does NOT wrap
- [ ] **Normal world** (both Overworld switches off): plays like vanilla

## Build (Claude)
- [ ] Version set to `0.1.0-alpha`
- [ ] Dev-only mods (Xaero's, Mod Menu) are not in the release jars
- [ ] Release jars built: `seamless_world_wrap-fabric-1.21.1-0.1.0-alpha.jar`, `seamless_world_wrap-neoforge-1.21.1-0.1.0-alpha.jar`
- [ ] Each jar loads on its own in a clean launcher instance (with Fabric API for Fabric)
- [ ] Changelog written
- [ ] Description updated (World Wrap tab, Start in a Forest, one-chunk worlds, known limitations)

## Page assets (Andrew)
- [ ] Icon: `art/icon-512.png` (or the pixel version)
- [ ] 3 to 5 screenshots or a short clip (arrow in the back, one-chunk world, portal room, edge crossing)
- [ ] Decide: keep MIT license, or switch to LGPL-3.0

## Publish (Andrew)
- [ ] Create the Modrinth project: name, summary, description, icon, license, categories (worldgen, adventure)
- [ ] Environment: server required, client optional
- [ ] Upload Fabric jar (1.21.1, loader Fabric, dependency: Fabric API required)
- [ ] Upload NeoForge jar (1.21.1, loader NeoForge)
- [ ] Mark the versions as **Alpha**
- [ ] Submit for review; add payout method + tax form for withdrawals
- [ ] Optional: CurseForge too

## Known limitations (list on the page)
- Mobs don't chase or pathfind across an edge yet
- Light doesn't carry across an edge
- An axis that doesn't wrap is endless (no wall at the edge yet)
- Minecarts can flicker for one frame at an edge while you ride them
- Copies of players and mobs need the mod on the client
