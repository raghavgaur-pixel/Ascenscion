# Ascension Core Gameplay Design

## Vision

Ascension is a classless Minecraft MMORPG built around persistent progression through a multi-floor tower. Minecraft supplies the platform, world, and rendering layer; Ascension owns the gameplay model, progression, combat, content, and persistence contracts.

## Character Identity

A character is defined by choices rather than a permanent class. Weapon selection, equipment, abilities, stat investment, professions, and progression choices form the player's build. The engine must avoid assumptions that a player belongs to a single archetype.

## Progression

Ascension uses two complementary progression axes:

1. **Character progression** — level and experience improve the character's combat and utility potential.
2. **Tower progression** — defeating major floor objectives unlocks access to higher floors and their content.

The systems are intentionally independent: character level should not be treated as a substitute for floor access, and floor access should not hard-code a player's build.

## Tower Model

Each floor is a persistent MMO region, not merely an instance. A floor may contain towns, wilderness, NPCs, quests, dungeons, resources, secrets, dynamic events, and a major floor progression path culminating in a floor boss. Players may remain on a completed floor to explore, socialize, farm, craft, or pursue optional content.

A floor boss unlocks progression to the next configured floor. The engine therefore models floor access as persistent progression while allowing individual activities inside a floor to be instanced later where appropriate.

## Abilities

Abilities use a hybrid data-driven architecture:

- Definitions are immutable assets and may be reloaded safely.
- Costs use resource identifiers so new resources do not require API changes.
- Targeting is a stable vocabulary separate from concrete platform resolution.
- Cooldowns are runtime state and are never persisted with content definitions.
- Concrete behavior is registered through an executor boundary.
- The application service owns validation, cooldowns, resource accounting, rollback, and orchestration.

This allows built-in abilities and extension-provided abilities to coexist without creating a Java class for every piece of content.

## Death Philosophy

Death is high-stakes but never permanent. The final penalty model is intentionally an engine-level policy so a server owner can configure consequences without character deletion. Potential penalties can include durability loss, temporary recovery effects, resource loss, or activity-specific failure states.

## Design Guardrails

- No class-locking of equipment or abilities unless a content definition explicitly requests a restriction.
- No hard-coded gameplay IDs inside orchestration code.
- No persistent storage of transient cooldown state.
- No irreversible resource consumption until an ability execution is committed.
- No direct dependency from core gameplay contracts onto Bukkit/NMS types.
- Content reloads must not corrupt live player runtime state.
