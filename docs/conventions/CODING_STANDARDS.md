# Coding Standards

## Object Design

- Constructor injection by default
- Favor immutable state where possible
- Keep classes focused on one responsibility
- Avoid inheritance unless it reduces real duplication cleanly

## Threading

- Bukkit entity, world, and inventory interaction is main-thread only unless explicitly documented otherwise
- Blocking I/O is forbidden on the main thread
- Async work must return to the scheduler abstraction before touching game state

## Configuration

- Gameplay values belong in config or data definitions
- Config identifiers must be stable and human-readable
- Modules should validate config at load time and fail fast on invalid state

## Persistence

- Repositories define storage contracts
- Database implementations do not leak SQL concerns into gameplay services
- Caching rules must be explicit about ownership and invalidation

## API Surface

- Package-private visibility is preferred unless cross-package access is required
- Public interfaces should exist at real subsystem boundaries, not as ceremony

