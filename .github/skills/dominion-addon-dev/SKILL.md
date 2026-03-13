---
name: dominion-addon-dev
description: >-
  Develop Dominion addon plugins using the DominionAPI. Use when creating new Dominion addon plugins,
  writing code that interacts with the Dominion land-claim system, listening to Dominion events,
  operating on dominion/group/member data via Providers, registering custom flags,
  or checking privilege/environment flags within dominions. Also use when setting up Gradle/Maven
  build files for DominionAPI dependency, creating plugin.yml for Dominion addons,
  or debugging Dominion API integration issues.
compatibility:
  - DominionProtect
---

# Dominion Addon Development Skill

You are an expert developer for **Dominion** addon plugins (Minecraft Paper/Spigot).
Dominion is a land-claim protection plugin; addons extend it using the **DominionAPI**.

## Key References

- **JavaDoc**: https://lunadeermc.github.io/DominionAPI/
- **Documentation**: https://github.com/LunaDeerMC/DominionDocs
- **API Reference**: Load `references/dominion-api-reference.md` for complete API details.

## Project Setup

### Gradle (Kotlin DSL) — Recommended

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("cn.lunadeer:DominionAPI:4.7.3")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    compileOnly 'cn.lunadeer:DominionAPI:4.7.3'
}
```

### Maven

```xml
<dependency>
    <groupId>cn.lunadeer</groupId>
    <artifactId>DominionAPI</artifactId>
    <version>4.7.3</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml Requirements

The addon's `plugin.yml` MUST include `Dominion` in the `depend` list:

```yaml
name: YourAddonPlugin
version: '1.0.0'
main: com.example.yourplugin.YourPlugin
api-version: '1.20'
depend:
  - Dominion
```

Set `folia-supported: true` if your addon supports Folia.

## Getting the API Instance

Always obtain `DominionAPI` in `onEnable()` after verifying the Dominion plugin is loaded:

```java
import cn.lunadeer.dominion.api.DominionAPI;

public final class YourPlugin extends JavaPlugin {
    private DominionAPI dominionAPI;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("Dominion")) {
            dominionAPI = DominionAPI.getInstance();
            getLogger().info("DominionAPI loaded successfully");
        } else {
            throw new IllegalStateException("Dominion plugin is not enabled.");
        }
        getServer().getPluginManager().registerEvents(this, this);
    }
}
```

## Core API Patterns

### 1. Query Dominion Data (Read-Only)

Use `DominionAPI` methods for all read operations — they read from cache and are safe on any thread.

```java
// Get dominion at a location
DominionDTO dom = dominionAPI.getDominion(player.getLocation());

// Get dominion by name or ID
DominionDTO dom = dominionAPI.getDominion("myDominion");
DominionDTO dom = dominionAPI.getDominion(42);

// Get player's current dominion
DominionDTO current = dominionAPI.getPlayerCurrentDominion(player);

// Get all dominions owned by a player
List<DominionDTO> owned = dominionAPI.getAllDominionsOfPlayer(player.getUniqueId());

// Get player info
PlayerDTO playerDTO = dominionAPI.getPlayer(player.getUniqueId());

// Get member in a dominion
MemberDTO member = dominionAPI.getMember(dom, player);

// Get group of a member
GroupDTO group = dominionAPI.getGroup(member);
```

### 2. Check Flags

```java
// Check privilege flag (with player notification on denial)
boolean canBuild = dominionAPI.checkPrivilegeFlag(location, priFlag, player);

// Check privilege flag silently (no notification)
boolean canBuild = dominionAPI.checkPrivilegeFlagSilence(location, priFlag, player);

// Check environment flag
boolean hasFire = dominionAPI.checkEnvironmentFlag(location, envFlag);
```

**Important (since 4.5.0)**: Always prefer the `Location`-based overloads over `DominionDTO`-based overloads. The `Location`-based methods also check world-wide flags.

### 3. Modify Data via Providers

All data modifications MUST go through Providers to ensure consistency and trigger events.
Provider methods are asynchronous and return `CompletableFuture`.

The `operator` parameter controls permission checks:
- Pass a `Player` to enforce permission checks
- Pass `Bukkit.getConsoleSender()` to bypass permission checks (console-level access)

```java
// DominionProvider — create, delete, rename, resize, transfer dominions
DominionProvider domProvider = dominionAPI.getDominionProvider();

CompletableFuture<DominionDTO> future = domProvider.createDominion(
    Bukkit.getConsoleSender(),  // operator
    "newDominion",              // name
    playerUUID,                 // owner
    world,                      // world
    new CuboidDTO(x1,y1,z1, x2,y2,z2), // boundaries
    null,                       // parent (null = top-level)
    false                       // skipEconomy
);
DominionDTO created = future.get(); // null means failure

// GroupProvider — create, delete, rename groups; manage group members and flags
GroupProvider groupProvider = dominionAPI.getGroupProvider();

// MemberProvider — add, remove members; set member flags
MemberProvider memberProvider = dominionAPI.getMemberProvider();
```

### 4. Listen to Dominion Events

Register listeners the standard Bukkit way. All events extend `CallableEvent`.

```java
import cn.lunadeer.dominion.events.dominion.DominionCreateEvent;
import cn.lunadeer.dominion.events.member.MemberAddedEvent;

public class MyListener implements Listener {

    @EventHandler
    public void onDominionCreate(DominionCreateEvent event) {
        if (event.isCancelled()) return;
        // React to dominion creation
    }

    @EventHandler
    public void onMemberAdded(MemberAddedEvent event) {
        if (event.isCancelled()) return;
        // Use callback to get the result after data is processed
        event.afterAdded(memberDTO -> {
            if (memberDTO == null) return; // addition failed
            DominionDTO dominion = event.getDominion();
            Player player = Bukkit.getPlayer(memberDTO.getPlayerUUID());
            if (player != null) {
                player.teleportAsync(dominion.getTpLocation());
            }
        });
    }
}
```

**Key event categories:**

| Package | Events |
|---------|--------|
| `cn.lunadeer.dominion.events` | `PlayerMoveInDominionEvent`, `PlayerMoveOutDominionEvent`, `PlayerCrossDominionBorderEvent`, `FlagRegisterEvent` |
| `cn.lunadeer.dominion.events.dominion` | `DominionCreateEvent`, `DominionDeleteEvent` |
| `cn.lunadeer.dominion.events.dominion.modify` | `DominionRenameEvent`, `DominionSizeChangeEvent`, `DominionSetEnvFlagEvent`, `DominionSetGuestFlagEvent`, `DominionSetMapColorEvent`, `DominionSetMessageEvent`, `DominionSetTpLocationEvent`, `DominionTransferEvent` |
| `cn.lunadeer.dominion.events.group` | `GroupCreateEvent`, `GroupDeleteEvent`, `GroupRenamedEvent`, `GroupSetFlagEvent`, `GroupAddMemberEvent`, `GroupRemoveMemberEvent` |
| `cn.lunadeer.dominion.events.member` | `MemberAddedEvent`, `MemberRemovedEvent`, `MemberSetFlagEvent` |

**Data operation events** (like `DominionCreateEvent`, `MemberAddedEvent`) process the actual data **after** all listeners run. Use provided callback methods (e.g., `event.afterAdded(...)`) to access results.

### 5. Register Custom Flags (since 4.7.0)

Two flag types exist:
- **`EnvFlag`** — environment flags (not player-specific, e.g., weather control)
- **`PriFlag`** — privilege flags (player-specific, e.g., can-build)

```java
import cn.lunadeer.dominion.api.dtos.flag.EnvFlag;
import cn.lunadeer.dominion.api.dtos.flag.PriFlag;
import cn.lunadeer.dominion.api.dtos.flag.Flags;

// Define custom flags
public static EnvFlag NO_RAIN = new EnvFlag(
    "no_rain",           // unique flag_name (no spaces)
    "No Rain",           // display_name
    "Prevents rain in this dominion.",  // description
    false,               // default_value
    true,                // enabled
    Material.SUNFLOWER   // CUI material icon
);

public static PriFlag CUSTOM_ACTION = new PriFlag(
    "custom_action",     // unique flag_name (no spaces)
    "Custom Action",     // display_name
    "Allows custom action in dominion.", // description
    false,               // default_value
    true,                // enabled
    Material.RED_BED     // CUI material icon
);

// Register and apply in onEnable()
Flags.registerEnvFlag(NO_RAIN);
Flags.registerPriFlag(CUSTOM_ACTION);
Flags.applyNewCustomFlags();  // Must call after registration
```

## DTO Quick Reference

| DTO | Key Methods |
|-----|------------|
| `DominionDTO` | `getId()`, `getName()`, `getOwner()`, `getWorld()`, `getCuboid()`, `getTpLocation()`, `getEnvFlagValue(flag)`, `getGuestFlagValue(flag)`, `getGroups()`, `getMembers()`, `getParentDomId()`, `getJoinMessage()`, `getLeaveMessage()` |
| `PlayerDTO` | `getId()`, `getUuid()`, `getLastKnownName()`, `getSkinUrl()`, `getUiPreference()` |
| `MemberDTO` | `getId()`, `getPlayerUUID()`, `getDomID()`, `getGroupId()`, `getFlagValue(flag)`, `getPlayer()` |
| `GroupDTO` | `getId()`, `getDomID()`, `getNamePlain()`, `getNameRaw()`, `getFlagValue(flag)`, `getMembers()` |
| `CuboidDTO` | `getPos1()`, `getPos2()`, `x1()..z2()`, `xLength()`, `yLength()`, `zLength()`, `getSquare()`, `getVolume()`, `contain(...)`, `intersectWith(...)` |

## Rules

1. DominionAPI is a `compileOnly` / `provided` dependency — never shade it into your jar.
2. Always verify `Dominion` is enabled before calling `DominionAPI.getInstance()`.
3. Use `Location`-based flag checks over `DominionDTO`-based ones (since 4.5.0) to include world-wide flags.
4. All Provider operations are async (`CompletableFuture`) — handle appropriately.
5. Event callbacks (e.g., `afterAdded`) run after actual data processing — use them for post-operation logic.
6. Custom flag names must be unique and contain no spaces.
7. Call `Flags.applyNewCustomFlags()` after registering all custom flags.
8. Never modify DTO data directly — always use Providers for writes.
9. Paper API 1.20.1+ is the minimum supported version.
10. The plugin must declare `depend: [Dominion]` in `plugin.yml`.
