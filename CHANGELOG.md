# Changelog

## 2026-06-25

- Initialized Ascension repository structure for Phase 1.
- Added architecture, development, progress, todo, and changelog documentation.
- Added Maven project descriptor with Paper-focused dependency management.
- Added foundational runtime framework for bootstrap, modules, DI, configuration, scheduling, and integration discovery.
- Added architecture-lock boundary packages for future API, networking, scripting, tooling, and diagnostics concerns.
- Added a typed global registry framework and registry module.
- Added the database layer foundation with engine selection, HikariCP pooling, async execution, transactions, DAO support, and migrations.
- Added SQLite and PostgreSQL connection providers and default database configuration.
- Added a modular player profile system with persistent component registration, structured component serialization, repository persistence, and online profile caching.
- Added the Phase 3 runtime engine with an internal event bus, lifecycle events, runtime task framework, centralized game loop, and reusable runtime state containers.
- Added a player session framework with join and quit lifecycle orchestration, async profile attachment, runtime session disposal, and an injected game context.
