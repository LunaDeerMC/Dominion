# Vault / VaultUnlocked economy integration

This opt-in probe runs on a **disposable localhost Paper server** with a connected
non-admin player, real Dominion, EssentialsX, and one real Vault implementation.
It changes the player's Essentials balance, creates persistent claims, and
temporarily registers controlled economy providers. Never install it on a live
server. Use a fresh world and Dominion SQLite database for each server variant.

## Build and run

From the repository root:

```sh
./gradlew :shadowJar -PBuildFull=true
./gradlew -I tests/e2e/vault-economy/init.gradle vaultEconomyHarnessJar
```

Install the full Dominion JAR and `build/vault-economy/VaultEconomyProbe.jar` with
EssentialsX `2.22.0` in two separate Paper `26.2` build `132` / Java `25` servers:

| Server | Bridge plugin |
| --- | --- |
| Classic | Vault `1.7.3` |
| Unlocked | VaultUnlocked `2.20.1` |

Only one bridge belongs in each server's `plugins/` directory. Both bridge JARs
declare their Bukkit plugin name as `Vault`. No other economy plugin should be
installed; the authentic baseline requires Essentials to expose the legacy
`net.milkbowl.vault.economy.Economy` service and no modern service.

Use `server-ip=127.0.0.1`, `server-port=25586`, `online-mode=false`,
`enforce-secure-profile=false`, `network-compression-threshold=-1`,
`spawn-protection=0`, and `allow-flight=true`. Once startup finishes, connect:

```sh
MC_PORT=25586 CLIENT_NAME=VaultProbe python -u tests/e2e/issue298/client.py
```

The existing standard-library protocol client supports Paper 26.2 / protocol
776 and disconnects upon `E298_DONE`. The first player join automatically starts
the suite after 60 ticks. `-Dvaultprobe.manual=true` disables this behavior; use
`vaulte2e VaultProbe` from the console after connecting instead. The test changes
the connected player's op status to false and explicitly denies `dominion.admin`.

Require a `VAULT_E2E ... ALL SUMMARY` with positive checks and `failures=0`, every
phase's `SUMMARY` with `failures=0`, and no `FAIL` lines. Classic Vault runs two
phases; Unlocked runs four. Missing summaries or a disconnected player make the
run incomplete. Preserve server/client logs and the tested JAR hashes with results.

## Authentic plugin baseline

The first phase contains no controlled providers. It reports actual plugin
versions/authors and service registrations, then verifies Dominion selects its
legacy `Vault` adapter in both server variants. It changes real Essentials money
from `1000` to `990` by withdrawal, then to `995` by deposit, checking both
Dominion reads and Essentials' own stored user balance.

It separately seeds `1000`, enables the player's economy settings with price
`0.1` per square block, and dispatches `dominion create` as that connected player.
The normal selection map contains a 20x20 footprint. The actual
`DominionCreateEvent` must retain the operator, footprint and `skipEconomy=false`.
The resulting claim must exist in SQLite, its returned/database IDs must match,
and the real Essentials balance must be `960`.

This exercises command permissions, parsing, selection, provider/event validation,
economy charging and database persistence. Command dispatch originates from
Bukkit; the probe does not send a client chat command packet or perform selection
tool mouse clicks. Claims remain in the disposable database as evidence.

## Controlled service scenarios

These phases use Java proxies implementing the APIs supplied by the actual
bridge JAR. They have a small ledger, transaction counters and real API response
objects. They verify Dominion's handling of API behavior; they do **not** establish
compatibility with a third-party native Vault2 economy plugin.

- **Modern precedence:** register a modern service while real Essentials remains
  registered as the legacy service. Require the `Vault2` adapter, working
  withdrawal/deposit and a paid persisted claim. The legacy balance must remain
  unchanged.
- **Failure handling, for each available API:** insufficient balance must reject
  before calling withdrawal. With sufficient balance, a `FAILURE` response,
  exception before ledger mutation, and exception after mutation must reach the
  caller as errors. Withdrawal and deposit each invoke their provider exactly
  once; no retry, opposite transaction or cross-API transaction is permitted.
  For modern controls a second controlled legacy service independently counts
  any forbidden fallback transactions.
- **Real create rejection, for each available API:** with sufficient balance,
  a provider `FAILURE` response and an exception after withdrawal must both make
  the create result null, leave no SQLite claim, and call withdrawal once. The
  latter intentionally leaves the ledger debited once: the probe verifies that
  Dominion does not retry an operation whose outcome is uncertain. It does not
  assert rollback inside an external provider.

Classic Vault has no Vault2 classes, so the harness accesses both APIs reflectively
and explicitly skips modern controls there. Each phase clears Dominion's cached
adapter to isolate service selection; during an individual failure matrix the
same selected adapter remains in use. Temporary services, the permission
attachment, selection and in-memory economy configuration are cleaned up at suite
completion. The test intentionally leaves fixture claims and balance changes.

The harness targets Paper scheduling, SQLite and the connected-player command
path. It does not cover Folia scheduling, concurrent transactions, server reloads,
cross-server databases or provider-specific transaction rollback.

## Verified run (2026-10-24)

- Dominion: working-tree fix based on `ec88c35bd4d9012a1a15d16a0c25f4ab326a058a`.
- Runtime: Paper `26.2-132-ver/26.2@19ebc4a`, Temurin Java 25, fresh SQLite
  databases, two separate localhost servers and non-admin protocol clients.
- Vault `1.7.3-b131` + EssentialsX `2.22.0`: **2 phases, 64 checks, 0 failures**.
- VaultUnlocked `2.20.1` + EssentialsX `2.22.0`: **4 phases, 135 checks, 0 failures**.
- Both authentic baselines created the paid claim and changed the Essentials
  balance from 1000 to 960. The Unlocked modern-service control also persisted its
  paid claim while leaving the real legacy balance unchanged.
- Every failed-create control left no claim in SQLite. Withdrawal/deposit failure
  responses and exceptions, including exceptions after ledger mutation, invoked
  the selected provider once without a transaction through the other API.
- Both clients received `E298_DONE` and exited successfully; both servers stopped.
  Independent database reads found one successful claim in the classic server
  and two in the Unlocked server, with no failed-create fixtures present.
- `./gradlew :core:test -PBuildFull=true --console=plain`: **196 tests passed**,
  including **43 VaultConnect regression cases**, no failures, errors or skips.
- Full plugin build and probe compilation passed.
- Tested full JAR: `Dominion-work.4-full.jar`, SHA-256
  `0b8aa06411e358b4985c952e2a09b64e695388781f22090dbd9836ab43e9bd3b`.

Before this fix, the same official VaultUnlocked/EssentialsX combination exposed
`Economy Legacy: EssentialsX Economy` and `Economy Modern: None`, but Dominion
reported no economy and did not create the paid claim. Classic Vault worked.

EssentialsX prints an unsupported-server-version notice on Paper 26.2; its actual
legacy economy registration, reads, writes and paid-claim integration passed in
both server variants. This run does not validate its other features on 26.2.
The offline test environment also logs a failed Mojang public-key request; both
clients completed login and the full suite. Modern services and failure responses
above are explicitly controlled test providers, not third-party modern economy
plugin compatibility claims.
