# Release checklist: Seamless World Wrap 0.1.0-alpha

## Testing (Claude runs the game, Andrew plays)
- [x] **NeoForge launches** and a wrapped world loads (World Wrap tab, crossing an edge, portal room)
- [x] **Big world** (64 x 64 and 2048 x 2048): crossing is seamless (preloaded chunks, reused meshes, no blip)
- [ ] **Multiplayer**: local server, two players see each other across an edge; settings reach the client
- [ ] **Unmodded client** joins the wrapped server and can walk across an edge (no copies/smooth crossing expected)
- [ ] **Existing world** from before the mod: loads and does NOT wrap
- [ ] **Normal world** (both Overworld switches off): plays like vanilla

- [x] **Fabric** re-checked after the NeoForge fixes (fresh 16 x 16 world, crossings, portal room)
- [ ] **Village across an edge** generates whole (fix in, not yet seen in game)

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
- Copies of players and mobs, and the seamless crossing, need the mod on the client
- With Sodium (or another renderer replacement), crossing an edge rebuilds nearby chunk meshes (a brief flicker)
