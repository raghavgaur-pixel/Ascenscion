# Ascension Floor 1 — External Build Sources

This document records third-party Minecraft builds considered for the Floor 1 visual direction.

## Primary candidates

### Epic Medieval Kingdom — Pixelbiester
- Source: https://www.planetminecraft.com/project/huge-2000x2000-medieval-world-download/
- Creator download page: https://www.patreon.com/pixelbiester/posts/epic-medieval-3-89499139
- Scale: 2000 x 2000
- Includes two castles, a harbor city, two villages, taverns, a farm, custom terrain/vegetation and connected pathways.
- Full download is distributed through the creator's Patreon; obtain the build from the creator before use.
- Intended use in Ascension: overall world layout / city-scale reference and, once properly licensed/downloaded, potential foundation world.

### Medieval Town — Ybreis
- Source: https://www.planetminecraft.com/project/medieval-town-6975069/
- The page states the schematic is about 300k blocks, or about 42k blocks when excluding air/entities with `//paste -e -a`.
- Terms shown on the page permit use on a server and personal modification, while prohibiting claiming the build as one's own or redistributing modified copies.
- Intended use in Ascension: modular Haven districts and streets, subject to retaining creator attribution and complying with the source terms.

### Medieval Town Pack — mattwillen
- Source: https://www.planetminecraft.com/project/medieval-town-pack-download/
- 37 hand-crafted schematics including a castle, houses, towers, church/graveyard, blacksmith, warehouse, market shops, gate, modular walls and a complete village.
- The creator states the pack may be used for anything provided the structures are not claimed as one's own.
- Intended use in Ascension: reusable Haven building library.

## Licensed specialist pieces

### EcoSMP Medieval Tower
- Source: https://www.planetminecraft.com/project/medieval-tower-free-download-6318736/
- License shown: CC BY-NC-SA 4.0.
- Intended use: watchtowers / skyline landmarks, with attribution and license compliance.

### EcoSMP Big Medieval House
- Source: https://www.planetminecraft.com/project/big-medieval-house-build-free-schematic-download-full-video-tutorial-medieval-cottagecore-fantasy-whimsical/
- License shown: CC BY-NC-SA 4.0.
- Intended use: residential anchors / guild or noble properties, with attribution and license compliance.

### EcoSMP Mini Medieval Castle
- Source: https://www.planetminecraft.com/project/minecraft-small-medieval-castle-free-schematic-download-amp-full-video-tutorial/
- License shown: CC BY-NC-SA 4.0.
- Intended use: barracks / guard captain residence / small fortress landmarks, with attribution and license compliance.

### Thaerix Medieval Black Castle
- Source: https://www.planetminecraft.com/project/medieval-black-castle/
- The page states the schematic is under a Creative Commons license and asks for credit.
- Intended use: dark-stone secondary fortress or First Gate visual reference; verify exact license details before redistribution.

## Visual references only unless creator permission is confirmed

### Windfall Haven — Ragnar le Rouge
- Source: https://www.planetminecraft.com/project/medieval-island-3950300/
- Highly relevant visual reference for vertical citadel architecture, layered walls, towers, terrain integration, gates and dramatic skyline composition.

### Medieval City — Amonos / JINTUBE
- Source: https://www.youtube.com/watch?v=1OAJ14BYwU8
- Strong visual reference for a compact fortified city with a grand stone castle, surrounding houses, water features, and agricultural land.
- Creator page states © JINTUBE with no copying/modification without permission, so use as visual reference only unless permission is obtained.

### Nerima Kingdom — Timtenth_Buildings
- Source: https://www.planetminecraft.com/project/nerima-kingdom/
- Strong visual reference for a dense fantasy kingdom, cliffside construction, harbor, circular civic spaces and large-scale walls.
- Download is distributed through the creator's Patreon.

## Integration policy

Ascension should not silently redistribute third-party schematics whose terms do not explicitly permit redistribution. The preferred workflow is:

1. Obtain the chosen schematic/world directly from the creator/source.
2. Place the original asset into `plugins/Ascension/builds/` or provide it to the development workspace.
3. Use Ascension's build importer/placement manifest to integrate it into Floor 1.
4. Preserve attribution and source-license information in this file and in the runtime build manifest.
5. Do not claim third-party structures as original Ascension work.
