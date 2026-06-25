# Development

## Standards

- Java 21 language level
- Constructor injection by default
- No static managers
- No hardcoded gameplay constants
- Prefer interfaces at subsystem boundaries
- Threading behavior must be explicit

## Coding Conventions

- Package names are lower-case and grouped by subsystem.
- Public APIs must have clear names and narrow responsibilities.
- Services and repositories should be expressed as interfaces when replacement is plausible.
- Exceptions during bootstrap should fail fast and disable the plugin cleanly.
- Logging must carry subsystem context where relevant.

## Workflow

1. Inspect existing code before adding new classes.
2. Extend the current subsystem without bypassing architecture.
3. Update `ARCHITECTURE.md`, `TODO.md`, `PROJECT_PROGRESS.md`, and `CHANGELOG.md` whenever the repository changes materially.
4. Verify via build or focused tests whenever tooling is available.

## Definition Of Done Per Subsystem

- Architecture consistent with established boundaries
- Configuration externalized where appropriate
- Main-thread safety reviewed
- Async behavior documented
- Persistence boundaries defined when needed
- Documentation updated

