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
  - Typed configuration descriptors, validation, and reload-safe ownership
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
- `events`
  - Internal event bus and lifecycle events
- `task`
  - Named runtime task scheduling and ownership tracking
- `runtime.tick`
  - Centralized game loop and tick registration
- `session`
  - Disposable player runtime state and session lifecycle
- `state`
  - Reusable transient state containers
- `context`
  - Injected runtime context facade
- `api`
  - Future public API surface for external integration
- `assets`
  - Asset metadata, typed definition loading, localization, and hot reload
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
2. `runtime-engine`
3. `registry`
4. `assets`
5. `database`
6. `profiles`
7. `sessions`

Responsibilities are intentionally narrow:

- `core-infrastructure` creates directories, loads base configs, and discovers integrations.
  - It also exposes structured serialization codecs and the typed configuration service.
- `runtime-engine` initializes the internal event bus, task framework, and centralized game loop.
- `registry` initializes the global `RegistryHub` and foundational registries.
- `assets` owns typed config-backed asset loading, validation, localization, and reload-safe registry population.
- `database` loads database settings, starts the connection pool, and exposes migration services.
- `profiles` registers profile components, registers schema migrations, applies migrations, and exposes the profile service.
- `sessions` bridges Bukkit join and quit events into runtime sessions and exposes the shared `GameContext`.

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
- Session initialization crosses threads explicitly: profile loading is async, runtime session mutation returns to the main thread.

## Versioning Strategy

- Prefer Paper API only.
- Avoid NMS.
- If version-specific code becomes necessary, isolate it under a dedicated compatibility boundary and keep gameplay modules version-agnostic.

## Configuration Strategy

- All gameplay values will eventually live in structured configs.
- No hardcoded identifiers for items, abilities, or floors.
- Bootstrap config files are loaded through a dedicated configuration service.
- Versioned typed configs are loaded through `TypedConfigurationService`.
- Typed configs are module-owned, validated on load, and reloadable without partial replacement.
- Config defaults are generated from codecs rather than scattered raw map access.
- Database engine selection and pool sizing are configured through `config/database.yml`.
- Asset and localization framework settings are configured through `config/assets.yml` and `config/localization.yml`.

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

`PlayerProfile` and `PlayerSession` are intentionally separate:

- `PlayerProfile`
  - persistent, reconnect-safe data
- `PlayerSession`
  - disposable runtime state for an online player

Built-in profile components currently include:

- settings
- unlocked floors
- currencies
- statistics
- achievements

## Item Strategy

Phase 5 establishes a component-based runtime item model.

- `ItemDefinition` acts as an immutable blueprint and is loaded via the `AssetService`.
- Composition over inheritance: Item blueprints utilize models such as `ItemRarity`, `StatLine`, `SocketSchema`, `UpgradeSchema`, and `EvolutionSchema` instead of subclasses like `SwordItem`.
- `AscensionItem` acts as the runtime representation containing instance state and identity.
- Platform boundaries: The engine avoids passing Bukkit `ItemStack` objects directly. `BukkitItemMetadataEncoder` handles saving/loading of unique identities into the `PersistentDataContainer` behind an `ItemMetadataEncoder` interface.

## Equipment Strategy

Phase 6 establishes a dedicated Equipment Engine separate from the Item Engine.

- **Separation of Concerns:** The Item Engine defines "what an item is" while the Equipment Engine defines "what happens when an item is equipped".
- **Runtime State:** Equipment is considered runtime state and is tied to the `PlayerSession`. Persistent storage remains the responsibility of the `PlayerProfile`.
- **Stat Integration:** Equipment never modifies calculated stats directly. All stat changes flow through the existing Stat & Modifier Engine. The Equipment Engine only applies and removes `AttributeModifier`s.
- **Event-Driven:** Runtime gameplay systems communicate via the internal `EventBus`. The Equipment Engine fires events (e.g., `ItemEquippedEvent`) rather than coupling modules directly.
- **Platform Independence:** Gameplay systems interact with the platform-independent `AscensionItem` and `EquipmentContainer` abstractions. Bukkit remains strictly an adapter layer.
- **Data-Driven & Composition:** The engine prefers composition over inheritance. Future equipment features are assembled from reusable components registered within `ItemDefinition`, and loaded via the `AssetService`.

## Registry Strategy

The global registry framework exists to make content and system definitions discoverable without static managers.

- `RegistryDescriptor` defines the name and types of a registry.
- `RegistryHub` owns registry instances.
- `MutableRegistry` supports runtime registration and lookup.
- `ReloadableRegistry` adds atomic map replacement for reload-driven systems.
- Foundational registries currently exist for schema migrations, profile components, asset types, and built-in asset definition groups.
- Future systems such as items, skills, bosses, quests, dungeons, NPCs, floors, and achievements should register through the same framework.

## Serialization Strategy

- `SerializedObject` remains the stable structured data boundary between persistence, typed config, and assets.
- `SerializedObjectCodecRegistry` resolves pluggable codecs by format.
- YAML and JSON are implemented now; binary-safe transport is represented through a Base64-wrapped structured codec boundary.
- Future binary formats should remain behind the same codec registry and must not leak format-specific parsing into gameplay code.

## Asset Strategy

Phase 4 establishes a data-first asset pipeline for future MMORPG content.

- Every gameplay-facing definition should eventually be loaded as an immutable `AssetDefinition`.
- Common identity and metadata live in:
  - `AssetId`
  - `AssetDescriptor`
  - `SemanticVersion`
  - `AssetCompatibility`
  - `AssetReference`
- `AssetType<T>` describes how an asset group is discovered, validated, serialized, and registered.
- `AssetService` is the only engine service that should discover asset files, validate them, and populate registries.
- Asset discovery supports:
  - YAML and JSON inputs
  - duplicate detection
  - version compatibility checks
  - dependency validation
  - inheritance through `extends`
  - rollback-safe reload behavior
- Built-in definition groups now exist for:
  - localization bundles
  - items
  - skills
  - bosses
  - floors
  - quests
  - professions
  - loot tables
  - NPCs
- These are data definitions only. No gameplay behavior belongs in them.

## Localization Strategy

- Player-facing framework text should resolve through `LocalizationService`.
- Translation bundles are assets, not ad-hoc message files.
- Bundle lookup is namespace-aware and driven by typed localization settings.
- Fallback language resolution is centralized so future GUI and narrative systems can reuse the same service.

## Runtime Engine Strategy

Phase 3 introduces a dedicated runtime foundation that gameplay systems will build on rather than bypass.

### Internal Event Bus

- Internal systems communicate through `EventBus` instead of coupling themselves directly to Bukkit events.
- Listeners declare priority and owner identifiers.
- Listener exceptions are isolated and logged without stopping dispatch.
- Lifecycle events currently include:
  - module loaded
  - module unloaded
  - server ready
  - server shutdown
  - player session created
  - player session loaded
  - player ready
  - player leaving
  - player session saved
  - player session destroyed

### Task Framework

- Gameplay-facing code should schedule named runtime tasks through `RuntimeTaskService`.
- Tasks are owned by module identifiers for shutdown cleanup.
- Sync and async immediate, delayed, and repeating tasks are supported.
- Modules do not need direct Bukkit scheduler access.

### Game Loop

- A single centralized repeating task drives `GameLoop`.
- Future gameplay systems should register `TickTask` instances instead of creating their own repeating Bukkit tasks.
- Tick execution is ordered by `TickPriority`.
- Tick exceptions are isolated and per-task timings are tracked.

### Session Framework

- `PlayerSessionManager` creates sessions on join and destroys them on quit or shutdown.
- Session state holds only runtime data:
  - world and region identifiers
  - runtime variables
  - cooldowns
  - flags
  - metadata
  - active effects
  - combat engagement state
  - temporary social references
- Persistent data remains in `PlayerProfile`.

### Game Context

- `GameContext` is an injected facade for composition-time access to the event bus, task system, game loop, profile service, session manager, registries, and database service.
- It exists to reduce repetitive constructor fan-out in higher-level modules, not to justify hidden global lookups.

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

## Effects Strategy

Phase 7 establishes a universal runtime engine for managing temporary and persistent gameplay effects.

- **Separation of Concerns:** The Effects Engine orchestrates effect lifecycles, durations, stacking behaviors, and event emissions. It does not hardcode mechanics like bleeding or regeneration.
- **Runtime State:** Effects (`EffectInstance`) are bound to the runtime `PlayerSession` via `EffectContainer`.
- **Modifier Integration:** The Effects Engine never directly alters calculated stats. Instead, it interacts directly with the existing Modifier framework to apply/remove `AttributeModifier` instances.
- **Data-Driven Rules:** Exposes `EffectRule` for determining application validity (e.g. immunity, category constraints), supporting future combat extensions naturally.
- **Session Lifecycle:** Automatically purges active effects gracefully upon player logout or session destruction via the `EffectSessionListener`.

## Near-Term Phase Sequence

1. Architecture and runtime baseline
2. Persistence, profiles, and registry foundation
3. Runtime engine and player session lifecycle orchestration
4. Typed configuration reload and validation framework hardening
5. Item framework
6. Equipment engine
7. Effects engine
8. Combat and ability engine
