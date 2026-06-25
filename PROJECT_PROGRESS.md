# Project Progress

## Current Phase

Phase 2: persistence framework, player profile system, and registry foundation.

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

## Not Started

- Gameplay systems
- Combat
- Items
- Tower progression
- Internal event bus
- Player session listeners and runtime profile lifecycle hooks

## Risks / Constraints

- Local environment currently lacks `mvn`, so compile verification is pending.
- `.git` metadata was present but initially inconsistent; repository content itself is now structured normally.
- Profile persistence is implemented, but Paper-side join and quit listeners have not been added yet because session orchestration belongs to the next phase.

## Next Implementation Step

Phase 3 should implement runtime orchestration:

- internal event bus contracts
- player session lifecycle listeners for join, quit, and shutdown
- profile service integration with live player events
- typed configuration validation and reload flow for module-owned configs
- groundwork for future item framework registration events
