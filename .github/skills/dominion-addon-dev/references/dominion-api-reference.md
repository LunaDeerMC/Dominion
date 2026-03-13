# DominionAPI Complete Reference

> DominionAPI version: 4.7.3
> JavaDoc: https://lunadeermc.github.io/DominionAPI/
> Documentation: https://github.com/LunaDeerMC/DominionDocs

## Package Overview

| Package | Description |
|---------|-------------|
| `cn.lunadeer.dominion.api` | Core API interface (`DominionAPI`) |
| `cn.lunadeer.dominion.api.dtos` | Data Transfer Objects (`DominionDTO`, `PlayerDTO`, `MemberDTO`, `GroupDTO`, `CuboidDTO`) |
| `cn.lunadeer.dominion.api.dtos.flag` | Flag system (`Flag`, `EnvFlag`, `PriFlag`, `Flags`) |
| `cn.lunadeer.dominion.events` | Player movement events and flag registration |
| `cn.lunadeer.dominion.events.dominion` | Dominion lifecycle events (create, delete) |
| `cn.lunadeer.dominion.events.dominion.modify` | Dominion modification events (rename, resize, flags, messages, TP, transfer, map color) |
| `cn.lunadeer.dominion.events.group` | Group events (create, delete, rename, flags, member add/remove) |
| `cn.lunadeer.dominion.events.member` | Member events (add, remove, flag set) |
| `cn.lunadeer.dominion.providers` | Async data operation providers (`DominionProvider`, `GroupProvider`, `MemberProvider`) |
| `cn.lunadeer.dominion.utils` | Utility classes (color code parsing) |

---

## DominionAPI (Singleton Entry Point)

**Package**: `cn.lunadeer.dominion.api`

```java
DominionAPI api = DominionAPI.getInstance();
```

### Player Methods

| Method | Return | Description |
|--------|--------|-------------|
| `getPlayer(String name)` | `@Nullable PlayerDTO` | Get player by name |
| `getPlayer(UUID player)` | `@Nullable PlayerDTO` | Get player by UUID |
| `getPlayerName(UUID uuid)` | `@NotNull String` | Get player name by UUID |

### Dominion Query Methods

| Method | Return | Description |
|--------|--------|-------------|
| `getAllDominions()` | `List<DominionDTO>` | All dominions (including multi-server) |
| `getAllDominionsOfPlayer(UUID player)` | `List<DominionDTO>` | All dominions owned by player |
| `getChildrenDominionOf(DominionDTO parent)` | `List<DominionDTO>` | Child dominions of parent |
| `getDominion(Integer id)` | `@Nullable DominionDTO` | Get dominion by ID |
| `getDominion(String name)` | `@Nullable DominionDTO` | Get dominion by name |
| `getDominion(Location location)` | `@Nullable DominionDTO` | Get dominion at location |
| `getPlayerOwnDominionDTOs(UUID player)` | `List<DominionDTO>` | Dominions owned by player |
| `getPlayerAdminDominionDTOs(UUID player)` | `List<DominionDTO>` | Dominions where player is admin |
| `getPlayerCurrentDominion(Player player)` | `@Nullable DominionDTO` | Player's current dominion |
| `resetPlayerCurrentDominionId(Player player)` | `void` | Reset cached dominion ID for player |

### Member and Group Methods

| Method | Return | Description |
|--------|--------|-------------|
| `getMember(DominionDTO dominion, Player player)` | `@Nullable MemberDTO` | Get member by Player |
| `getMember(DominionDTO dominion, UUID player)` | `@Nullable MemberDTO` | Get member by UUID |
| `getGroup(MemberDTO member)` | `@Nullable GroupDTO` | Get group of a member |
| `getGroup(Integer id)` | `@Nullable GroupDTO` | Get group by ID |

### Flag Check Methods

| Method | Return | Description |
|--------|--------|-------------|
| `checkPrivilegeFlag(Location, PriFlag, Player)` | `boolean` | Check privilege flag (with notification) — **preferred since 4.5.0** |
| `checkPrivilegeFlag(DominionDTO, PriFlag, Player)` | `boolean` | Check privilege flag (deprecated — no world-wide check) |
| `checkPrivilegeFlagSilence(Location, PriFlag, Player)` | `boolean` | Check privilege flag silently — **preferred since 4.5.0** |
| `checkPrivilegeFlagSilence(DominionDTO, PriFlag, Player)` | `boolean` | Check privilege flag silently (deprecated) |
| `checkEnvironmentFlag(Location, EnvFlag)` | `boolean` | Check environment flag — **preferred since 4.5.0** |
| `checkEnvironmentFlag(DominionDTO, EnvFlag)` | `boolean` | Check environment flag (deprecated) |

### Provider Accessors

| Method | Return | Description |
|--------|--------|-------------|
| `getDominionProvider()` | `DominionProvider` | Access dominion operations |
| `getGroupProvider()` | `GroupProvider` | Access group operations |
| `getMemberProvider()` | `MemberProvider` | Access member operations |

### Statistics and Cache

| Method | Return | Description |
|--------|--------|-------------|
| `dominionCount()` | `Integer` | Total dominion count |
| `groupCount()` | `Integer` | Total group count |
| `memberCount()` | `Integer` | Total member count |
| `reloadCache()` | `void` | Reload all cached data |
| `reloadConfig()` | `void` | Reload configuration |

---

## Data Transfer Objects (DTOs)

### DominionDTO (Interface)

Represents a dominion (land claim region).

| Method | Return | Description |
|--------|--------|-------------|
| `getId()` | `@NotNull Integer` | Dominion ID |
| `getOwner()` | `@NotNull UUID` | Owner UUID |
| `getOwnerDTO()` | `@NotNull PlayerDTO` | Owner DTO |
| `setOwner(UUID)` | `DominionDTO` | Set owner (throws `SQLException`) |
| `getName()` | `@NotNull String` | Dominion name |
| `setName(String)` | `DominionDTO` | Set name (throws `SQLException`) |
| `getWorld()` | `@Nullable World` | World (null if not loaded) |
| `getWorldUid()` | `@NotNull UUID` | World UUID (always non-null) |
| `getCuboid()` | `@NotNull CuboidDTO` | Boundaries |
| `setCuboid(CuboidDTO)` | `DominionDTO` | Set boundaries (throws `SQLException`) |
| `getParentDomId()` | `@NotNull Integer` | Parent dominion ID (-1 if none) |
| `getJoinMessage()` | `@NotNull String` | Welcome message |
| `setJoinMessage(String)` | `DominionDTO` | Set welcome message (throws `SQLException`) |
| `getLeaveMessage()` | `@NotNull String` | Leave message |
| `setLeaveMessage(String)` | `DominionDTO` | Set leave message (throws `SQLException`) |
| `getEnvFlagValue(EnvFlag)` | `boolean` | Get environment flag value |
| `getGuestFlagValue(PriFlag)` | `boolean` | Get guest privilege flag value |
| `getEnvironmentFlagValue()` | `Map<EnvFlag, Boolean>` | All environment flags |
| `getGuestPrivilegeFlagValue()` | `Map<PriFlag, Boolean>` | All guest privilege flags |
| `setEnvFlagValue(EnvFlag, boolean)` | `DominionDTO` | Set env flag (throws `SQLException`) |
| `setGuestFlagValue(PriFlag, boolean)` | `DominionDTO` | Set guest flag (throws `SQLException`) |
| `getTpLocation()` | `@NotNull Location` | Teleport location (or center) |
| `setTpLocation(Location)` | `@NotNull DominionDTO` | Set TP location (throws `SQLException`) |
| `getColorR()`, `getColorG()`, `getColorB()` | `int` | Color components |
| `getColor()` | `@NotNull String` | Color as string |
| `getColorHex()` | `int` | Hex color value |
| `setColor(Color)` | `@NotNull DominionDTO` | Set color (throws `SQLException`) |
| `getGroups()` | `List<GroupDTO>` | All groups |
| `getMembers()` | `List<MemberDTO>` | All members |
| `getServerId()` | `Integer` | Server ID |

### PlayerDTO (Interface)

| Method | Return | Description |
|--------|--------|-------------|
| `getId()` | `Integer` | Player ID |
| `getUuid()` | `UUID` | Player UUID |
| `getLastKnownName()` | `String` | Last known name |
| `updateLastKnownName(String, URL)` | `PlayerDTO` | Update name (throws `SQLException`, `MalformedURLException`) |
| `getUsingGroupTitleID()` | `Integer` | Active group title ID |
| `getSkinUrl()` | `URL` | Skin URL (throws `MalformedURLException`) |
| `getUiPreference()` | `PlayerDTO.UI_TYPE` | UI preference |
| `setUiPreference(UI_TYPE)` | `void` | Set UI preference (throws `SQLException`) |

### MemberDTO (Interface)

| Method | Return | Description |
|--------|--------|-------------|
| `getId()` | `Integer` | Member ID |
| `getPlayerUUID()` | `UUID` | Player UUID |
| `getDomID()` | `Integer` | Dominion ID |
| `getGroupId()` | `Integer` | Group ID (-1 if no group) |
| `getFlagValue(PriFlag)` | `Boolean` | Get flag value |
| `getFlagsValue()` | `Map<PriFlag, Boolean>` | All flag values |
| `setFlagValue(PriFlag, boolean)` | `MemberDTO` | Set flag (throws `SQLException`) |
| `getPlayer()` | `@NotNull PlayerDTO` | Player DTO |

### GroupDTO (Interface)

| Method | Return | Description |
|--------|--------|-------------|
| `getId()` | `@NotNull Integer` | Group ID |
| `getDomID()` | `@NotNull Integer` | Dominion ID |
| `getNamePlain()` | `@NotNull String` | Plain name (no color codes) |
| `getNameRaw()` | `@NotNull String` | Raw name (with color codes) |
| `getNameColoredComponent()` | `@NotNull Component` | Name as Adventure Component |
| `getNameColoredBukkit()` | `@NotNull String` | Name with Bukkit color codes |
| `setName(String)` | `GroupDTO` | Set name (throws `SQLException`) |
| `getFlagValue(PriFlag)` | `@NotNull Boolean` | Get flag value |
| `getFlagsValue()` | `Map<PriFlag, Boolean>` | All flag values |
| `setFlagValue(PriFlag, boolean)` | `GroupDTO` | Set flag (throws `SQLException`) |
| `getMembers()` | `List<MemberDTO>` | Group members (throws `SQLException`) |

### CuboidDTO (Class)

Represents a 3D rectangular region.

**Constructors**:
- `CuboidDTO(int[] pos1, int[] pos2)`
- `CuboidDTO(CuboidDTO cuboid)` — copy constructor
- `CuboidDTO(int x1, int y1, int z1, int x2, int y2, int z2)`
- `CuboidDTO(Location loc1, Location loc2)`

| Method | Return | Description |
|--------|--------|-------------|
| `getPos1()` / `setPos1(int[])` | `int[]` / `void` | First position |
| `getPos2()` / `setPos2(int[])` | `int[]` / `void` | Second position |
| `getLoc1(World)` / `getLoc2(World)` | `Location` | Positions as Location |
| `x1()`, `y1()`, `z1()`, `x2()`, `y2()`, `z2()` | `int` | Individual coordinates |
| `xLength()`, `yLength()`, `zLength()` | `long` | Axis lengths |
| `getSquare()` | `long` | Base area (X × Z) |
| `getVolume()` | `long` | Volume (X × Y × Z) |
| `contain(CuboidDTO)` | `boolean` | Contains another cuboid |
| `contain(CuboidDTO, boolean ignoreY)` | `boolean` | Containment (optionally ignoring Y) |
| `contain(int x, int y, int z)` | `boolean` | Contains coordinates |
| `containedBy(CuboidDTO)` | `boolean` | Is contained by another cuboid |
| `intersectWith(CuboidDTO)` | `boolean` | Intersects with another cuboid |
| `minusSquareWith(CuboidDTO)` | `long` | Area difference |
| `minusVolumeWith(CuboidDTO)` | `long` | Volume difference |
| `addUp(int)`, `addDown(int)`, `addNorth(int)`, `addSouth(int)`, `addEast(int)`, `addWest(int)` | `void` | Expand in direction |

---

## Flag System

### Flag (Abstract Base Class)

**Package**: `cn.lunadeer.dominion.api.dtos.flag`

```java
new Flag(String flag_name, String display_name, String description, Boolean default_value, Boolean enable, Material material)
```

| Method | Return | Description |
|--------|--------|-------------|
| `getFlagName()` | `@NotNull String` | Internal flag name |
| `getDisplayName()` | `@NotNull String` | Display name (translatable) |
| `getDescription()` | `@NotNull String` | Description (translatable) |
| `getDefaultValue()` | `@NotNull Boolean` | Default value |
| `getEnable()` | `@NotNull Boolean` | Whether flag is enabled |
| `getMaterial()` | `@NotNull Material` | CUI icon material |
| `setDisplayName(String)`, `setDescription(String)`, `setDefaultValue(Boolean)`, `setEnable(Boolean)`, `setMaterial(String)` | `void` | Setters |

### EnvFlag (Environment Flag)

Represents dominion-level environment settings (not player-specific).

```java
new EnvFlag(String flag_name, String display_name, String description, Boolean default_value, Boolean enable, Material material)
```

### PriFlag (Privilege Flag)

Represents player-specific permission settings.

```java
new PriFlag(String flag_name, String display_name, String description, Boolean default_value, Boolean enable, Material material)
```

### Registering Custom Flags

```java
import cn.lunadeer.dominion.api.dtos.flag.Flags;

// Register flags
Flags.registerEnvFlag(myEnvFlag);
Flags.registerPriFlag(myPriFlag);

// Apply all new custom flags — MUST call after all registrations
Flags.applyNewCustomFlags();
```

---

## Providers (Async Data Operations)

All Provider methods return `CompletableFuture`. A `null` result from `.get()` means the operation failed.

### DominionProvider

**Package**: `cn.lunadeer.dominion.providers`

Accessible via `DominionAPI.getInstance().getDominionProvider()` or `DominionProvider.getInstance()`.

| Method | Parameters | Return | Description |
|--------|-----------|--------|-------------|
| `createDominion` | `CommandSender operator, String name, UUID owner, World world, CuboidDTO cuboid, @Nullable DominionDTO parent, boolean skipEconomy` | `CompletableFuture<DominionDTO>` | Create dominion |
| `deleteDominion` | `CommandSender operator, DominionDTO dominion, boolean skipEconomy, boolean force` | `CompletableFuture<DominionDTO>` | Delete dominion |
| `deleteDominion` | `CommandSender operator, DominionDTO dominion, boolean skipEconomy` | `CompletableFuture<DominionDTO>` | Delete dominion (force=true) |
| `renameDominion` | `CommandSender operator, DominionDTO dominion, String newName` | `CompletableFuture<DominionDTO>` | Rename dominion |
| `resizeDominion` | `CommandSender operator, DominionDTO dominion, type, direction, int size` | `CompletableFuture<DominionDTO>` | Resize dominion |
| `transferDominion` | `CommandSender operator, DominionDTO dominion, PlayerDTO newOwner, boolean force` | `CompletableFuture<DominionDTO>` | Transfer ownership |
| `transferDominion` | `CommandSender operator, DominionDTO dominion, PlayerDTO newOwner` | `CompletableFuture<DominionDTO>` | Transfer (force=true) |
| `setDominionTpLocation` | `CommandSender operator, DominionDTO dominion, Location newTpLocation` | `CompletableFuture<DominionDTO>` | Set TP location |
| `setDominionMessage` | `CommandSender operator, DominionDTO dominion, type, String message` | `CompletableFuture<DominionDTO>` | Set message |
| `setDominionEnvFlag` | `CommandSender operator, DominionDTO dominion, EnvFlag flag, boolean newValue` | `CompletableFuture<DominionDTO>` | Set env flag |
| `setDominionGuestFlag` | `CommandSender operator, DominionDTO dominion, PriFlag flag, boolean newValue` | `CompletableFuture<DominionDTO>` | Set guest flag |
| `setDominionMapColor` | `CommandSender operator, DominionDTO dominion, Color newColor` | `CompletableFuture<DominionDTO>` | Set map color |

### GroupProvider

Accessible via `DominionAPI.getInstance().getGroupProvider()` or `GroupProvider.getInstance()`.

| Method | Parameters | Return | Description |
|--------|-----------|--------|-------------|
| `createGroup` | `CommandSender, DominionDTO, String groupName` | `CompletableFuture<GroupDTO>` | Create group |
| `deleteGroup` | `CommandSender, DominionDTO, GroupDTO` | `CompletableFuture<GroupDTO>` | Delete group |
| `renameGroup` | `CommandSender, DominionDTO, GroupDTO, String newName` | `CompletableFuture<GroupDTO>` | Rename group |
| `setGroupFlag` | `CommandSender, DominionDTO, GroupDTO, PriFlag, boolean` | `CompletableFuture<GroupDTO>` | Set group flag |
| `addMember` | `CommandSender, DominionDTO, GroupDTO, MemberDTO` | `CompletableFuture<MemberDTO>` | Add member to group |
| `removeMember` | `CommandSender, DominionDTO, GroupDTO, MemberDTO` | `CompletableFuture<MemberDTO>` | Remove member from group |

### MemberProvider

Accessible via `DominionAPI.getInstance().getMemberProvider()` or `MemberProvider.getInstance()`.

| Method | Parameters | Return | Description |
|--------|-----------|--------|-------------|
| `addMember` | `CommandSender, DominionDTO, PlayerDTO` | `CompletableFuture<MemberDTO>` | Add member to dominion |
| `removeMember` | `CommandSender, DominionDTO, MemberDTO` | `CompletableFuture<MemberDTO>` | Remove member from dominion |
| `setMemberFlag` | `CommandSender, DominionDTO, MemberDTO, PriFlag, boolean` | `CompletableFuture<MemberDTO>` | Set member flag |

---

## Events

### Player Movement Events

**Package**: `cn.lunadeer.dominion.events`

#### PlayerMoveInDominionEvent

Triggered when a player enters a dominion.

```java
@EventHandler
public void onEnter(PlayerMoveInDominionEvent event) {
    Player player = event.getPlayer();      // @NotNull
    DominionDTO dom = event.getDominion();  // @NotNull
}
```

#### PlayerMoveOutDominionEvent

Triggered when a player leaves a dominion.

```java
@EventHandler
public void onLeave(PlayerMoveOutDominionEvent event) {
    Player player = event.getPlayer();      // @NotNull
    DominionDTO dom = event.getDominion();  // @Nullable (null if dominion deleted)
}
```

#### PlayerCrossDominionBorderEvent

Triggered when a player crosses any dominion border (entering, leaving, or moving between dominions).

```java
@EventHandler
public void onCross(PlayerCrossDominionBorderEvent event) {
    Player player = event.getPlayer();  // @NotNull
    DominionDTO from = event.getFrom(); // @Nullable (null = entering)
    DominionDTO to = event.getTo();     // @Nullable (null = leaving)
}
```

Note: `from` and `to` are never both null simultaneously, and never equal to each other.

### Dominion Lifecycle Events

**Package**: `cn.lunadeer.dominion.events.dominion`

- `DominionCreateEvent` — Cancellable, triggered when creating a dominion
- `DominionDeleteEvent` — Cancellable, triggered when deleting a dominion

### Dominion Modification Events

**Package**: `cn.lunadeer.dominion.events.dominion.modify`

All extend `ResultEvent` (cancellable, has `CommandSender`).

- `DominionRenameEvent` — Dominion renamed
- `DominionSizeChangeEvent` — Dominion resized (has `SizeChangeType` and `SizeChangeDirection` enums)
- `DominionSetEnvFlagEvent` — Environment flag changed
- `DominionSetGuestFlagEvent` — Guest privilege flag changed
- `DominionSetMapColorEvent` — Map color changed
- `DominionSetMessageEvent` — Message changed (has `MessageType` enum)
- `DominionSetTpLocationEvent` — Teleport location changed
- `DominionTransferEvent` — Ownership transferred

### Group Events

**Package**: `cn.lunadeer.dominion.events.group`

- `GroupCreateEvent` — Group created
- `GroupDeleteEvent` — Group deleted
- `GroupRenamedEvent` — Group renamed
- `GroupSetFlagEvent` — Group flag changed
- `GroupAddMemberEvent` — Member added to group
- `GroupRemoveMemberEvent` — Member removed from group

### Member Events

**Package**: `cn.lunadeer.dominion.events.member`

- `MemberAddedEvent` — Member added to dominion (has `afterAdded(Consumer<MemberDTO>)` callback)
- `MemberRemovedEvent` — Member removed from dominion
- `MemberSetFlagEvent` — Member flag changed

### FlagRegisterEvent

**Package**: `cn.lunadeer.dominion.events`

Triggered when a flag is registered. Cancellable.

```java
@EventHandler
public void onFlagRegister(FlagRegisterEvent event) {
    JavaPlugin plugin = event.getPlugin();
    Flag flag = event.getFlag();
    // event.setCancelled(true); // to prevent registration
}
```

---

## Build Configuration Example (Complete)

### build.gradle.kts (Multi-Module Root)

```kotlin
plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

allprojects {
    apply(plugin = "java")
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":core"))
}
```

### core/build.gradle.kts

```kotlin
plugins {
    id("java")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("cn.lunadeer:DominionAPI:4.7.3")
}
```

### plugin.yml

```yaml
name: YourPlugin
version: '@version@'
main: com.example.yourplugin.YourPlugin
api-version: '1.20'
folia-supported: true
depend:
  - Dominion
```
