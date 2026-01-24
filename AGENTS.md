# Repository Guidelines

## Project Structure & Module Organization

- `pom.xml` is the parent Maven build that aggregates modules.
- `arbercharts-core/` contains the core library: `src/main/java`, `src/main/resources`, `src/main/javadoc`, and tests in `src/test/java`.
- `arbercharts-demo/` contains the demo app (shaded JAR) under `src/main/java`.
- `site/` hosts the static site (HTML/CSS/JS, images, generated Javadoc).
- `dist/` contains release assets; `target/` folders are build outputs.
- Root docs: `README.md`, `USER_GUIDE.md`, `LICENSING.md`, `SLA*.md`, `RENDERER_CATALOG.md`.

## Build, Test, and Development Commands

- `mvn clean package` builds all modules and produces JARs in each module `target/`.
- `mvn -pl arbercharts-core test` runs core unit tests (JUnit Jupiter).
- `mvn -pl arbercharts-demo -am package` builds the demo JAR with dependencies.
- `java --enable-native-access=ALL-UNNAMED -jar arbercharts-demo/target/arbercharts-demo-1.3.0.jar` runs the demo.
- `mvn -Pperf test` runs performance tests tagged for JMH.

## Coding Style & Naming Conventions

- Java 25; 4-space indentation; braces on the same line.
- Package naming uses `com.arbergashi.charts.*`.
- Public APIs are documented with Javadoc; keep comments consistent with existing style.
- No formatter is enforced; avoid reformatting unrelated code.

## Testing Guidelines

- Unit tests use JUnit Jupiter; performance tests use JMH.
- Default test runs exclude perf tests; tag perf tests with `@Tag("perf")`.
- Use `*Test` naming and place tests under `arbercharts-core/src/test/java`.

## Commit & Pull Request Guidelines

- Commit messages are short, imperative, and sentence-cased (e.g., "Prepare Maven Central publishing").
- PRs should describe scope, impacted modules, and any version bumps in `pom.xml`.
- Include screenshots for demo UI changes and note updates to public docs or release assets.

## Rendering, HiDPI, and Thread Safety Notes

- HiDPI: rendering must scale with OS DPI; demos should verify crisp output on retina/4k.
- Medical scaling: when fixed scaling is enabled, calibration (25 mm/s, 10 mm/mV) must stay stable across resize.
- Models may be written from background threads and read on the EDT; ensure model code is thread-safe and lock-free where possible.
- Zero-GC: renderers with heavy fills (e.g., BoxPlot, Histogram) use cached image layers; keep `getUpdateStamp()` accurate so static frames blit without new allocations.
