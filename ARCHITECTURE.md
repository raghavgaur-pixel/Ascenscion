# Architecture

## Goals

Ascension is being structured as a long-lived MMORPG server platform rather than a short-lived Bukkit plugin. The architecture must support:

- Replaceable subsystems
- Testable business logic
- Strict separation between domain logic and Paper-specific infrastructure
- Async persistence and main-thread safe gameplay orchestration
- Gradual expansion into dozens of gameplay modules without collapsing into global state

## Architectural Style

The codebase follows a pragmatic Clean Architecture model:

- `core.bootstrap`
  - Plugin entrypoint and startup wiring
- `core.lifecycle`
  - Application lifecycle orchestration
- `core.module`
  - Module contracts, dependency ordering, startup and shutdown
- `core.di`
  - Lightweight dependency injection container for infrastructure services
- `core.service`
  - Shared runtime service registration
- `core.config`
  - Configuration loading and access boundaries
- `core.platform`
  - Paper platform abstractions
- `core.scheduler`
  - Main-thread and async scheduling abstraction
- `core.integration`
  - Optional external plugin integration boundaries
- `core.logging`
  - Logging abstraction

Future subsystem packages will be added without breaking this boundary structure:

- `profiles`
- `database`
- `items`
- `combat`
- `abilities`
- `weapons`
- `professions`
- `quests`
- `dungeons`
- `bosses`
- `tower`
- `guilds`
- `parties`
- `housing`
- `economy`
- `worldevents`
- `menus`
- `api`

## Dependency Direction

Dependencies flow inward:

1. Paper and third-party APIs are isolated to infrastructure adapters.
2. Modules depend on contracts and services, not concrete managers.
3. Gameplay systems will depend on domain services and repositories, not direct Bukkit state.
4. Persistence implementations will sit behind repository interfaces.

## Module Model

Every major subsystem is represented as a runtime module with:

- Stable module identifier
- Declared dependencies
- Deterministic startup order
- Deterministic reverse shutdown order

This avoids static initialization order problems and makes later subsystem extraction easier.

## Concurrency Model

- Bukkit world access remains on the main thread unless an API explicitly supports async usage.
- Database and external I/O must be async.
- Shared services are thread-safe or explicitly documented as main-thread only.
- Scheduler access is routed through an abstraction so async policy remains consistent.

## Versioning Strategy

- Prefer Paper API only.
- Avoid NMS.
- If version-specific code becomes necessary, isolate it under a dedicated compatibility boundary and keep gameplay modules version-agnostic.

## Configuration Strategy

- All gameplay values will eventually live in structured configs.
- No hardcoded identifiers for items, abilities, or floors.
- Bootstrap config files are loaded through a dedicated configuration service.
- Future systems will validate configuration on load and fail fast on invalid data.

## External Integrations

Integrations are optional and must degrade gracefully:

- PlaceholderAPI
- LuckPerms
- Vault
- WorldEdit
- WorldGuard
- ProtocolLib
- Citizens
- MythicMobs

Each integration will live behind an integration boundary and never contaminate core domain logic.

## Near-Term Phase Sequence

1. Architecture and runtime baseline
2. Configuration system hardening
3. Database layer
4. Profiles
5. Event bus
6. Item framework
7. Combat and ability engine

