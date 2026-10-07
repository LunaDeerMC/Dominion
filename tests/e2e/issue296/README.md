# Issue #296: resize boundary regression

This is an opt-in test plugin for a **disposable Paper server**, not a unit test.
It creates 30 test dominions and a test owner, dispatches real `/dominion resize`
commands, and compares all six coordinates in the database and the Dominion cache.
It does not remove its fixtures. Do not install it on a production server.

## Run

1. Build Dominion: `./gradlew :shadowJar -PBuildFull=true`.
2. Build the harness from the repository root:
   `./gradlew -I tests/e2e/issue296/init.gradle issue296HarnessJar`.
3. Install the Dominion full JAR and `build/issue296/Issue296.jar` into the test
   server's `plugins/` directory. Use an empty Dominion database to avoid fixture
   collisions. Start Paper and wait for startup to finish.
4. Run `issue296` from the server console once. Allow roughly two minutes.
5. Check `logs/latest.log` for `[Issue296] SUMMARY` with `failures=0` and no
   `[Issue296] FAIL` lines. Missing SUMMARY means the run did not finish.
6. Restart the server, wait for startup to finish, and run `issue296 verify`.
   Require `RESTART SUMMARY` with `checks=60 failures=0`.

The harness uses the console command path, real Bukkit events, asynchronous
provider operations, SQLite and cache reloads. It does not simulate a connected
Minecraft client or inventory/dialog clicks. It uses the Paper scheduler and is
not a Folia harness.

## Coverage

- Expand 1 and 100 blocks in all six directions, including negative X coordinates.
- Reject negative and zero sizes, including `Integer.MIN_VALUE` and negative
  contraction amounts, without changing stored or cached bounds.
- Back-to-back expansions in the same and different directions: every operation
  reported as successful must be reflected in the saved bounds. An overlapping
  operation may explicitly fail; retrying after completion must succeed.
- A retained DTO from before a successful resize must not overwrite newer bounds.
- Cancellation and a listener changing the size to a negative number must release
  the resize guard, allowing subsequent valid operations.
- Ordinary positive-size contraction remains functional.

## Findings

On the unmodified commit `b674ebf6`, Paper 26.2 build 132 / Java 25 / SQLite:

- All 12 single-command positive-size tests (six directions, 1 and 100 blocks)
  produced the expected bounds. The report's assertion that a *single positive
  100-block expansion* necessarily corrupts coordinates was not reproduced.
- `expand -100` on a 20-block extent shrank it to one block in all six directions.
  For example, X `[2000, 2020)` became `[2000, 2001)`.
- Issuing `expand 100 west` followed immediately by `expand 100 north` reported
  two successes, but X `[9000, 9020)` remained unchanged while Z became
  `[-100, 20)`: the second snapshot overwrote the first expansion.

The fix rejects non-positive sizes, permits only one pending resize per dominion
in this plugin instance, and checks the old bounds against the database before
validation, economy changes and saving. A busy or stale operation reports a retry
message instead of silently overwriting newer coordinates. Event listeners can
still modify the requested size/direction or cancel the operation.

This does not introduce cross-server locking or serialize unrelated operations
such as direct API `setCuboid` calls. Folia, connected-player permissions, economy
plugins and GUI interactions require separate environment-specific testing.

## Verified fixed build (2026-10-07)

- Paper `26.2-132-ver/26.2@19ebc4a`, Temurin Java 25, SQLite, Linux.
- Full plugin build and harness compilation passed.
- Clean-database run: **30 scenarios, 127 checks, 0 failures**.
- Server restart: **60 database/cache checks, 0 failures**.
- Tested JAR: `Dominion-fix-issue-296-resize-safety.1-full.jar`.
- SHA-256: `4c9ca5b1067b02e65bfe85efc520ef87ab4524d6b10a8ec140c58b81a98f404f`.

The Paper runtime was bootstrapped from the cached official 26.2 development
bundle's Paperclip and patched Minecraft server, with its resolved dependencies.
Public server download endpoints were unavailable in this environment. Online
authentication and update-check requests failed due to network restrictions;
the offline, localhost-only test server and the test matrix ran successfully.
