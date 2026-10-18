# Repository Guidelines

## Project Structure & Module Organization

- `pom.xml` is the parent Maven build that aggregates modules.
- `arbercharts-core/` contains the core library: `src/main/java`, `src/main/resources`, `src/main/javadoc`, and tests in `src/test/java`.
- `arbercharts-swing-bridge/` and `arbercharts-server-bridge/` own the desktop and Java2D server backends.
- `arbercharts-spring-boot-starter/` provides server auto-configuration; `arbercharts-starter/` is a local bundle module.
- `arbercharts-demo/` and `arbercharts-spring-boot-demo/` contain the Swing and browser workbenches.
- `docs/` contains current technical documentation and dated verification evidence for the current release.
- `dist/` and module `target/` directories contain generated output and are not committed.
- Root documentation consists of `README.md`, `RELEASING.md`, the current release notes, `LICENSE`, and `NOTICE`.

## Repository Content

- Keep GitHub focused on source, technical documentation, tests, and current demo images.
- Commercial offers, pricing and service agreements belong in the separate business portal.
- The product website is maintained in the separate portal. Do not restore a repository-root `site/` directory or a second product website here.
- Keep one canonical MIT `LICENSE`; do not add contradictory source-access or binary-only delivery terms.
- Remove obsolete release instructions, migration scripts and generated state from the current tree. Git history retains earlier versions.
- Run `python3 tools/check_repository.py` to check documentation links, release metadata and repository content before publishing.

## Build, Test, and Development Commands

- `mvn clean package` builds all modules and produces JARs in each module `target/`.
- `mvn -pl arbercharts-core test` runs core unit tests (JUnit Jupiter).
- `mvn -pl arbercharts-demo -am package` builds the demo JAR with dependencies.
- `java -jar arbercharts-demo/target/arbercharts-demo-2.5.0.jar` runs the Swing demo on a graphical desktop.
- `java -Djava.awt.headless=true -jar arbercharts-spring-boot-demo/target/arbercharts-spring-boot-demo-2.5.0.jar` runs the browser demo.
- `mvn -Pperf test` runs JUnit tests tagged `perf`. JMH benchmarks live in the Swing bridge and run through the JMH launcher; see `docs/VERIFICATION_STRATEGY.md`.

## Coding Style & Naming Conventions

- Java 25; 4-space indentation; braces on the same line.
- Package naming uses `com.arbergashi.charts.*`.
- Public APIs are documented with English Javadoc; keep code comments and documentation in English.
- No formatter is enforced; avoid reformatting unrelated code.

## Testing Guidelines

- Unit tests use JUnit Jupiter; performance tests use JMH.
- Default test runs exclude perf tests; tag perf tests with `@Tag("perf")`.
- Use `*Test` naming and place tests under the owning module's `src/test/java`.

## Commit & Pull Request Guidelines

- Commit messages are short, imperative, and sentence-cased (e.g., "Prepare Maven Central publishing").
- PRs should describe scope, impacted modules, and any version bumps in `pom.xml`.
- Include screenshots for demo UI changes and note updates to public docs or release assets.

## Rendering, HiDPI, and Thread Safety Notes

- HiDPI: rendering must scale with OS DPI; demos should verify crisp output on retina/4k.
- Medical scaling: when fixed scaling is enabled, calibration (25 mm/s, 10 mm/mV) must stay stable across resize.
- Models may be written from background threads and read on the EDT; ensure model code is thread-safe and lock-free where possible.
- Zero-GC: renderers with heavy fills (e.g., BoxPlot, Histogram) use cached image layers; keep `getUpdateStamp()` accurate so static frames blit without new allocations.
