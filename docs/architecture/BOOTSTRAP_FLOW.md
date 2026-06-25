# Bootstrap Flow

1. Paper loads `AscensionPlugin`.
2. `AscensionBootstrap` wires platform services.
3. The service container receives core runtime singletons.
4. `ModuleManager` resolves startup order from declared module dependencies.
5. `AscensionApplication` starts modules in dependency order.
6. Core infrastructure creates the data folder, loads base configs, and discovers optional integrations.
7. The runtime engine starts the internal event bus, runtime task service, and centralized game loop.
8. The registry module initializes foundational registries.
9. Database and profile modules start persistence services and profile infrastructure.
10. The session module registers Bukkit join and quit listeners, exposes the session manager, and builds the shared `GameContext`.
11. `ServerReadyEvent` is published after startup completes.
