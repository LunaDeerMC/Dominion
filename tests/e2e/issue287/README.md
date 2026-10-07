# Issue #287: bed and respawn-anchor explosion protection

This opt-in regression test runs on a **disposable Paper server** with a real,
non-OP Minecraft protocol client. The harness creates four Dominion claims,
sets their persisted flags, resets nearby terrain, and checks the blocks after
real anchor, bed and TNT explosions. It does not synthesize explosion events or
add an alternative protection listener.

Do not install it on a production server: it rewrites blocks around `(0,100,0)`
and `(0,100,8)` in the first overworld and Nether, and retains its claim fixtures.

## Run

1. Build Dominion and the test plugin from the repository root:
   ```bash
   ./gradlew :shadowJar -PBuildFull=true
   ./gradlew -I tests/e2e/issue287/init.gradle issue287HarnessJar
   ```
2. Install the full Dominion JAR and `build/issue287/Issue287.jar` in a disposable
   Paper server's `plugins/` directory. Start with an empty Dominion database,
   both overworld and Nether enabled, and no other gameplay/protection plugins.
   Bind the server to localhost and use these test settings:
   ```properties
   server-ip=127.0.0.1
   server-port=25587
   online-mode=false
   enforce-secure-profile=false
   spawn-protection=0
   allow-flight=true
   view-distance=3
   simulation-distance=3
   ```
3. Install the bot dependencies with Node.js 22 or later:
   ```bash
   cd tests/e2e/issue287
   npm ci
   ```
4. Start Paper and wait for startup to finish. From the same machine, run:
   ```bash
   node client.js /absolute/server/path/plugins/Issue287/ready.json
   ```
   The client connects as `Issue287Bot`, sends `/issue287 <run-token>`, and
   right-clicks the actual blocks prepared by the harness. Do not give the bot
   operator status. Allow about four minutes for the matrix.
5. Require client exit status **0**, a server log line with
   `SUMMARY scenarios=16/16 ... failures=0`, and no `[Issue287] FAIL` lines.
   A missing summary or a nonzero client exit means the run did not pass.

The default client protocol is `26.1`, compatible with the tested Paper
26.1.2 server. `ISSUE287_VERSION`, `ISSUE287_HOST`, and `ISSUE287_PORT` override
the connection settings. `ISSUE287_READY_FILE` can replace the path argument.
The client and server share the ready file; each run has a unique token so stale
results cannot pass a new run. The client requires all 16 scenarios, a positive
check count, and zero failures. It fails on connection/click errors or an
eight-minute timeout.

The fixture claims can be reused on a second run, including after restart. The
harness checks their owner, world and exact bounds before reuse. Other claims
at the test coordinates prevent setup and fail the run. This is a Paper
scheduler harness, not a Folia test.

## Coverage and assertions

- An anchor in the overworld or bed in the Nether explodes immediately outside
  the claim. The corresponding explosion flag is tested as false and true.
- `anchor_explode` and `bed_explode` are deliberately opposite, proving that
  each source uses its own flag. TNT flags stay false in these cases.
- Inside each claim, denied `anchor`/`bed` interaction prevents the explosion;
  allowed interaction is tested with block damage both disabled and enabled.
- Real primed TNT outside the claim verifies `tnt_explode=false/true` and keeps
  the preexisting entity-explosion protection covered.
- One anchor/bed explosion reaches two adjacent claims with opposite flag
  values. Both assignments are tested to require per-target protection.
- Before every interaction, database and cache values and all target/source
  claim memberships are checked. The bot is a non-owner and non-OP.
- After each explosion, all protected glass must survive and some allowed
  glass must be destroyed. Unclaimed glass must still be damaged, proving
  protection does not cancel the entire explosion. Allowed damage uses a
  positive-count assertion because vanilla blast propagation is randomized.

The primary claim is `[0,16) × [95,110) × [-8,8)`; its neighbor is
`[0,16) × [95,110) × [8,24)`. Ordinary cases place 30 glass targets inside the
primary claim and 3 outside. Adjacent cases place 18 targets in each claim and
3 outside. Bed/anchor interactions use the real TCP client. TNT is spawned as
a real primed entity with a one-tick fuse. Player invulnerability and flight
keep the test client stable; they do not change the block-explosion path.

## Compatibility boundary

Modern explosion events preserve the original block state even though the live
block at the source is already air. The fixed listener reads that snapshot.
Old Spigot APIs without `getExplodedBlockState()` cannot distinguish bed from
anchor explosions at this point; the production fallback permits block damage
only when **both** explosion flags allow it. This deliberately conservative
fallback cannot provide independent bed/anchor behavior on those old APIs.
The matrix above verifies independent flags on the modern Paper API. The
probe uses reflection only to print optional snapshot diagnostics, so it can
compile against the project's base API.

The production listener was also loaded against the real Spigot 1.20.1 API in
an ABI compatibility smoke test. Six checks covered all four flag combinations,
per-location filtering, and already-cancelled events. This checks the legacy
API fallback, not gameplay on an old Spigot server.

## Recorded verification

The initial investigation reproduced the issue on Paper
`26.1.2-74-ver/26.1.2@e4e17fc`, Java 25 and SQLite: a real outside-anchor click
with `anchor_explode=false` destroyed all 30 claimed glass blocks. The event's
live material was `AIR`, while its snapshot material was `RESPAWN_ANCHOR`.
Nether beds showed the same defect with `RED_BED` snapshots.

Fixed build verified on 2026-10-07 with the same Paper version, Temurin Java
25+36, SQLite, and the committed protocol-client dependencies:

- **16/16 scenarios, 234 checks, 0 failures**; client exit status 0.
- Disabled anchor/bed explosion flags preserved all 30 claimed targets; enabled
  flags destroyed all 30. Unclaimed targets were still destroyed in both cases.
- Opposite flags in adjacent claims preserved all 18 protected targets and
  destroyed all 18 allowed targets, for both source types and assignments.
- Denied inside interactions caused no explosion; allowed inside interactions
  separately obeyed their explosion-damage flag. TNT controls passed.
- Tested JAR: `Dominion-4.9.4-release-full.jar`.
- SHA-256: `3b0cc2d5f2ab929ea3da26bd4c22eea281cf61beed4a05f71c6c48e0ba553cce`.

The Paper runtime used its cached official development bundle's patched server
and resolved dependencies. Authentication-key/update requests to external
services were unavailable; the offline localhost gameplay and assertions ran
successfully. This records a Paper 26.1.2 gameplay run, not all supported server
versions.
