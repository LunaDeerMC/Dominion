# Issue #298: splash and lingering potion protection

This opt-in harness runs on a **disposable Paper server**. It writes two persistent
dominions and a stone platform at x=94..146, y=99..108, z=3..23, teleports connected
players, and launches real potions. Never install it on a production server.
Use a fresh Dominion database for each run because the fixture coordinates are fixed.

## Run

1. Build from the repository root:
   `./gradlew -I tests/e2e/issue298/init.gradle :shadowJar -PBuildFull=true issue298HarnessJar`.
2. Install the Dominion full JAR and `build/issue298/Issue298.jar` in a disposable
   Paper 26.2 build 132 server running Java 25. Set `online-mode=false`,
   `server-ip=127.0.0.1`, `server-port=25598`, `spawn-protection=0`, `pvp=true`,
   `difficulty=normal`, and `allow-flight=true` in `server.properties`.
3. Connect the two protocol clients in separate terminals:

   ```sh
   python -u tests/e2e/issue298/client.py
   CLIENT_NAME=Issue298Target python -u tests/e2e/issue298/client.py
   ```

   The clients require only Python's standard library. `MC_PORT`, `MC_PROTOCOL`
   and `MC_TIMEOUT` override port 25598, protocol 776, and a 1200-second timeout.
   They are ordinary offline players; the harness explicitly removes OP status
   and checks that neither has the `dominion.admin` permission.
4. Run `issue298` from the server console. The complete matrix takes about five
   minutes at 20 TPS. Require `[Issue298] SUMMARY scenarios=74` with `failures=0`,
   no `[Issue298] FAIL` lines, and four `USE` entries in the visitor client
   log. Missing SUMMARY means the run did not finish. Both clients disconnect
   after receiving `E298_DONE`.

`issue298 baseline` runs a smaller 12-scenario matrix: denied and allowed harming
potions against cows, villagers and creepers, for both splash and lingering
potions. Run this with the original production listeners to demonstrate the
regression: the six denied scenarios should fail while the allowed controls pass.
The baseline mode still asserts correct behavior; expected failures must not be
reported as a successful fixed-build run.

## What is exercised

- Real database and cache entries, with persisted guest values for the existing
  `ANIMAL_KILLING`, `VILLAGER_KILLING`, `MONSTER_KILLING`, and `PVP` flags.
- Harming potions against cows, villagers and creepers with permissions denied
  and allowed. Creepers provide a non-undead monster susceptible to poison.
- Poison installation and subsequent server-driven damage for each mob category.
- Projectiles/cloud centers outside a protected dominion, targets across both
  directions of an adjacent denied/allowed boundary, and unclaimed targets.
- Two targets in the same impact/cloud with different permissions: the protected
  animal remains unharmed while the permitted one is damaged.
- Healing and regeneration remain available to protected animals. Healing damages
  zombies only when permitted; harming still heals zombies with killing denied.
- PVP denial/allowance and target-position boundary checks, including poison.
- Self-inflicted damage and a witch projectile source retain their previous behavior.
- Vanilla base potion items cover the normal gameplay path. Separate custom poison
  items exercise custom effects as well. Turtle Master base potions verify that
  the harmful slowness effect in a potion with multiple effects is recognized.
- Four actual client right clicks throw splash and lingering potions with each
  permission value, providing native network/item-use positive and negative controls.

The other scenarios call `LivingEntity.launchProjectile`, set its real potion item,
and let the server simulate its downward flight, collision, splash, cloud creation,
effect application and health changes. The harness never constructs or dispatches
synthetic potion, damage, or cloud events. LOWEST listeners record the engine's
candidate entities before protection; MONITOR listeners record accepted intensity
or cloud membership. Assertions require an actual launch, actual impact, a real
candidate target, the correct accepted applications, and the resulting health or
effect state. This prevents missed throws from masquerading as successful protection.

The Python client handles login/configuration, server teleports, keepalives and
actual item use. It does not render Minecraft. The test does not cover Folia
scheduling, interactions with other protection plugins, water bottles, or non-player entities
outside the existing Animals/Villager/Monster protection categories. The plugin is compiled
against the project's minimum API and selects newer potion entity/base-type APIs
when available; the recorded runtime results must identify the exact server used.

## Verified run (2026-10-17)

- Runtime: Paper `26.2-132-ver/26.2@19ebc4a`, Temurin Java 25+36, SQLite,
  two non-OP protocol clients, and a fresh Dominion database for each run.
- Baseline: restored the original `Piston` and `CloudEffect` listener classes from
  `7b851164eae060289e7908aaf4c86a99cc7601b9` in the otherwise identical full JAR.
  `SUMMARY scenarios=12 checks=140 failures=12`: all six denied scenarios
  incorrectly applied effects and damaged their targets; all allowed controls passed.
- Fixed build: `SUMMARY scenarios=74 checks=764 failures=0`, with no failed
  assertions or event-handler exceptions. All four client right clicks completed
  (`USE 36`, `USE 37`, `USE 73`, `USE 74`), and both clients received `E298_DONE`.
- Full build: `./gradlew :shadowJar -PBuildFull=true --console=plain` passed.
- Automated regression suite:
  `./gradlew -I tests/e2e/issue298/init.gradle issue298HarnessJar test -PBuildFull=true --console=plain`
  passed all 285 tests, including 82 potion regression cases, with no failures,
  errors, or skipped tests.
- Tested full JAR SHA-256:
  `ae8fda219c3ff46ae9831f2b641095b3d9e432bec3c75cab7b972141a1b35461`.
