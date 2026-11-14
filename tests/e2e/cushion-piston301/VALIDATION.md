# Local validation, 2026-11-14

Executed on a new, loopback-only DeerFoliaPlus 26.3 world with Java 25.0.4.1.
The only installed plugins were Dominion and `CushionPiston301`.

- Standard Gradle `:versions:v26_3:test`: **72 tests passed**, including 56 new
  piston regressions and all existing tests in that version module.
- The version-module JAR and the documented Gradle harness build also completed.
- An independent JUnit run against the actual 26.3 runtime passed all 61 selected
  piston/cushion-source tests. Using the same tests against the unchanged official
  4.9.9 JAR produced 41 expected assertion failures and 20 passes, confirming the
  tests detect the original behavior rather than merely exercise the fixed code.
- **138 assertions passed, 0 failures.** Full harness output is in `RESULTS.txt`.
- No `Cannot getEntities asynchronously`, `Could not pass event`, or harness errors
  appeared in the complete server log.
- Water/lava extension and ordinary stone/TNT/fire circuits succeeded.
- Head-only and pushed-stone interactions cancelled and preserved cushions when
  protection denied destruction; the actual movement destroyed them when permitted.
- A protected cushion beyond water survived, and the harmless piston action succeeded.
- The server stopped normally after the final test.

The tested artifact is a **local regression build**, not a published release or a
claim of a full Gradle rebuild: the official Dominion 4.9.9 Full JAR was copied and
only `CushionPistonProtection.class` was replaced with the fixed source compiled
using `javac --release 21` against the actual runtime API.

| Artifact | SHA-256 |
| --- | --- |
| Official Dominion 4.9.9 Full | `22fa8d3e11ce90749acebf69252ece1b7555c17c0b86e1602185000cd18cfa71` |
| Fixed Java source | `993208abbe6bffb48d9e1cc3e8253ad93cccd8a3fda645a9bf4dbaad0b4799a7` |
| Replacement class | `3408008e4b7f29d1c56188e622aa6ce8e03db80b324429c378bfbbcb9390674c` |
| Tested Dominion JAR | `bd698f295d3e622ccf3032ecbd8a7303448b3399a1cbaafc24f69fe0bebdcc64` |
| DeerFoliaPlus 26.3 core | `7641f8d5d3c5717072ec47f26b46f9d40afbd02071937ea28f67962a0ddcfee8` |
| Harness JAR | `0c718bfd1a28c95254361d3ea8dd3cb543f2437cc2f6b9180b34a58841a4bccc` |

The raw console log and artifact provenance are retained outside the repository at
`/workspace/scratch/dominion-issue301-fix/`. Test results do not claim verification of
all Folia forks, the reporter's TNT duplication machine, or deferred/third-party
mutations of event block states. A separate Paper single-thread server was not run.
