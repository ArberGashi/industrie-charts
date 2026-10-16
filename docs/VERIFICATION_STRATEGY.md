# Verification strategy

This document describes checks in the **2.2.0** source tree. It distinguishes automated contracts from visual approval and performance measurements. The [independent rendering audit](audits/2026-10-16-rendering/README.md) preserves the original defects. The [correction record](audits/2026-10-16-rendering/corrections.md) maps subsequent renderer, grid and UI fixes to regression evidence; a passing build alone does not establish complete visual acceptance.

## Existing verification layers

| Layer | What is checked | What it does not establish |
| --- | --- | --- |
| Core unit, property and concurrency tests | Model invariants, selected concurrent-access behavior, context calculations, registry contracts and spatial processing | Correctness under every workload or all application concurrency patterns |
| `CoreSovereigntyTest` | Every compiled production Core class, platform/framework boundaries, public renderer API rules and Maven dependency declarations | Runtime behavior of reflection or arbitrary external providers |
| `ZeroGcArchitectureTest` | Forbidden desktop constructor calls, per-call `StringBuilder` construction in renderer code and the `ChartRenderer` implementation hierarchy | A general proof of allocation-free rendering |
| `CanvasBackendContractTest` | 15 scenarios on each of the Swing and server canvases, plus two Swing host-clip regressions: **32 cases** covering geometry, negative/empty sizes, bounds, large polylines, clipping/restoration, widths, alpha, extreme offscreen input and voxel state | Complete charts, pixel-identical edges or all spatial styles |
| `CanvasTextContractTest`, `OverlayTextContractTest`, `NativeTextContractTest` | Actual raster glyphs and font metrics, baseline placement, clipping, alpha, enabled overlays, state reset and native text encoding | Every installed font, text shaping or the external native consumer's raster output |
| `GridGeometryContractTest`, `SpatialRendererBackendContractTest` | All eleven grid Canvas/batch paths, shared ternary/polar geometry, independent axis visibility and actual Swing/server spatial markers, traces and alpha | Catalog-wide visual equivalence or a logarithmic tick-selection policy |
| `CommandStreamCanvasTest` | Command headers/payloads, clip save/restore/clear and portable voxel fill commands | Rendering correctness of an external command-stream consumer |
| `ServerRenderServiceTest` | Non-empty output, preferred renderer ranges and explicit bounds, pooled-state reset, preservation of returned images, failed renders, context replacement, overflow and shutdown with active renders | An unbounded concurrency or resource-leak soak |
| `RendererSmokeTest` in the demo | 155 catalog `ChartRenderer` entries are constructed and rendered through the server host; most must produce non-transparent output | Numerical correctness, label quality, complete UI composition or visual acceptance |
| `RendererVisualRegressionTest` in Core | Visible pixels and repeatable hashes for eight bounded renderer fixtures; a deliberately empty renderer must fail the content oracle | A reviewed image baseline or a semantic correctness oracle |
| Family-specific renderer regressions | Independent numeric and geometry landmarks for financial, medical, statistical, analysis, circular and specialized corrections, including logical ring buffers and cache invalidation | Exhaustive correctness for every catalog entry, input or mutation sequence |
| Swing presentation and demo workspace tests | Axis precision, host coordinates, secondary axes, transforms, dock/legend behavior, meaningful export content, initial multi-series ranges and disposal of demo-owned asynchronous renderers | Real-display calibration, all accessibility workflows or a long-running lifecycle soak |
| Swing animation tests | Controlled monotonic timing, fractional wheel input, rapid retargeting around the visible pointer position, crosshair fade cancellation, hidden-card pause/resume, reduced motion and timer/model-listener cleanup; actual OHLC/vector frame changes | Physical display cadence, native trackpad gestures or a long-running animation soak |

The smoke suite excludes the separate `PerformanceAuditRenderer` helper and permits blank output only from the disabled-by-default `CalibrationRenderer`. Other overlays receive active state and must paint content; performance overlay output is checked separately. Test-owned closeable renderers are released after each smoke case.

The Core test named `RendererVisualRegressionTest` compares two renders from the same implementation; it does not use ApprovalTests or stored golden images. The old nonzero-CRC check was insufficient because a transparent buffer also has a checksum. The current test separately requires nonzero alpha, and its test-only Java2D canvas fills polygon interiors and uses half-open rectangle extents. Separate regressions exercise those reference-canvas semantics. Other repaired test oracles are recorded in the [correction record](audits/2026-10-16-rendering/corrections.md#test-oracle-corrections).

## JDK-based architecture checks

ArchUnit has been removed. `CompiledCore` uses the **Java 25 class-file API** to inventory and verify bytecode, and the JDK `jdeps` tool to analyze dependencies. It locates production output through the code source of `Charts.class`, so custom Maven output directories are supported. Tests and neighboring bridge modules are not imported as production Core classes.

Bytecode verification resolves type hierarchies from class-file resources in the
installed JDK modules and test classpath. It does not load or initialize those
types or activate the optional Vector API module. A separate process with only
`java.base` resolved verifies the complete Core; invalid assignments and missing
type hierarchies must still fail. This keeps the architecture checks active in
the scalar fallback test run.

The scan fails on empty or malformed input, analyzer failure, unresolved dependencies, or classes missing from the dependency result. The module boundary covers every package owned by `java.desktop`, including ImageIO and audio, as well as the configured UI/server frameworks and bridge packages. Compile, runtime, provided and optional platform declarations in the Core POM are checked.

Fixtures outside the `.core` package exercise AWT, ImageIO, audio, generic type signatures and annotations. Additional fixtures cover public final types, direct constructor calls, constructor method references, and the distinction between reusable setup buffers and per-call allocations. Existing public integration types have explicit allowlist entries; packages are not broadly exempted. Renderer placement follows the `ChartRenderer` hierarchy rather than a class-name suffix.

Direct `StringBuilder` allocation during construction or static initialization is allowed so an instance can reuse its buffer. Allocation in render methods and their helpers remains prohibited by the structural rule. Cache misses, buffer growth, string formatting, collection internals and backend behavior still require workload-specific allocation profiling. No static check establishes universal zero-GC behavior.

## Commands

Use the existing **Eclipse Temurin JDK 25** and **Maven 3.9 or newer**. CI already uses Temurin 25 on Linux, Windows and macOS. An Oracle JDK or another machine-wide Java installation is not required. The current development dependency baseline includes Spring Boot 4.1.1 and JUnit 6.1.3; see [Dependency updates](DEPENDENCY_UPDATES.md).

From the repository root:

```sh
# Full reactor: tests, fresh binaries, source JARs and English Javadoc JARs.
mvn -B -Pquality-gate -Djava.awt.headless=true clean verify

# Core scalar fallback without the Vector API runtime module.
mvn -B -pl arbercharts-core -Djava.awt.headless=true test -Darbercharts.test.jvmargs=

# Checkstyle and Maven baseline/dependency governance.
mvn -B -Pquality-gate validate

# Focused architecture checks, with a separate output directory.
mvn -B -pl arbercharts-core test \
  -Dtest=CoreSovereigntyTest,ZeroGcArchitectureTest \
  -Darbercharts.buildDirSuffix=-architecture

# Both raster backends and the renderer catalog, including required reactor modules.
mvn -B -pl arbercharts-demo -am -Djava.awt.headless=true test \
  -Dtest=CanvasBackendContractTest,CanvasTextContractTest,SpatialRendererBackendContractTest,RendererSmokeTest,DemoWorkspaceTest \
  -Dsurefire.failIfNoSpecifiedTests=false

# Browser request ordering, retries and current-image download state.
node --test arbercharts-spring-boot-demo/src/test/frontend/workbench.test.mjs
```

These are reproducible verification commands, not a claim that every check was executed on every revision. Keep generated `target`/`target-*` output out of source control. Avoid simultaneous builds against the same output directories or audit probes reading a JAR while it is being replaced.

## Visual and performance follow-up

Use the [dated audit and its retained probes](audits/2026-10-16-rendering/README.md) to select focused regressions before changing renderer behavior. Its complete component inventory does not mean every component received independent numerical or visual acceptance.

Future acceptance must combine semantic geometry assertions with reviewed images, realistic labels and data, repeated model/theme changes, ordinary and spatial dispatch, and complete export paths. Real-display HiDPI checks, physical medical calibration, interaction/accessibility, animation and concurrency soak remain separate tasks. Do not promote known-defect audit images into golden baselines.

Run performance and allocation measurements with explicit data sizes, warmup, JVM options and backend settings. Report measured allocations and timing distributions for that workload; distinguish setup and cache growth from steady-state drawing. The repository's performance profile is available through `mvn -Pperf test`, but functional or structural test success is not a performance result.
