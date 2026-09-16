# Floor 1 Authored Asset Pipeline

Phase 9 now uses an authored world bundle instead of the procedural rectangle/box builders as the primary Floor 1 environment.

## Runtime flow

1. `AuthoredFloorOneProvisioner` looks for `Ascension-Floor1-Authored-Assets-v1.zip` in the Ascension plugin data directory.
2. On first install it replaces the legacy Phase 9 Floor 1 world when the old Ascension build marker is present.
3. The bundle's `world/` directory becomes `ascension_floor_001`.
4. The bundle's First Gate Sponge schematic is extracted to `plugins/Ascension/floor1-assets/`.
5. `AuthoredStructurePaster` uses WorldEdit to paste the First Gate castle once.
6. Existing Phase 9 gameplay services continue to provide NPCs, quests, starter gear, combat, mobs and boss progression.

## Authored map layout

The supplied Medieval Worldcraft Castle world is the base map and is shifted into the existing Phase 9 coordinate space so the current gameplay coordinates remain usable. Celestia Town is inserted as a secondary district to the east.

The First Gate castle schematic is authored from the supplied Medieval Town Collection 1 castle Litematic and is pasted at:

- X: `-50`
- Y: `70`
- Z: `690`

The existing gameplay trigger for the First Gate remains at the north approach, so crossing the gate area can awaken the boss encounter.

## Supplied donor sources

- Medieval Worldcraft Castle 1.21.1 — Silveran666 / CurseForge.
- Celestia Town by leolerenard — Fireless007 listing / CurseForge; supplied world is versioned from 1.20.6 and is used as donor terrain/structures in the combined world.
- Medieval Town Collection 1 Castle — elaineandsparky; supplied as Litematic and converted to Sponge Schematic v2 for WorldEdit.
- Gendry's Tavern v1.2 (Hardcore Mode) — TryzzMC / CurseForge. The supplied release is Minecraft 1.21.11 while Ascension Phase 9 targets Paper 1.21.1, so it is retained as a donor source and is not inserted into the 1.21.1 world bundle yet.

Creator credits should remain with the respective original works whenever the resulting map or asset bundle is shared.
