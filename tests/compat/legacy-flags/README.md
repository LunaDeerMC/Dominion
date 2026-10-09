# Legacy flag binary compatibility fixture

`LegacyFlagReader.java` represents an addon that still calls the old `CONTAINER`
API. Its checked-in JAR is compiled **against the API before the runtime fallback
change**, not against the API being tested. Gradle does not compile this source.
`LegacyFlagBinaryCompatibilityTest` loads only the consumer class from that JAR;
all Dominion API classes and implementations come from the current test runtime.

The same consumer object retains the same public permission Map while the test
sets `CHEST` to false, true, then false. It checks the old constant, lookup by name,
public flag enumeration, the DTO getter, Map lookup/default/key/entry operations,
and both original silent privilege-check overloads through `DominionInterface`.
The database and player/cache are mocked, but the DTO, Map, API entry points and
permission-check implementation are real. This is a binary compatibility test,
not a Minecraft server integration test.

## Rebuild intentionally

With a JDK 17 or newer on `PATH`, supply an API JAR from **before this change** and
the Paper 1.20.1 API JAR:

```bash
bash tests/compat/legacy-flags/compile.sh /path/to/pre-fallback-api.jar /path/to/paper-api.jar
```

The optional third argument selects a different output file. Compilation uses
`--release 17 -g:none`, and the class-only JAR uses a fixed timestamp. The script
prints SHA-256 hashes of its inputs and output. Do not replace the first argument
with the current API artifact: that would lose the test's independent old binary.
No archived API JAR is needed when running tests.

The checked-in fixture was built with `javac 17.0.16` from the unchanged API
submodule commit `b122447f74b71826b65af6709326d13b7d76f421`. This baseline already
defines split permissions but does not resolve reads of their deprecated names.
The consumer source deliberately refers only to the historical API calls and
never mentions `CHEST` or the new alias helpers.

| Input or output | SHA-256 |
| --- | --- |
| Baseline API JAR | `f966d34d0824aab80d3cd8953254a8b56d2c4224610af937f6831cc499b69518` |
| Paper 1.20.1 API JAR | `161ecf24e6ffb325a79eb7eb04904419c2b80f602672e4758785e9389e9038d1` |
| `LegacyFlagReader.java` | `aa14206a9f73875ac16d348b2206c66f08299d3472dcc7fb1741af2af88608a6` |
| `legacy-flag-reader.jar` | `453640a238ed8afc9c22ce7694796d8d907eb5055509dca8ce125de3a2409472` |

The baseline API JAR hash documents the actual compiler input; independently
building the same API commit can change archive metadata and therefore its hash.
