# Issue #290: automatic Nether portal creation

`nether_portal_create` is an environment flag controlling automatically generated
paired Nether portal exits (`PortalCreateEvent.CreateReason.NETHER_PAIR`). It is
enabled by default and its default value is `false`: a generated exit must not
change any block in a dominion that denies the flag. This includes the frame,
portal blocks, supporting platform and blocks cleared to air.

Enable automatic exits for a dominion with:

```text
/dominion set_env <dominion-name> nether_portal_create true
```

The flag applies to every trigger, including owners, administrators, non-player
entities and events without a triggering entity. Manual frame construction and
ignition retain their existing permissions. Existing portals and End platforms
are outside this flag's scope. Protection is based on every affected block's
destination location, in either direction between the Overworld and Nether.

## Upgrade behavior

- Existing dominions receive `false` when the database column is added. Later
  explicit values survive restarts and reconciliation.
- Missing definitions in `flags.yml` are added with `enable: true` and
  `default: false` under `environment.nether_portal_create`.
- Existing WorldWide configurations receive
  `environment.nether_portal_create: false`. This also protects unclaimed land
  when WorldWide is enabled. Unclaimed land without WorldWide remains allowed.
- New default groups include the flag under Natural Changes. Existing customized
  groups are preserved, so the new flag appears under Ungrouped until assigned.

## Automated checks

From the repository root:

```sh
./gradlew :core:test :versions:v1_20_1:test
./gradlew shadowJar
```

The listener tests use real `PortalCreateEvent` instances and the production
environment permission check, with mocked world/cache data. They do not exercise
the server's actual portal search or teleportation pipeline.

## Manual server checks

Use a disposable server and fresh test worlds. Ensure no existing destination
portal can be reused when checking automatic creation. Test both Overworld to
Nether and Nether to Overworld on supported Paper/Spigot and Folia versions.

1. Deny the flag in a destination dominion. Enter a source portal whose generated
   exit would occupy that dominion. Confirm that no portal, frame, platform or
   clearing operation modifies protected blocks.
2. Enable the flag and repeat; confirm that an exit is generated. Repeat with a
   guest who lacks `place` and with the owner to check environment semantics.
3. Put the prospective exit across a dominion boundary, including a sub-dominion
   with an opposing setting. Denying even one affected position must prevent the
   entire creation. Include a fallback portal whose air clearing or support
   platform, rather than its portal blocks, intersects the denied dominion.
4. Repeat on unclaimed land with WorldWide disabled, then enabled with the flag
   false and true. Only the enabled/false combination should deny creation.
5. Check manual ignition, travel through existing portals and End-platform
   creation. They must retain their existing behavior regardless of this flag.
6. On versions supporting non-player exit creation, send an entity through a
   source portal and repeat the allow/deny cases. On Folia, repeat with a player
   to exercise the creation event's null triggering entity.
7. Restart after explicitly enabling one dominion and confirm that its flag
   remains true while a default dominion remains false. Check the flag's name,
   description and obsidian icon in both chest and dialog UIs where supported.

Folia 1.21.11's portal pipeline can still teleport to the calculated destination
after creation is cancelled. This flag guarantees block protection; keeping the
entity in its original world or finding another safe exit is not part of it.
Record teleport behavior separately from the block-protection result.
