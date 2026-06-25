# Ascension

Ascension is a production-oriented Paper plugin intended to power a large-scale Minecraft MMORPG centered on climbing a multi-world tower.

This repository is being built in strict subsystem order. Phase 1 establishes architecture, project structure, build configuration, and the foundational runtime framework. Gameplay systems are intentionally deferred until the foundation is stable.

## Technology Baseline

- Java 21
- Maven
- Paper API
- Adventure API
- SQLite for development
- PostgreSQL for production

## Current Scope

- Clean Architecture project baseline
- Modular runtime bootstrap
- Lightweight dependency injection container
- Configuration loading framework
- Lifecycle and module orchestration
- Integration boundaries for external plugins

## Build

The project uses Maven:

```bash
mvn clean package
```

The current execution environment for this session does not include `mvn`, so build verification must be performed once Maven is available locally or in CI.

