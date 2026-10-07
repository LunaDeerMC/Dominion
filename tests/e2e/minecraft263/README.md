# Minecraft 26.3 compatibility verification

Use a disposable, localhost-only Paper 26.3 server with Java 25 and a fresh SQLite database.
The probes create persistent claims, replace blocks and teleport test players. They must not
be installed on a production server. The plugin itself does not add any new flags.

## Build and run

1. Build both the plugin and probes from the repository root:
   ```sh
   ./gradlew test :shadowJar -PBuildFull=true
   ./gradlew -I tests/e2e/minecraft263/init.gradle minecraft263HarnessJar
   ./gradlew -I tests/e2e/issue287/init.gradle issue287HarnessJar
   ./gradlew -I tests/e2e/issue288/init.gradle issue288HarnessJar
   ```
2. Install the full Dominion JAR and the three probe JARs from `build/`.
   Set `server-ip=127.0.0.1`, `server-port=25583`, `online-mode=false`,
   `white-list=false`, `enforce-secure-profile=false`, `spawn-protection=0`,
   `allow-flight=true`, and `network-compression-threshold=-1`.
   Start the server and wait for `Done` and Dominion's enabled message.
3. Run `python -u tests/e2e/minecraft263/client.py`. After `PLAY`, run `probe263`
   in the server console. Require `Minecraft263Probe SUMMARY ... failures=0`.
   The client must log a dialog callback and two break requests. Missing SUMMARY
   or any FAIL/exception is a failure. Stop the client after collecting its packet log.
4. For the existing explosion matrix, run:
   ```sh
   PROBE_NAME=Issue287Bot ISSUE287_READY_FILE=/absolute/server/plugins/Issue287/ready.json \
     python -u tests/e2e/minecraft263/client.py
   ```
   It starts `/issue287` itself with a unique run token and sends real right-click
   packets. Require client `RESULT` with 16 scenarios, positive checks, zero
   failures, and the matching server SUMMARY. A stale ready file cannot pass.
5. For the firework matrix, start the client with `PROBE_NAME=Issue288Visitor`,
   then run `issue288` in the console. Require its zero-failure SUMMARY, two
   `CROSSBOW fired` lines and firework visual packets. Restart and run
   `issue288 verify` to validate persistence.
6. Run the [resize probe](../issue296/README.md) on a separate disposable database:
   its fixed claim coordinates conflict with the explosion matrix. Require both
   the initial 127-check summary and the 60-check restart summary.

`PROBE_PORT` overrides the default port. The standard-library Python client uses
protocol 777, including 26.3's position-bearing teleport confirmation and
length-prefixed custom-click NBT. `packet-ids.json` is extracted from the ordered
Paper 26.3 build 159 Login/Configuration/GameProtocols declarations (the bundle
delimiter occupies game clientbound ID 0). It is a protocol probe, not a renderer.

## Scope

- Real server enablement and version-specific NMS backend selection.
- Every enabled existing guest/environment flag's allow/deny decision against a
  real claim and connected non-op player. This tests policy lookup, not every
  gameplay event which can reach each flag.
- Event-dispatch allow/deny tests for 22 existing block interactions and block
  breaking. These are explicitly synthetic Bukkit events on the real server.
- Actual client-to-engine block breaking, denied then allowed with world-state assertions.
- Block/item display creation, unique entity IDs, metadata, teleport and removal
  packets sent to a connected client.
- Actual dialog delivery, custom-click callback execution, and single-use replay rejection.
- Actual bed/anchor/TNT explosions, firework damage, crossbow/dispenser launches,
  boundary protection and SQLite/cache persistence through the existing probes.
- New-content audits log `AUDIT`, separately from regression assertions. In particular,
  the original straw-bed audit preceded approval; straw-bed use now maps to `bed`.

No rendered-client visual verification, Folia/Spigot runtime, external economy,
WorldGuard/PlaceholderAPI integration, or multi-server SQL deployment was tested.
The complete gameplay behavior of every flag is not exhaustively covered.
See [the results and approval proposals](REPORT.zh-CN.md) for exact evidence and gaps.
