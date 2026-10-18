# Industrie Charts 2.5.0

Release date: 18 October 2026.

Version 2.5.0 corrects model, rendering and integration defects found during the
release review, refreshes the Swing workbench, and cleans the repository and
packaged notices. Both executable demos are built from the same release sources
as the libraries.

## Correctness and integration

- Keep negative and constant series inside automatically calculated Y bounds.
  Apply logarithmic X/Y transforms in both directions and test them against
  independent numeric expectations.
- Generate finite, advancing ticks for narrow ranges at large offsets; avoid
  repeated grid lines when floating-point addition cannot advance a tick.
- Break area fills at missing or invalid samples instead of connecting across
  absent data.
- Preserve logical timestamps and channel selection when signal buffers wrap.
  Include finite error bounds in model ranges and prevent nonfinite values from
  invalidating automatic bounds. Reset the medical sweep position on clear.
- Notify circular-model listeners after releasing the writer lock so a listener
  can write again. Concurrent writers can deliver overlapping notifications;
  see the documented ownership and callback contract.
- Start the Spring Boot starter without optional Actuator classes and respect
  an application-provided server rendering service.
- Preserve spatial line continuity, default fill colors and dashed strokes
  across the Swing and server backends.
- Include forecast horizons and complete predicted candle wicks in automatic
  axis ranges, while preserving manual zoom and pan during demo animation.
- Make manually recorded Actuator statistics thread-safe and return only
  supported measurements. The endpoint reports `maxRenderTime` instead of
  mislabelling the maximum as a percentile. Fabricated fields are removed;
  unrecorded duration and memory values remain `null`, with an
  explicit measurement source. Consumers of its JSON should account for this
  response-contract correction.
- Report unavailable PNG encoders instead of returning empty byte arrays, and
  remove owned pool gauges when a server rendering service closes.

## Swing workbench

- Use a compact 36-logical-pixel title bar on supported macOS windows and an
  embedded menu bar on Windows and Linux, with renderer search and theme actions.
  Display the application title and remove the redundant catalog footer hint.
- Draw two-point Swing segments through the direct Java2D line primitive,
  avoiding the expensive general-polyline path for medical and other grids.
  Preserve clipping, stroke geometry and the fallback for large coordinates.
- Add functional side strips for the renderer catalog and Chart tools, also
  available through the View menu and Cmd/Ctrl+1 and Cmd/Ctrl+2. Chart tools shares
  Reload, PNG export and benchmark actions with the existing controls. PDF/SVG
  entries appear only when the corresponding optional backend is available;
  neither backend is bundled with the demo.
  Keep filter text, selection, chart instances and panel widths when toggling
  the side panels. When only one side fits, explicit opening takes precedence
  over focus in the previous panel. Passive resizing retains the focused side,
  or the most recently opened side if neither has focus; keep the other closed
  until explicitly reopened.
- Show the actual artifact version, runtime details and MIT notice in the
  content-sized About dialog. Remove the unimplemented Preferences action.
- Align navigation, toolbar, chart frame and status-bar spacing with UI scaling.
  Keep long renderer names visible with a separate category line. Move the chart
  actions below them when space is limited, including at a 1000-pixel window
  width and 150% UI scale, so either side panel remains accessible. The compact
  macOS title bar retains its 36-logical-pixel height.
- Restore the circular Sunburst demo by selecting the matching renderer and
  treating a hierarchy with a root as nonempty.
- Hide irrelevant Cartesian axes and grids in both Sunburst examples and the
  circular latency showcase. Restore the advanced Point-and-Figure price range
  and give the basic example discrete rising X and falling O columns. Activate
  the demo's calibration pulse.
  Show PhysicalScale and other overlays with the signal or gauge they describe.
- Run paint benchmarks on the Swing event dispatch thread, yield between frames
  and restore animation state when a run completes or is cancelled.
- Keep medical sweep samples at their fixed display positions across repeated
  buffer cycles, without overriding manual zoom, pan or calibration. Animate
  spirometry and VCG as complete curves, preserving their measurement domain.
- Use the active theme's series palette for EEG channels, preserving readable
  contrast when switching between light and dark appearances.
- Supply distinct synthetic data and units for all 17 medical examples. Show
  closed VCG and flow-volume loops, separate EEG rhythms, pressure and breathing
  traces, and frequency/depth coordinates with independent color intensity.
  Include every displayed channel in the initial viewport; fixed ECG scaling
  applies only to ECG examples.

## Packaging and repository cleanup

- Include the canonical MIT `LICENSE` and `NOTICE` in runtime, source and
  Javadoc JARs, and align their license metadata. Remove obsolete delivery
  restrictions and duplicate licensing documents.
- Preserve bundled third-party license and copyright files in separate
  `META-INF/third-party/` directories in shaded applications.
- Remove superseded SLA and pricing documents, old release instructions,
  obsolete migration/native-distribution scripts and generated repository output.
  Earlier versions remain available in Git history.
- Correct outdated convenience-API exception messages without changing which
  operations are implemented. The quick start documents working model, panel
  and server examples.
- Update the architecture, concurrency and logging documentation to describe
  the implemented contracts and their limits. Generate the Swing grid mapping
  from the actual selection rules and preserve all 156 catalog entries.
- Add repository consistency checks for current release metadata, local links,
  generated grid documentation and unwanted artifacts. Validate actual package
  contents, including project libraries nested in the executable Boot JAR.
- Replace the superseded dependency inventory with current version checks and
  primary sources. Update Logback Core and Classic together to 1.6.5; FlatLaf
  and Extras remain at the verified current stable version, 3.7.2.
- Activate Spring configuration metadata processing explicitly on Java 25.
  Generate the JMH benchmark index in the Swing bridge, where the benchmarks
  live, and remove unused Core benchmark dependencies and launcher code.
- Exercise every catalog entry through the real Swing selection path in both
  themes. Strengthen browser and backend test expectations with independent
  values and checks for the failure paths that earlier tests missed.
- Check circular-buffer snapshots against retained input values and propagate
  worker failures from concurrency tests. Require successful writes and reads
  so a test cannot pass because all worker threads stopped before doing work.
- Replay the opt-in Swing memory test on the EDT with complete, reversible
  gestures. Require real viewport changes and keep the data in view throughout
  the run; compare retained heap in bytes after warmup.

The dated rendering audit retains its original findings and evidence. Screenshot
provenance identifies the application version used for each set of captures.

## Upgrade and runtime

Use version `2.5.0` consistently for all ArberCharts modules:

- `com.arbergashi:arber-charts-parent`
- `com.arbergashi:arbercharts-core`
- `com.arbergashi:arbercharts-server-bridge`
- `com.arbergashi:arbercharts-spring-boot-starter`
- `com.arbergashi:arbercharts-swing-bridge`

The documented model, renderer and chart-host APIs remain available. Applications upgrading from 2.1.0 must
still apply the [module migration instructions](docs/RENDERING_ARCHITECTURE.md#migration-from-210)
introduced in 2.2.0, including the Swing bridge dependency for desktop helpers.

The runtime remains **Eclipse Temurin 25**. Spring Boot remains at 4.1.1;
see [Dependency updates](docs/DEPENDENCY_UPDATES.md) for the checked versions
and the Logback update.
The server raster backend still needs `java.desktop` in headless mode. This
release contains JVM artifacts, with no native-image execution claim.

### Upgrading from 2.2.0

The Java 25 requirement and module layout are unchanged. Update all ArberCharts
coordinates together and review the following observable corrections.

Applications consuming `/actuator/charts` must update their JSON mappings:

| Previous field or behavior | 2.5.0 contract |
| --- | --- |
| `p99RenderTime` | `maxRenderTime`: the maximum recorded duration, not a percentile. |
| Root `status` and `renderers` | Removed because they were fixed placeholders. |
| Performance `p50RenderTime`, `p95RenderTime`, `p999RenderTime`, `failedRenders`, `concurrentRenders` | Removed; these measurements were not collected. |
| Memory `averageBytes`, `allocationsPerRender` | Removed; these measurements were not collected. |
| `/actuator/charts/renderers` | Returns only `measurementSource: unavailable`. |
| Unrecorded durations and memory | `null` until the first sample; `totalRenders` starts at zero. |
| `recordRender(double, long)` with invalid input | Rejects negative durations or byte counts and nonfinite durations with `IllegalArgumentException`, without changing statistics. |

`measurementSource` changes from `unavailable` to `manual` after a valid sample.
Root responses contain formatted duration/size strings; section responses use
numeric milliseconds and bytes. The endpoint is not populated automatically by
the server service; the service's Micrometer meters remain separate measurements.

The public `ChartAutoConfiguration.chartActuatorEndpoint()` factory remains
available for direct callers. Obtain the managed `ChartActuatorEndpoint` bean
when using Spring. Declare a replacement endpoint with `@Bean` to customize it;
overriding the outer factory no longer replaces the conditional inner bean
factory. Registration still depends on Actuator availability, endpoint exposure
and the absence of a replacement bean.

Automatic ranges now include forecast output. A custom `PredictionModel` used
with `PredictiveShadowRenderer` can be called during range calculation as well
as painting. Range calculation supplies a 1-by-1 plot context covering the
source data and a separate output buffer; it does not replace the rendered
prediction or its audit history. Predictors that depend on viewport dimensions
or advance mutable state per call require application-specific checks of their
ranges and forecasts.

`DefaultPlotContext` applies logarithmic transforms in both directions.
Nonpositive or nonfinite logarithmic bounds or data coordinates map to `NaN` on the
affected axis; custom renderers should omit nonfinite mapped coordinates.
Corrected bounds, ticks, missing-data gaps and theme colors can change chart
images. Review image-snapshot differences against the intended geometry.

Applications producing native images should also review the
[native metadata changes](docs/GRAALVM_AUDIT.md). The JVM release does not claim
native-image compatibility.

## Downloads and verification

The GitHub release provides the Swing and Spring Boot demo JARs and ZIP bundles,
library artifacts with sources and English Javadoc, and SHA-256 checksums.
The demos are excluded from Maven Central; the five coordinates above form the
Central release. Previously published Central versions are immutable, so 2.5.0
is a new publication rather than an in-place change to 2.2.0.

Release verification includes a fresh reactor build, scalar-fallback tests,
independent regression expectations, actual demo selection paths, repository
checks and inspection of the rebuilt artifact contents. The
[verification strategy](docs/VERIFICATION_STRATEGY.md) describes functional and
backend coverage, while [release preparation](RELEASING.md) documents packaging
and publication checks. These checks do not replace the outstanding visual,
physical calibration, stress and workload-specific performance acceptance work.
