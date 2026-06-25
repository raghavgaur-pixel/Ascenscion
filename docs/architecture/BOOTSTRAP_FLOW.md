# Bootstrap Flow

1. Paper loads `AscensionPlugin`.
2. `AscensionBootstrap` wires platform services.
3. The service container receives core runtime singletons.
4. `ModuleManager` resolves startup order from declared module dependencies.
5. `AscensionApplication` starts modules in dependency order.
6. Core infrastructure creates the data folder, loads base configs, and discovers optional integrations.
7. Future modules will register their own services during startup through the shared registry.

