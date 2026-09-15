# TODO

## Phase 1: Architecture And Foundation

- [x] Create repository structure
- [x] Create root architecture and project tracking documents
- [x] Add Maven project baseline and dependency management
- [x] Add plugin bootstrap and lifecycle framework
- [x] Add module system with dependency ordering
- [x] Add lightweight dependency injection container
- [x] Add configuration service abstraction
- [x] Add scheduler and platform abstractions
- [x] Add integration registry boundary
- [ ] Add Maven wrapper once Maven or wrapper artifacts are available
- [x] Build and verify in a Maven-enabled CI environment

## Phase 2: Persistence, Profiles, And Registries

- [x] Add registry framework and registry hub
- [x] Add database configuration and engine selection
- [x] Add connection pool management
- [x] Add async database execution and transactions
- [x] Add schema migration framework
- [x] Add DAO support abstractions
- [x] Add repository pattern boundaries
- [x] Add modular player profile aggregate
- [x] Add component-based persistent profile data
- [x] Add async profile loading and online caching
- [x] Add shutdown profile save flow
- [x] Add structural boundary packages required by the architecture lock

## Phase 3: Runtime Engine And Player Lifecycle

- [x] Add internal event bus
- [x] Add lifecycle events
- [x] Add owned runtime task scheduler
- [x] Add centralized game loop
- [x] Add tick registration framework
- [x] Add reusable runtime state containers
- [x] Add disposable player session model
- [x] Add player session manager
- [x] Add Bukkit join and quit session bridge
- [x] Add injected game context facade

## Next Phases

- [x] Phase 4: typed configuration validation, asset loading, localization, and reload framework
- [x] Phase 5: stat, attribute, and item foundation
- [x] Define item asset schemas and runtime item stack abstraction
- [x] Add stat and attribute value objects plus calculation boundaries
- [x] Add item metadata encoding and decoding for Bukkit item storage
- [x] Add item registry-backed runtime lookup and item builder services
- [x] Phase 6: Equipment engine foundation
- [x] Phase 7: Effects engine foundation
- [x] Phase 8: Combat engine foundation

## Phase 9: Ability And Content Systems — COMPLETE

- [x] Establish canonical stat vocabulary and immutable stat values
- [x] Establish data-driven ability asset definition
- [x] Establish ability request/result/service contracts
- [x] Implement ability registry integration and reload validation
- [x] Implement cooldown/resource enforcement
- [x] Implement targeting and execution pipeline
- [x] Integrate abilities with CombatService and EffectsService
- [x] Add persistent player progression: level, XP, and tower progression state
- [x] Define tower/floor runtime contracts and progression rules
- [x] Add first playable content pack: Floor 1, starter equipment, abilities, mobs, quests, and boss
- [x] Add automated unit/integration tests for the Phase 9 contracts
- [x] Add build verification and CI gate
