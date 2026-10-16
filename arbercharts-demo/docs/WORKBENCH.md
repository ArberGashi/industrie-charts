# Swing workbench

The Swing demo uses an IDE-style workbench: a compact application bar, a searchable
renderer catalog, and a chart workspace with its own actions. The visual hierarchy
is inspired by desktop development tools, including IntelliJ IDEA. The palette,
layout, and application identity are specific to Industrie Charts; no JetBrains
assets or theme files are included.

![Dark workbench](images/workbench-dark.png)

![Light workbench](images/workbench-light.png)

## Design system

The 48-pixel application bar contains the application identity, global renderer
search, and theme switch. A resizable 292-pixel catalog sits beside the workspace.
The active renderer and category remain visible above the chart, alongside Reload
and Export PNG. The status bar reports chart setup time and the actual artifact
version obtained from Maven metadata.

Inter is loaded from the existing bundled font dependency and used at 13 points
for controls. Auxiliary labels use 11–12 points. The chart remains the main visual
content; the surrounding surfaces use small control radii and simple separators.

| Role | Dark | Light |
| --- | --- | --- |
| Application chrome | `#242832` | `#EDF0F4` |
| Chart workspace | `#1D2129` | `#FFFFFF` |
| Catalog surface | `#252A34` | `#F4F6F8` |
| Primary text | `#DCE1E9` | `#283442` |
| Secondary text | `#A0AAB8` | `#647185` |
| Accent | `#80A9E7` | `#2866AE` |

Both FlatLaf theme files define the same component keys. Chart colors are kept in
`src/main/resources/themes/charts.properties` so the chart background and UI
surfaces agree in both appearances.

## Interaction

The demo opens `LineRenderer` immediately. Filter the catalog by renderer name,
class name, or category. Enter opens the first result; Escape clears the filter.
The global Find renderer action searches across the catalog and clears a
conflicting sidebar filter when opening a result.

| Shortcut | Action |
| --- | --- |
| Cmd/Ctrl+F | Find renderer |
| Cmd/Ctrl+R | Reload selected chart |
| Cmd/Ctrl+E | Export selected chart as PNG |
| Cmd/Ctrl+T | Switch dark/light appearance |
| Cmd/Ctrl+B | Benchmark selected chart |

Cached chart views retain their state when revisited. Export and benchmark actions
always target the selected view. Export is unavailable while that view is loading.
The existing File menu retains optional PDF and SVG exports.

Zoom responds to fractional trackpad input, accumulates rapid wheel gestures, and
settles smoothly from its current position. Dragging or resetting the view cancels
an older transition. Crosshair coordinates and labels move together immediately;
only their short opacity transition is animated.

Showcase signals advance from monotonic elapsed time while their card is visible.
Selecting another chart pauses the hidden signal; returning resumes its active
clock without a catch-up jump. Medical samples retain their 250 Hz simulation
cadence, and the financial demo updates one visible live bar while preserving a
fixed history size. A long EDT stall is capped at 250 ms of simulated catch-up.
Reload, theme replacement, and disposal release animation timers and listeners.

Use `-Darbercharts.reduceMotion=true` before `-jar` to start the workbench without
interaction or showcase animation. Embedding applications can call
`ArberChartPanel.setAnimationsEnabled(false)` on the EDT to change this setting at
runtime; re-enabling it resumes the visible demo signal.

![Compact light workbench at 1000 by 680 logical pixels](images/workbench-light-compact.png)

## Verification

The three workbench screenshots above were regenerated from the final shaded
`dist/releases/jvm/2.2.0/arbercharts-demo-2.2.0.jar` after the
quality-gate build, including the corrected initial multi-series autoscaling.
The captured JAR's SHA-256 is
`4235c83d04026e5911c07c150e278056e9ead9a99cfb9c83755e5fd4d1b088e8`.
They are direct 2x paints of the actual `DemoApplication.buildWorkspace()` component
hierarchy on the EDT, using the installed Eclipse Temurin 25 JDK, FlatLaf 3.7.2,
and JSVG 2.2.0. The headless harness waits for the initial chart, verifies that its
series fits the viewport, and disposes its demo resources after capture.

Reproduce these images from the repository root using the packaged JAR:

```bash
capture_dir="$(mktemp -d)"
demo_jar="dist/releases/jvm/2.2.0/arbercharts-demo-2.2.0.jar"
javac -cp "$demo_jar" -d "$capture_dir" docs/audits/2026-10-16-rendering/harness/WorkbenchScreenshots.java
java --add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED \
  -Djava.awt.headless=true -cp "$capture_dir:$demo_jar" \
  com.arbergashi.charts.demo.WorkbenchScreenshots arbercharts-demo/docs/images
```

The visual checks cover dark and light appearances, 1360 by 880 and 1000 by 680
logical pixels, and painting at 2x resolution. Interaction checks cover filtering,
empty results, selecting across filters, cached selection, reload, theme changes,
and PNG export from the active chart. All sixteen bundled SVG icons were loaded
and painted with JSVG 2.2.0. `DemoWorkspaceTest` retains regression coverage for
the default chart and export after returning to a cached view. Its export oracle
checks series-colored pixels at mapped data coordinates and rejects a solid
background. Animation regressions use a controlled monotonic clock on the EDT to
check cadence independence, rapid retargeting, cancellation, hidden-card resume,
reduced motion, and cleanup. The live financial bar is also compared in two actual
PNG exports. The four frames below were painted by the real Swing panel on the EDT
at controlled times, without opening a desktop window. They document geometry;
physical display cadence remains a separate desktop check.

![Controlled Swing zoom frames](../../docs/audits/2026-10-16-rendering/assets/swing-zoom-frames.png)

These checks run offscreen. Native title-bar gestures, file dialogs, and transitions
between physical monitors still need a desktop check. Existing axis, grid, legend,
and individual renderer presentation findings are tracked in the independent
rendering investigation; this workbench change does not alter those algorithms.

Run the automated demo tests with the existing local JDK:

```bash
mvn -pl arbercharts-demo -am test -Djava.awt.headless=true
```

Build and launch the desktop demo:

```bash
mvn -pl arbercharts-demo -am package -Djava.awt.headless=true
java -jar arbercharts-demo/target/arbercharts-demo-2.2.0.jar
```
