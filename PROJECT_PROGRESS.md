# Project Progress

## Current Phase

Phase 3: runtime engine and player lifecycle foundation.

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

## Not Started

- Gameplay systems
- Combat
- Items
- Tower progression
- Typed configuration validation and reload
- Item registry-backed engine

## Risks / Constraints

- Local environment currently lacks `mvn`, so compile verification is pending.
- `.git` metadata was present but initially inconsistent; repository content itself is now structured normally.
- Runtime session orchestration is implemented, but deeper config validation and reload infrastructure still belongs to the next phase.

## Next Implementation Step

Phase 4 should harden the configuration system:

- typed config accessors and validation boundaries
- reload-safe config lifecycle
- per-module config registration and ownership
- data definition loading patterns needed before the item framework
