# Copilot Instructions for Dominion

## Architecture

Dominion is a Minecraft territory/anti-grief plugin (1.20.1+). Multi-module Gradle build, Java 17 toolchain (Java 21 for version modules).

| Module | Purpose |
|--------|---------|
| `api/` | Public API — DTOs (`DominionDTO`, `MemberDTO`, `GroupDTO`), events, flag definitions, provider interfaces. Published to Maven Central. |
| `core/` | Main plugin — commands, cache, DOOs (persistence), event registration, UIs (CUI/TUI), configuration, custom ORM. |
| `versions/v1_20_1/` thru `v1_21_9/` | Version-specific event handlers using NMS via paperweight. **5 modules**: `v1_20_1`, `v1_21`, `v1_21_4`, `v1_21_8`, `v1_21_9`. |
| `languages/` | YAML translations — `en_us.yml`, `zh_cn.yml`, plus `cui/` and `tui/` subdirs per locale. |
| `docs/` | VuePress documentation site. |

### Data Flow
1. **DTOs** (`api/dtos/`) — abstract data contracts
2. **DOOs** (`core/doos/`) — "Database Object Operations" implementing DTOs with persistence via custom ORM
3. **CacheManager** (`core/cache/`) — in-memory state, multi-server sync
4. **Handlers** (`core/handler/`) — provider pattern responding to cache events

### Key Terminology
- **DOO** (not DAO) — combines DTO implementation + persistence in one class
- **EnvFlag** — environment/territory-wide flags (e.g., `ANIMAL_SPAWN`, `TNT_EXPLODE`)
- **PriFlag** — privilege/per-player flags (e.g., `ADMIN`, `ANIMAL_KILLING`)

## Build & Test

```bash
./gradlew shadowJar                    # Lite JAR (dependencies external)
./gradlew shadowJar -PBuildFull=true   # Full JAR (dependencies bundled)
cd api && ./gradlew clean build        # API module only
cd docs && npm run docs:dev            # Docs dev server
```

Output: `build/libs/Dominion-{version}-{lite|full}.jar`. **No automated tests** — testing is manual on Minecraft servers.

## Code Style & Conventions

### Package Structure
- `cn.lunadeer.dominion.api` — API module
- `cn.lunadeer.dominion` — core module (`.cache`, `.commands`, `.configuration`, `.doos`, `.events`, `.handler`, `.managers`, `.storage`, `.uis`, `.utils`)
- `cn.lunadeer.dominion.v1_21_9.events.player` — version-specific handlers

### Database Layer
- Use `core/src/main/java/cn/lunadeer/dominion/storage/` for persistence.
- `DatabaseManager` owns HikariCP, Flyway migrations, and the MyBatis `SqlSessionFactory`.
- Repositories under `storage/repository/` are the only place that should issue database CRUD queries.

### Text/Translation Pattern
Every class with user-facing text defines a static inner class extending `ConfigurationPart`. Java `camelCase` fields auto-convert to YAML `kebab-case`:
```java
public class DominionCreateCommand {
    public static class DominionCreateCommandText extends ConfigurationPart {
        public String createDescription = "Create a new dominion...";
    }
    // Reference: Language.dominionCreateCommandText.createDescription
}
```

### Configuration System
Reflection-based YAML: `ConfigurationFile` / `ConfigurationPart` base classes. Annotations: `@Comments`, `@Headers`, `@HandleManually`, `@PostProcess`, `@PreProcess`. See `core/src/.../configuration/Configuration.java`.

### Static Singletons
Uses `CacheManager.instance`, `Dominion.instance` — no dependency injection.

## Version Compatibility System

Event handlers in `versions/v1_20_1/` through `v1_21_9/` are auto-registered via reflection in `EventsRegister.java`:
- Scans ALL version packages, filters with annotations
- `@SpigotOnly` / `@PaperOnly` — server implementation filtering
- `@LowestVersion(v1_21)` / `@HighestVersion(v1_20_1)` — MC version gating
- `ImplementationVersion` enum ordered high→low: `v1_21_9, v1_21_8, v1_21_4, v1_21, v1_20_1`
- All event handlers use `EventPriority.LOWEST` (intercept first)

### Adding Event Handlers
Place in version module under `events/player/` or `events/environment/`:
```java
// versions/v1_21_9/src/main/java/cn/lunadeer/dominion/v1_21_9/events/player/MyHandler.java
@LowestVersion(XVersionManager.ImplementationVersion.v1_21_9)
@PaperOnly  // Optional
public class MyHandler implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void handler(SomeEvent event) {
        if (event.isCancelled()) return;
        checkPrivilegeFlag(event.getEntity().getLocation(), Flags.MY_FLAG, player, event);
    }
}
```

Key helpers from `misc/Others`: `checkPrivilegeFlag()`, `checkEnvironmentFlag()`, `checkPrivilegeFlagSilence()`, `bypassLimit()`.

## Adding a New Flag

1. Define in `api/dtos/flag/Flags.java`: `public static final EnvFlag MY_FLAG = new EnvFlag("my_flag", "Display", "desc", defaultVal, enabled, Material.ICON);`
2. Add language keys in all `languages/*.yml` files
3. Create event handler(s) in appropriate version module(s)
4. `FlagReconciler` adds missing runtime flag columns to the relevant entity tables during migration.

## Database ORM (`core/storage/`)

Use MyBatis mapper interfaces and SQL providers with schema constants from `DatabaseSchema`. Dynamic table and column names must be validated with `DatabaseSchema` helpers before they are interpolated into SQL providers.
Flyway Java migrations live under `storage/migration/` and must remain Java migrations, not SQL migration files.

Flag values are stored as boolean columns on the owning tables and reconciled after Flyway migration:
- `dominion` — environment flags and guest privilege flags
- `dominion_member` — member privilege flags
- `dominion_group` — group privilege flags
- `privilege_template` — template privilege flags

## Security

Two permission levels: `dominion.default` (all players) and `dominion.admin` (OP only).
Bypass check: `bypassLimit(player)` returns true for OP or `dominion.admin`.
Flag cascade: disabled flag → allow → no dominion check → WorldWide settings → admin bypass → member flags → group flags → guest defaults.

## Important Files

- `settings.gradle.kts` — module definitions (includes all 5 version modules)
- `core/src/main/resources/plugin.yml` — plugin metadata, commands, permissions
- `build.gradle.kts` — version management, ShadowJar, Hangar publishing
- `version.properties` — auto-incremented version suffix
- `api/src/main/java/cn/lunadeer/dominion/api/dtos/flag/Flags.java` — all flag definitions
- `core/src/main/java/cn/lunadeer/dominion/events/EventsRegister.java` — reflection-based handler registration
