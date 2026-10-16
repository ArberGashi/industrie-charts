# Final verification record — 16 October 2026

This record covers the **2.2.0** release build and its preceding verification.
Publication is recorded separately by the GitHub release and Maven Central
artifact metadata. The original [investigation](README.md) remains separate from the
[implemented corrections](corrections.md).

## Java build

The final eight-module reactor completed successfully with the existing
**Eclipse Temurin 25.0.4.1** installation and **Maven 3.9.16**:

```sh
mvn -B -Djava.awt.headless=true -Pquality-gate,central-release clean verify
```

The build used freshly compiled output following `clean`; strict Javadoc checks,
Checkstyle, dependency governance, binary JARs, source JARs and Javadoc JARs all
completed. Release artifacts were signed with the existing publishing key.
The executable Swing and Spring Boot JARs were staged under their exact `2.2.0`
directories and used for the final captures.

| Module | Test cases | Failures / errors | Skipped |
| --- | ---: | ---: | ---: |
| Core | 466 | 0 / 0 | 2 |
| Server bridge | 10 | 0 / 0 | 0 |
| Spring Boot starter | 2 | 0 / 0 | 0 |
| Swing bridge | 77 | 0 / 0 | 1 |
| Swing demo and backend contracts | 230 | 0 / 0 | 0 |
| Spring Boot demo | 71 | 0 / 0 | 0 |
| **Total** | **856** | **0 / 0** | **3** |

The three skipped cases are opt-in stress tests: two concurrent model read/write
tests and the long-running Swing pan/zoom memory test. They require
`-Darbercharts.stress=true` and were not run in this verification.

The final Core also passed separately without the Vector API runtime flag:

```sh
mvn -o -B -pl arbercharts-core -Darbercharts.test.jvmargs= test
```

That run contains the same 466 cases, with no failures/errors and the same two
stress skips. The class-file architecture scan still verifies optional Vector
references by reading class resources without activating the module.

Inspection of the final Core JAR found no moved desktop allocation or viewport
extraction classes. `jdeps -summary` reported only `java.base`, `java.logging`,
`jdk.incubator.vector` and the provided native-image API. It reported no
`java.desktop` or unresolved dependency. All six generated Javadoc entry pages
declare English as their language. No ArchUnit dependency or import remains.

## Browser demo and images

The executable application serves eleven real renderer examples. Its 71 Java
tests include 54 HTTP/PNG cases and 17 deterministic data contracts. The eight
Node browser-state regression tests passed independently:

```sh
node --test arbercharts-spring-boot-demo/src/test/frontend/workbench.test.mjs
```

The running browser was inspected in light and dark themes. Fresh workbench
captures show the current eleven-example catalog at desktop and narrow widths.
No browser warnings or errors were reported during those checks.

An independent review of the first gallery captures found that the search icon
did not scroll with its input. Giving the search field a positioned containing
block corrected the actual UI. The affected Spring Boot reactor was rebuilt
with `mvn -o -B -Pquality-gate -pl arbercharts-spring-boot-demo -am verify`:
549 cases, no failures/errors and the two Core stress skips. The eight Node tests
also passed again. The running JAR was replaced before the final captures;
browser geometry confirmed that the field and icon now scroll together.

The README shows **twelve actual 1280 × 720 Workbench captures**:
Candlestick, Heatmap, Donut, Radar, Boxplot and KDE in both themes. They include
example navigation, chart settings and the dataset inspector. The
[capture manifest](../../../arbercharts-spring-boot-demo/docs/screenshots/workbench-gallery/manifest.json)
records the selected page states, viewport, image hashes and final executable
artifact hash. All twelve images were visually reviewed. The README displays
each example at full content width; its six obsolete image files were removed.
The staged browser distribution includes the refreshed documentation images.

Six renderer families were exported in both themes through the running HTTP
application at **1600 × 900** output pixels and **2×** scale: Candlestick,
Heatmap, Donut, Radar, Boxplot and KDE. All twelve images were visually reviewed.
The [export manifest](../../../arbercharts-spring-boot-demo/docs/screenshots/renderers/manifest.json)
records exact requests, image hashes and the executable artifact hash. These
standalone chart exports remain available separately for reports and renderer
comparisons.

Swing documentation images were also repainted from the final executable JAR
on the EDT at 2× scale, including the corrected initial multi-series range.
The [Swing workbench documentation](../../../arbercharts-demo/docs/WORKBENCH.md)
contains the dimensions, artifact checksum and reproducible capture command.

## Coverage limits

This is regression and selected visual evidence, not complete acceptance of all
156 catalog entries. Native trackpad/window gestures, physical medical
calibration, mixed-monitor DPI, full PDF/SVG comparison, long-running animation
and concurrency stress, native-image compilation and workload-specific
performance/allocation measurements remain separate acceptance checks.
