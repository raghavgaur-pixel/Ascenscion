# Project Progress

## Current Phase

Phase 9: Ability and content systems — foundation in progress.

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
- Implemented built-in persistent profile components for settings, unlocked floors, currencies, statistics, and achievements
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
- Added built-in definition groups for localization, items, skills, bosses, floors, quests, professions, loot tables, and NPCs
- Implemented a rollback-safe asset reload service with registry integration and dependency validation
- Added a localization framework backed by translation bundle assets and typed localization settings
- Added default asset and localization directory scaffolding plus bundled seed localization data
- Added equipment engine with separation of concerns from the item engine
- Implemented the Effects Engine, introducing an abstract EffectService managing generic gameplay effect lifecycles, duration ticking, modifier scaling, and automatic teardown on player disconnect
- Implemented the Combat Engine foundation with a deterministic attack/damage pipeline, typed damage sources and types, immutable combat snapshots, isolated health mutation, and internal combat events
- Phase 9 foundation: canonical stat vocabulary, immutable stat values/sets, data-driven ability definitions, and explicit ability request/result/service contracts

## Not Started / In Progress

- Ability registry integration and runtime execution
- Resource and cooldown enforcement
- Targeting pipeline
- Player level/XP progression
- Tower runtime progression
- Gameplay content and first playable floor

## Risks / Constraints

- Local environment currently lacks `mvn`, so compile verification is pending.
- `javac` is available locally, but dependency-resolved project compilation still requires Maven or an equivalent build runner.

## Next Implementation Step

Implement the Phase 9 ability runtime: registry-backed definitions, cooldown/resource enforcement, validation, targeting contracts, and deterministic execution integration with CombatService and EffectsService.
