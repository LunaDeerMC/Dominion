# Issue #288: firework entity damage

This opt-in plugin runs on a **disposable Paper server**. It creates three
persistent test dominions and a stone test platform, teleports a connected test
player, and launches real fireworks. Never install it on a production server.
Use a fresh database for every full run because fixtures occupy fixed coordinates.

## Run

1. Build Dominion and the harness from the repository root:
   `./gradlew -I tests/e2e/issue288/init.gradle :shadowJar -PBuildFull=true issue288HarnessJar`.
2. Install the Dominion full JAR and `build/issue288/Issue288.jar` in a disposable
   Paper 1.21.11 server's `plugins/`. Use `online-mode=false`,
   `server-ip=127.0.0.1`, `server-port=25588` and `spawn-protection=0`.
3. In a separate temporary directory run `npm install minecraft-protocol@1.66.2`.
   Copy `client.cjs` there and run `node client.cjs`. This joins as a non-op guest
   and fires a prepared crossbow only when the harness requests it.
4. After the client joins and server startup completes, run `issue288` from the
   server console. Require `[Issue288] SUMMARY` with `failures=0`, and no
   `[Issue288] FAIL` lines. Missing SUMMARY means the run did not finish.
5. Restart the server and run `issue288 verify` from the console. Require
   `RESTART SUMMARY checks=6 failures=0`.

## Coverage

- New dominions default to denying firework damage; opt-in values persist in
  SQLite and the cache across a restart.
- Cow, villager, zombie and armor stand damage is blocked by default and allowed
  when enabled, with real engine damage and explosion events as positive controls.
- A firework outside a dominion cannot damage protected animals inside it.
  Adjacent dominions with opposite flags prove that the victim's location decides.
- Unclaimed land keeps its ordinary damage behavior.
- Actual dispenser and client-fired crossbow rockets exercise both flag values.
  The crossbow shooter stands outside the dominion and the victim stands inside.
- Existing PVP restrictions for ownerless fireworks remain effective when firework damage is enabled;
  a separate positive control enables both PVP and firework damage.
- Explosions still occur without being cancelled, including ordinary no-star
  rockets. The connected client also logs received explosion animation packets.

The test calls `Firework.detonate()` to make timing deterministic; it does not
construct synthetic damage events. Dispenser and crossbow fireworks fly for two
server ticks before detonation. The test does not exercise a rendered client,
elytra flight, Folia scheduling, or interactions with other protection plugins.

## Simulate an upgrade

After the ordinary restart check, stop this disposable server and back up its
Dominion database and config files. Remove the `firework_damage_entity` column
from its `dominion` table (`ALTER TABLE dominion DROP COLUMN firework_damage_entity;`),
remove the matching environment flag entry from
`flags.yml` and `world-wide/default.yml`, and remove its references in the flag
groups. This represents a schema-5 installation that predates the new flag.
Restart and run `issue288 verify-defaults`; require
`UPGRADE SUMMARY checks=6 failures=0`. Check that the flag entry and world-wide
value were regenerated with a false default. Existing flag groups remain intact;
new flags are available under Ungrouped until added to a customized group.

## Verified fixed build (2026-10-07)

- Paper `1.21.11-132-ver/1.21.11@c5eb079`, Temurin Java 21.0.8, SQLite, Linux.
- Full plugin build and all **112 automated tests passed**.
- Clean-database engine/client run: **22 scenarios, 136 checks, 0 failures**.
- Connected client received **22 firework explosion animation packets** and sent
  **2 crossbow firing inputs** from outside the protected dominions.
- Normal server restart: **6 database/cache checks, 0 failures**.
- Simulated old-schema upgrade: **6 database/cache checks, 0 failures**. All
  existing rows acquired `false`; missing flag/world-wide entries were regenerated
  with false defaults and existing group definitions were preserved.
- Tested JAR: `Dominion-4.9.4-release-full.jar`.
- SHA-256: `590560806feb01356a9cef6d43a2d983085f294651a500d9c56d6082f0e0b7dd`.

The runtime used the official Paper download. Online authentication and update
checks could not access their services; the localhost-only offline test client
and the complete test matrix ran successfully. The disposable server was stopped
when verification finished.
