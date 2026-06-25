# Development

## Standards

- Java 21 language level
- Constructor injection by default
- No static managers
- No hardcoded gameplay constants
- Prefer interfaces at subsystem boundaries
- Threading behavior must be explicit
- Public services should be documented with JavaDocs
- Persistence must remain behind repository and DAO boundaries

## Coding Conventions

- Package names are lower-case and grouped by subsystem.
- Public APIs must have clear names and narrow responsibilities.
- Services and repositories should be expressed as interfaces when replacement is plausible.
- Exceptions during bootstrap should fail fast and disable the plugin cleanly.
- Logging must carry subsystem context where relevant.
- Composition is preferred over inheritance.
- Future subsystems should register runtime definitions through the registry framework instead of introducing static globals.
- Future content definitions should load through `AssetService` rather than direct file parsing.
- Player-facing framework text should resolve through `LocalizationService`.
- Versioned structured configs should register through `TypedConfigurationService`.
- Reloadable definition sets should prefer `ReloadableRegistry` rather than ad-hoc mutable maps.
- SQL belongs in repositories or DAO support only.
- Persisted module-owned profile data should be modeled as profile components rather than fields added to `PlayerProfile`.
- Runtime-only player state belongs in `PlayerSession`, not `PlayerProfile`.
- Future recurring gameplay logic should prefer `GameLoop` tick registration over ad-hoc repeating tasks.
- Future cross-system runtime communication should prefer the internal event bus over direct module coupling where appropriate.
- Module-owned runtime tasks and listeners should use owner identifiers that match module ids.

## Workflow

1. Inspect existing code before adding new classes.
2. Extend the current subsystem without bypassing architecture.
3. Update `ARCHITECTURE.md`, `TODO.md`, `PROJECT_PROGRESS.md`, and `CHANGELOG.md` whenever the repository changes materially.
4. Verify via build or focused tests whenever tooling is available.

## Testing Expectations

- Public services should be constructor-injected and unit-test friendly.
- Thread-sensitive services should keep Bukkit dependencies at the edges.
- Repositories should be testable against integration fixtures or mocked database services.
- Runtime services should keep disposal paths explicit and testable.

## Definition Of Done Per Subsystem

- Architecture consistent with established boundaries
- Configuration externalized where appropriate
- Main-thread safety reviewed
- Async behavior documented
- Persistence boundaries defined when needed
- Documentation updated
- Registry interactions documented when new registries are introduced
- Runtime lifecycle and shutdown behavior documented when new engine services are introduced
- Asset ownership, validation, and reload behavior documented when new content groups are introduced
