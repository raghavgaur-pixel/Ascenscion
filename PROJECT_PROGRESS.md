# Project Progress

## Current Phase

Phase 9: Ability and content systems — **complete** on `phase-9-foundation`.

## Completed

- Established package and documentation layout
- Defined architectural boundaries and development conventions
- Added Maven build descriptor with dependency and shading strategy
- Implemented plugin bootstrap and application lifecycle orchestration
- Implemented module contract and dependency-ordered module manager
- Implemented thread-safe service container and service registry
- Implemented configuration loading abstractions for default resource-backed YAML files
- Implemented Paper platform and scheduler adapters
- Implemented optional integration discovery registry
- Added architectural boundary packages for API, assets, math, version, network, testing, debug, devtools, and scripting
- Implemented a global registry framework with typed descriptors and a central registry hub
- Implemented a database framework with connection-provider abstraction, HikariCP pooling, async execution, transactions, migrations, and DAO support
- Added SQLite and PostgreSQL connection configuration support
- Implemented schema migration tracking with automatic migration application
- Implemented a modular player profile aggregate with component registration and structured component persistence
- Implemented built-in persistent profile components for settings, unlocked floors, currencies, statistics, achievements, and quest state
- Implemented async player profile loading, online caching, and shutdown saves
- Implemented an internal event bus with lifecycle event support, priority ordering, and listener isolation
- Implemented a named runtime task framework with sync, async, delayed, repeating, and owner-scoped cleanup support
- Implemented a centralized game loop with tick registration, ordered execution, metrics, and exception isolation
- Implemented reusable runtime state containers for cooldowns, flags, metadata, context variables, and state machines
- Implemented a disposable player session model separate from persistent profiles
- Implemented a player session manager with async profile loading, join and quit lifecycle handling, and safe shutdown disposal
- Implemented a shared injected game context facade for future gameplay modules
- Added a structured serialization codec registry with YAML, JSON, and binary-safe codec boundaries
- Added a typed configuration framework with versioning, validation, default generation, and reload-safe ownership
- Extended the registry framework with reloadable registries for hot-swappable definition sets
- Implemented the asset framework with immutable asset metadata, typed asset groups, validation, inheritance, and duplicate detection
- Added built-in definition groups for localization, items, skills, bosses, floors, quests, professions, loot tables, NPCs, mobs, abilities, and effects
- Implemented rollback-safe asset reload with registry integration and dependency validation
- Added a localization framework backed by translation bundle assets and typed localization settings
- Added equipment engine with separation of concerns from the item engine
- Implemented the Effects Engine with generic gameplay effect lifecycle management, duration ticking, modifier scaling, stacking, and teardown
- Implemented the Combat Engine foundation with a deterministic attack/damage pipeline, typed damage sources and types, immutable combat snapshots, isolated health mutation, and internal combat events
- Implemented Phase 9 canonical stat vocabulary and data-driven ability definitions
- Implemented ability request/result/service contracts, cooldown/resource enforcement, actor-scoped serialization, and rollback on failed execution
- Integrated authored abilities with CombatService and EffectsService
- Added real area target resolution for live LivingEntity targets
- Added persistent Ascension-side runtime state for non-player combatants
- Added configurable mob/boss logical health and runtime spawning bridge
- Added `/asc cast` support for any targeted LivingEntity
- Added persistent player quest state with active/completed objectives and round-trip serialization
- Added quest progression for NPC interaction, location reach, mob defeat, and boss defeat
- Added quest reward handling for XP, currencies, items, and floor unlocks
- Added combat-to-progression event bridge and automatic Floor 1 onboarding
- Added starter equipment content, Floor 2 unlock target, and referenced loot tables
- Added Phase 9 command surface for quest, mob, and floor vertical-slice testing
- Added Phase 9 asset packaging and quest-state tests
- Passed the GitHub Actions Maven/Java 21 build gate with all 17 tests green on the final Phase 9 head

## Not Started / In Progress

- Dungeons, world events, professions, guilds, parties, economy, and later-floor content
- Production-grade mob AI/pathing and encounter orchestration
- Full player-facing UI/menus and HUD systems
- Additional tower floors beyond the Phase 9 Floor 2 unlock target

## Risks / Constraints

- `phase-9-foundation` remains the integration branch; `main` has not been promoted.
- Phase 9 has a verified CI build/test gate, but actual live-server behavior still requires a Paper server integration run.

## Next Implementation Step

Phase 10 should build on this stable vertical slice with production mob AI, encounter state, player-facing quest/ability UI, loot resolution, and additional tower floors.
