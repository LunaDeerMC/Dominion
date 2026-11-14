# Cushion piston empty-shape regression (#301)

Use **only a disposable local Paper/Folia 26.3 server**. The harness creates terrain and
entities around `(100008, 80, 100008)`, overwrites that world's Dominion world-wide
configuration, and stops the server on completion. Never install it on a live server.
Minecraft EULA acceptance must be provided by the server operator.

```sh
./gradlew shadowJar -PBuildFull=true
./gradlew -I tests/e2e/cushion-piston301/init.gradle cushionPiston301HarnessJar
```

Install the resulting Dominion Full JAR and `build/cushion-piston301/CushionPiston301.jar`
in a new server's `plugins/` directory. Bind the server to loopback and use a new flat
world, a new SQLite database, view/simulation distance 2, and no other plugins.
Folia runs use the region scheduler; no player or network bot is needed.

The harness starts at `ServerLoadEvent` and writes `cushion-piston301-results.txt`.
Require `SUMMARY ... failures=0` **and** inspect the server log for unexpected errors,
especially `Cannot getEntities asynchronously` and `Could not pass event ... Dominion`.
Bukkit may catch listener exceptions, so a successful event-state assertion alone
does not prove the listener completed.

Coverage:

- Actual release helper called with real world AIR/WATER/LAVA/STONE blocks, six
  directions and extension/retraction. These are constructed events, not six-direction
  redstone machines.
- Real redstone extension/retraction with water, lava, stone, TNT, and fire.
- Real world-wide permissions, toggling only `cushion_environment_break` while other
  environment flags allow the test circuits.
- Actual cushions in the piston head path and in a pushed stone's destination:
  denied permission must cancel movement and preserve the cushion; allowed permission
  must let the movement destroy it.
- A protected cushion beyond water: destroying water must not create a fictitious
  full-block sweep and cancel an otherwise harmless piston action.

Ordinary material movement and cushion interactions are real server operations;
permissions, entities, Bukkit world geometry, and the Dominion event listener are not
mocked. Thin-block/honey geometry and synthetic outline-empty/collision-nonempty
fallback cases are covered separately by unit tests. This harness does not reproduce
the reporter's specific TNT duplication machine or test third-party event mutation.
