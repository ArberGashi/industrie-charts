# Industrie Charts 2.2.0

Release date: 16 October 2026.

Version 2.2.0 separates the production Core from desktop APIs, strengthens
backend contracts, corrects renderer and grid behavior, and introduces refreshed
Swing and browser workbenches. The runtime baseline is Eclipse Temurin 25.

## Architecture and migration

- Production Core no longer depends on AWT, Swing, ImageIO or the bridge modules.
  Whole-module architecture checks use the JDK Class-File API and `jdeps` instead
  of ArchUnit. The checks also run without activating the optional Vector module.
- `ViewportAuditExtractor`, `ZeroAllocPool` and `AllocationStats` retain their
  Java package names but move to `arbercharts-swing-bridge`. Applications using
  these desktop helpers must add the Swing bridge when upgrading.
- Backend contracts cover geometry, text, clipping, alpha, pooled state and
  spatial metadata. The Java2D server bridge still requires `java.desktop` in
  headless mode. Additional frontends implement the existing rendering contracts.

See the [migration guide](docs/RENDERING_ARCHITECTURE.md#migration-from-210)
for module ownership and the current text and spatial limitations.

## Renderer, grid and host corrections

- Shared grid geometry aligns ordinary Canvas and batch paths, including polar
  and ternary coordinates.
- Targeted numerical fixes cover financial indicators and OHLC geometry,
  medical ring-buffer ordering, statistical distributions, circular profiles,
  analysis transforms and specialized coordinate systems.
- Radar profiles and grid rings close correctly, handle missing data and reuse
  drawing buffers. KDE preparation preserves distinct observations and their
  weights, includes kernel tails and invalidates cached data correctly.
- Swing axes, secondary scales, docked legends, tooltips, crosshairs, preferred
  export sizes and host clipping receive focused regression coverage.
- Existing image tests now reject transparent output and use a corrected raster
  test backend. Numerical tests include independent reference calculations.

The [correction record](docs/audits/2026-10-16-rendering/corrections.md) maps changes
to tests. The independent audit inventories all 156 catalog entries; it does not
claim complete visual acceptance of every renderer.

## Workbenches and documentation

- The Swing workbench has a searchable catalog, light/dark themes and consistent
  chart actions. Animation uses monotonic active time, accumulates fractional
  wheel input, pauses hidden examples and releases timers and model listeners.
  Reduced motion is supported.
- The new Spring Boot 4.1.1 workbench serves eleven real examples: line, area,
  step, bar, scatter, candlestick, donut, radar, heatmap, box plot and KDE. PNG
  requests use fresh renderer state and bounded output sizes and concurrency.
- The README replaces all six obsolete preview images with actual full
  Workbench captures in both themes. Standalone PNG examples remain available.
- Public Javadoc is generated in English with strict checks. Dependencies and
  build plugins are updated as recorded in [Dependency updates](docs/DEPENDENCY_UPDATES.md).

## Downloads and runtime

GitHub provides current Swing and Spring Boot executable JARs and ZIP bundles,
the library JARs with sources and Javadoc, and SHA-256 checksums. Source archives
are available from the release tag. Use an existing Eclipse Temurin 25
installation; a commercial JDK is not required.

Maven coordinates remain under `com.arbergashi`, at version `2.2.0`:

- `arber-charts-parent`
- `arbercharts-core`
- `arbercharts-server-bridge`
- `arbercharts-spring-boot-starter`
- `arbercharts-swing-bridge`

The demo applications are GitHub downloads rather than Maven Central modules.
This release contains current JVM artifacts. The obsolete bundled macOS native
library is removed; no newly built native library or native-image validation is
claimed.

## Verification and limits

The full quality-gate reactor contains 856 test cases with no failures or errors;
three opt-in stress cases are skipped. The Core scalar fallback and eight
frontend state tests are checked separately. Browser and Swing captures provide
selected visual evidence.

Physical medical calibration, mixed-monitor DPI, full PDF/SVG comparisons,
long-running stress and workload-specific allocation/performance measurements
remain separate acceptance checks. See [Verification strategy](docs/VERIFICATION_STRATEGY.md).
