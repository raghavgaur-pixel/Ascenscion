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
- `core.cache`
  - Thread-safe in-memory cache boundaries
- `registry`
  - Global runtime registry framework and registry hub
- `serialization`
  - Structured serialization contracts for persisted module data
- `database`
  - Connection management, migrations, DAO support, and repositories
- `profiles`
  - Modular player profile aggregate and profile service
- `api`
  - Future public API surface for external integration
- `assets`
  - Future asset boundaries
- `math`
  - Future formula and math value objects
- `version`
  - Future version compatibility boundaries
- `network`
  - Future network and protocol boundaries
- `testing`
  - Future testing harness support
- `debug`
  - Future debug tooling boundaries
- `devtools`
  - Future developer tooling boundaries
- `scripting`
  - Future scripting boundaries

Future subsystem packages will be added without breaking this boundary structure:

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

## Runtime Modules

The module graph is now:

1. `core-infrastructure`
2. `registry`
3. `database`
4. `profiles`

Responsibilities are intentionally narrow:

- `core-infrastructure` creates directories, loads base configs, and discovers integrations.
- `registry` initializes the global `RegistryHub` and foundational registries.
- `database` loads database settings, starts the connection pool, and exposes migration services.
- `profiles` registers profile components, registers schema migrations, applies migrations, and exposes the profile service.

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
- Player profiles remain cached while online and are flushed asynchronously on shutdown.

## Versioning Strategy

- Prefer Paper API only.
- Avoid NMS.
- If version-specific code becomes necessary, isolate it under a dedicated compatibility boundary and keep gameplay modules version-agnostic.

## Configuration Strategy

- All gameplay values will eventually live in structured configs.
- No hardcoded identifiers for items, abilities, or floors.
- Bootstrap config files are loaded through a dedicated configuration service.
- Future systems will validate configuration on load and fail fast on invalid data.
- Database engine selection and pool sizing are configured through `config/database.yml`.

## Persistence Strategy

The persistence layer now follows a strict multi-layer shape:

1. `DatabaseService`
   - owns async execution, transactions, and the connection pool
2. DAO support
   - centralizes prepared-statement execution patterns
3. repositories
   - own aggregate persistence rules and SQL
4. services
   - coordinate repositories and runtime caching

Key decisions:

- SQLite and PostgreSQL share the same repository contracts.
- HikariCP owns connection pooling.
- Schema creation is handled through ordered migrations stored in `schema_migrations`.
- Prepared statements are mandatory through the `StatementBinder` contract.
- Transactions are explicit through `DatabaseTransaction`.
- No gameplay-facing service executes SQL directly.

## Player Profile Strategy

Profiles are intentionally modular rather than monolithic.

- Core profile metadata lives in the `player_profiles` table.
- Module-specific state lives in `profile_component_data`.
- Each profile component is registered through `ProfileComponentDefinition`.
- Components serialize to structured YAML payloads behind a serialization boundary.
- Future modules can add persistent profile data by registering a new profile component definition without editing the `PlayerProfile` aggregate.

Built-in profile components currently include:

- settings
- unlocked floors
- currencies
- statistics
- achievements

## Registry Strategy

The global registry framework exists to make content and system definitions discoverable without static managers.

- `RegistryDescriptor` defines the name and types of a registry.
- `RegistryHub` owns registry instances.
- `MutableRegistry` supports runtime registration and lookup.
- Foundational registries currently exist for schema migrations and profile components.
- Future systems such as items, skills, bosses, quests, dungeons, NPCs, floors, and achievements should register through the same framework.

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
2. Persistence, profiles, and registry foundation
3. Internal event bus and player session lifecycle orchestration
4. Typed configuration reload and validation framework hardening
5. Item framework
6. Combat and ability engine
