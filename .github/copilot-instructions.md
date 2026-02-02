# Copilot Instructions for Dominion

## Architecture Overview

Dominion is a Minecraft territory/anti-grief plugin (1.20.1+) with a multi-module Gradle structure:

| Module | Purpose |
|--------|---------|
| `api/` | Public API for addon developers - DTOs, events, provider interfaces |
| `core/` | Main plugin - commands, cache, events, UI, database operations |
| `v1_20_1/`, `v1_21/`, `v1_21_9/` | Version-specific event handlers for Minecraft API changes |
| `languages/` | YAML translations (`zh_cn.yml` is the reference) |
| `docs/` | VuePress documentation site |

### Data Flow Pattern
1. **DTOs** (`api/dtos/`) define data contracts (e.g., `DominionDTO`, `MemberDTO`, `GroupDTO`)
2. **DOOs** (`core/doos/`) are database objects implementing DTOs with persistence logic
3. **CacheManager** (`core/cache/`) maintains in-memory state synchronized across multi-server setups
4. **Handlers** (`core/handler/`) respond to cache and provider events

### Version Compatibility System
Event handlers in `v1_20_1/`, `v1_21/`, `v1_21_9/` are auto-registered via reflection. Use annotations:
- `@SpigotOnly` / `@PaperOnly` - server implementation filtering
- `@LowestVersion` / `@HighestVersion` - Minecraft version gating

## Build Commands

```bash
./gradlew shadowJar                    # Build lite JAR (dependencies external)
./gradlew shadowJar -PBuildFull=true   # Build full JAR (dependencies bundled)
cd api && ./gradlew clean build        # Build API module only
cd docs && npm run docs:dev            # Run docs locally
```

**Output:** `build/libs/Dominion-{version}-{lite|full}.jar`

**Note:** No automated tests exist. Testing is manual on Minecraft servers.

## Key Conventions

### Adding a New Flag
1. Define in `api/dtos/flag/` (extend `EnvFlag` or `PriFlag`)
2. Register in `Flags.java` static initializer
3. Add to `DominionDOO.java` field mapping and `fields()` method
4. Add language keys in `languages/*.yml`

### Adding Event Handlers
Place in version-specific modules under `events/player/` or `events/environment/`:
```java
// v1_21/src/main/java/cn/lunadeer/dominion/v1_21/events/player/MyHandler.java
@PaperOnly  // Optional: restrict to Paper servers
public class MyHandler implements Listener { ... }
```

### Translation Files Structure
```yaml
# languages/en_us.yml - keys use kebab-case
dominion-text:
  loading-config: Loading Configurations...
  plugin-version: 'Plugin Version: {0}'  # Use {0}, {1} for placeholders
```
Separate files exist for CUI (`cui/`) and TUI (`tui/`) interfaces.

### Database Operations
Use the custom ORM in `core/utils/databse/`:
```java
// Example from DominionDOO.java
Select.from("dominion").where("id", id).execute(Dominion.database);
Update.table("dominion").set(fieldName, value).where("id", id).execute(Dominion.database);
```

## Integration Points

- **Vault/PlaceholderAPI/WorldGuard** - soft dependencies in `plugin.yml`
- **DominionAPI** - entry point for addons: `DominionAPI.getInstance()`
- **bStats** - metrics registered in `Dominion.java` onEnable

## Important Files

- `core/src/main/resources/plugin.yml` - plugin metadata, commands, permissions
- `build.gradle.kts` - version management, dependency toggling, Hangar publishing
- `version.properties` - auto-incremented version suffix