# UI presentation integration audit

Date: 2026-10-16. Source baseline: the `2.2.0-SNAPSHOT` checkout inspected on the audit date. Executable evidence uses the confirmed baseline build at `dist/releases/jvm/arbercharts-demo-2.2.0-SNAPSHOT.jar`; its UI/renderer implementation matches the inspected sources. Concurrently changed backend primitives and documentation are excluded from that equivalence claim. This is an independent investigation for a later presentation-quality workstream. Production sources and renderer implementations were not changed by this investigation.

## Scope and evidence boundary

The inspection follows the production Swing presentation path from `ArberChartBuilder` and `SwingChartDisplayProvider` through `ArberChartPanel`, its child components, the spatial consumer, and the export service. The [separate renderer audit](renderer-report.md) covers individual renderer geometry and grid implementations. The concurrent backend cleanup owns `AwtCanvasAdapter`, `ImageBufferCanvas`, resource lifecycle, and primitive contract tests; defects already being fixed there are not repeated as open findings here.

The active component inventory and inspection coverage are:

| Area | Inspected production components and integration | Verification boundary |
| --- | --- | --- |
| Chart composition | `ArberChartPanel`, builder, primary/pre-data/overlay passes, clip handling, viewport cache, empty-state path, normal and spatial dispatch | Source-level integration inspection; bounded headless reproduction below. No exhaustive animation or concurrency soak. |
| Axes | `AxisConfig`, `AxisScale`, `NiceScale`, `DefaultPlotContext`, font/anchor placement, inversion, formatting, requested tick counts, primary/secondary configuration | Linear Cartesian integration examined. Nonlinear configuration is checked for actual propagation. |
| Grid integration | Panel visibility gate and demo grid selection: Default, Medical, Financial, Statistical, Analysis, Isometric, SmithChart, Polar, Ternary | Selection and contract checked here. Detailed geometry belongs to the renderer audit. Geo and Logarithmic are public implementations but are not selected by the current demo switch. |
| Overlay UI | `HighPrecisionCrosshair`, `TooltipOverlay`, `InteractiveLegendOverlay`, `DockedLegendPanel`, `LegendChartContext`, `LayerVisibilityModel`, freeze measurement label | Construction, z-order, event routing, snap/label flow, theme changes, and dock layout inspected. |
| Spatial presentation | `SwingSpatialBatchConsumer`, panel spatial setup, style/batch dispatch, fill/flush order | Source inspected. Complete 3D projections, solid-fill fidelity, and device-scale behavior need dedicated renderer/backend images. |
| Scale and typography | `ChartScale`, `PhysicalScaleResolver`, `ChartFonts`, `SwingRenderHints`, `SwingAssets`, `ChartEngine` | Headless logical-pixel behavior can be reproduced. Physical monitor dimensions, mixed-DPI monitor moves, fractional OS scale, and actual font availability cannot be certified by this inspection. |
| Export | `ChartExportService` PNG/SVG/PDF geometry and `printAll` path, `ChartExportHandler`, direct panel export | PNG can be reproduced headlessly. Optional PDFBox/JFreeSVG output was source-inspected, not visually certified. |
| Application usage | Demo chart construction, category axis presets, showcase grid/preset selection, medical unit conversion, public display provider | Chart presentation path inspected. All surrounding gallery/search/dialog widgets were not visually audited. |
| Public helpers outside the active path | `HighDpiRenderer`, `AccessibilitySupport` | Invocation search found no production calls from panel/demo. Their existence is not evidence of working per-monitor adaptation or keyboard chart navigation. The accessibility helper explicitly contains unimplemented key actions. |

No statement in this report means that every renderer, every theme, or every display configuration has passed visual acceptance. The findings below are concrete contract failures, not a general claim that all remaining appearances are correct.

## Prioritized findings

### UI-01 — P1: Medical scale presets use pixels per unit as if they were millimeters per unit

Trigger: configure `setXAxisConfig(new AxisConfig().setMedicalScale(25))` and `setYAxisConfig(new AxisConfig().setMedicalScale(10))`, including the equivalent `AxisScale.ofMedical(25, 10)` API.

`AxisConfig.setMedicalScale` stores `1 / mmPerUnit` as a fallback and provides `getResolvedUnitsPerPixel(pixelsPerMillimeter)` for the physical conversion. The panel only reads `getUnitsPerPixel()` when computing its viewport. It measures physical density after it has already built the context, and never applies that density to the axes. At a 96-DPI logical density, a one-second interval therefore occupies 25 logical pixels instead of about 94.49; a 1 mV interval occupies 10 instead of about 37.80. Resize may preserve this wrong pixel scale, so resize stability alone would not detect the fault.

Evidence: [ArberChartPanel.java:1489](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1489), [:1504](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1504), and [:890](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L890); [AxisConfig.java:286](../../../arbercharts-core/src/main/java/com/arbergashi/charts/api/AxisConfig.java#L286) and [:308](../../../arbercharts-core/src/main/java/com/arbergashi/charts/api/AxisConfig.java#L308).

The demo's medical preset manually divides by density at [DemoApplication.java:1935](../../../arbercharts-demo/src/main/java/com/arbergashi/charts/demo/DemoApplication.java#L1935), which is a separate path and can hide the public API defect. A future fix should make physical density an explicit input to context construction, distinguish logical from device pixels, and test calibrated displacement across resize before claiming actual monitor calibration.

### UI-02 — P2: Docked legends and chart content use different coordinate spaces

Trigger: `setDockedLegend(LEFT|RIGHT|TOP|BOTTOM)` on a displayed chart.

BorderLayout reserves space for the docked legend and moves/shrinks `overlayCanvas`. The chart is painted on the outer panel, while `getCalculatedPlotBounds()` uses the outer panel's full width/height. Crosshair bounds and mouse coordinates use the inner canvas. On a right dock, data continues behind the legend; on left/top docks, chart pixels and overlay hit positions are offset by the dock size. This affects snapping, tooltips, zoom focus, and freeze measurement as well as visual overlap.

Evidence: [ArberChartPanel.java:514](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L514), [:822](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L822), [:846](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L846), [:1108](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1108), and [:1520](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1520).

A future fix should establish one plot surface and coordinate convention for data painting, axes, children, and events, rather than applying independent offsets to each overlay. Existing dock tests only establish that components can be created/switched and that size policies are returned; they do not assert plot containment or pixel-to-data alignment.

### UI-03 — P2: Exporting an unrealized panel can produce a valid but empty image

Trigger: create a panel, call `setPreferredSize(500, 400)`, leave actual size at zero, and call `ChartExportService.exportPng`.

`ExportGeometry.from` uses preferred size to allocate a correctly sized image, but the actual component remains zero-sized and is never laid out. The subsequent `panel.printAll(g2)` therefore has no component area to paint. The export file may exist and have nonzero length while containing no chart. The same common geometry/printing path is used by optional SVG/PDF backends.

Evidence: [ChartExportService.java:56](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/export/ChartExportService.java#L56), [:66](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/export/ChartExportService.java#L66), and [:238](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/export/ChartExportService.java#L238).

`ChartExportServiceTest.testExportPngWithPreferredSize` checks existence and byte length, which cannot distinguish a blank transparent PNG from a real chart. A future regression test should inspect non-background content and child layout, with temporary sizing/restoration confined to the EDT.

### UI-04 — P2: Axis labels do not share a bounded, precision-aware layout with their grid

There are several separately reproducible issues in this pipeline:

- Default number formatting is capped at two fraction digits. With a range of `0..0.004`, all five generated tick labels become `0` (with six ticks requested), so the data remains visually distinguishable while the numeric axis does not. Precision must follow tick spacing or an explicit formatter.
- `NiceScale` deliberately rounds bounds outward, but `drawAxisLabels` draws all returned ticks without filtering to the visible range. For a viewport `0.1..1.9`, ticks `0` and `2` are drawn outside the plot; the right label can fall outside the component and disappear. The default grid does filter mapped line coordinates to the plot, so labels and visible grid lines diverge.
- Plot margins are fixed (`48` logical pixels left, `24` right) rather than measured against formatted labels. Large numbers or unit suffixes extend left of the component. No collision reduction, rotation, or label skipping is applied to dense requested ticks.
- `MIDDLE_LEFT` is treated as horizontally centered in `drawAnchoredString`, shifting explicitly left-anchored labels by half their width.
- X/Y grid flags are ORed into one global gate. Disabling only the X grid while leaving Y enabled leaves the default grid image unchanged. Major grid tick counts are computed separately from panel axis ticks, so requested axis counts do not define matching major grid positions.

Evidence: [AxisConfig.java:83](../../../arbercharts-core/src/main/java/com/arbergashi/charts/api/AxisConfig.java#L83); [ArberChartPanel.java:894](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L894), [:1049](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1049), [:1066](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1066), [:1088](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1088), and [:1520](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1520); [DefaultGridLayer.java:107](../../../arbercharts-core/src/main/java/com/arbergashi/charts/render/grid/DefaultGridLayer.java#L107).

The next workstream should use one explicit tick/layout result for axis labels and major grids, with precision, visible-range filtering, text metrics, margins, and axis-specific visibility tested separately. This is a host presentation contract and does not require rewriting data renderers.

### UI-05 — P2: The default overlay legend is not wired to series interactions

Trigger: click a series name in the default overlay legend.

The panel installs an action listener only on `DockedLegendPanel`. The main `InteractiveLegendOverlay` gets a visibility resolver but no action listener, and its click handler exits when `actions == null`. In addition, the full-size visible crosshair is above the overlay legend and has no hit-test passthrough, so normal pointer events can be delivered to the crosshair instead of a legend row. Directly dispatching a click to the legend still cannot toggle anything because the listener is missing.

Evidence: [ArberChartPanel.java:422](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L422), [:429](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L429), and [:450](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L450); [InteractiveLegendOverlay.java:89](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/render/legend/InteractiveLegendOverlay.java#L89).

There is a separate identity limitation: rows and visibility are keyed by `BaseRenderer.getId()`, e.g. every `LineRenderer` has `line`. The demo builds three independent standard series with three renderer instances of the same type. Their rows share the same label/ID, and visibility toggling by that ID affects all of them. `LegendChartContext` supplies only the first model for all renderers, so per-series names/metadata are not represented by the host contract. Live focus values are also never supplied by the panel to either legend. A future change needs a host-level series identity/model association independent of renderer type.

Evidence: [ArberChartPanel.java:403](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L403), [:938](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L938); [InteractiveLegendOverlay.java:460](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/render/legend/InteractiveLegendOverlay.java#L460); [BaseRenderer.java:67](../../../arbercharts-core/src/main/java/com/arbergashi/charts/render/BaseRenderer.java#L67); [DemoApplication.java:1215](../../../arbercharts-demo/src/main/java/com/arbergashi/charts/demo/DemoApplication.java#L1215).

### UI-06 — P2: Changing theme with a docked legend re-enables the overlay legend

Trigger: enable a docked legend, then call `setTheme` or switch Look and Feel.

`applyLegendPlacement` hides the overlay in docked mode. `applyThemeToOverlays`, invoked by theme/LAF changes, sets the overlay visible using only `legendVisible` and ignores placement. The docked legend stays visible, so a second legend appears inside the chart.

Evidence: [ArberChartPanel.java:533](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L533), especially [:542](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L542) and [:546](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L546).

The existing theme benchmark does not assert mutual exclusion of legend placements. A future test should cover all placements after theme and LAF updates.

### UI-07 — P2: Public coordinate-transform and secondary-axis configuration do not reach rendering

Trigger: use `ArberChartBuilder.setCoordinateTransform`, `setSmithChartMode`, or `setLayerSecondary` with `setYAxisSecondary`.

The panel stores `coordinateTransformer` and `yAxisSecondaryConfig` but never reads them in context creation or rendering. All layers receive the same linear `DefaultPlotContext`; layer `AxisRole` is not consulted. A secondary layer configured for `-1000..1000` can therefore be rendered against a primary `-1..1` scale, and a custom transform is never invoked. This is an integration defect; it does not imply that renderer classes implementing their own special projection are defective.

Evidence: [ArberChartPanel.java:311](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L311), [:326](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L326), [:961](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L961), and [:1415](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1415); [ArberChartBuilder.java:219](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/api/ArberChartBuilder.java#L219), [:224](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/api/ArberChartBuilder.java#L224), [:324](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/api/ArberChartBuilder.java#L324), and [:398](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/api/ArberChartBuilder.java#L398).

A future host context needs to preserve inverse transforms for hit testing as well as forward transforms and per-axis configuration. Setters/builders returning successfully are insufficient validation.

### UI-08 — P2: Wheel focus and drag direction assume a non-inverted X axis

Trigger: enable `AxisConfig.setInverted(true)` and zoom near one edge or drag horizontally.

The context maps inverted coordinates, but mouse wheel focus is calculated as `minX + range * relativeX`, and drag uses the same signed delta as the ordinary axis. Consequently, the data value under the pointer moves during wheel zoom and drag movement reverses relative to the rendered data.

Evidence: [ArberChartPanel.java:630](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L630), [:657](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L657), and [:660](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L660).

`UiInteractionTest` verifies changing range for a normal axis, not preservation of the data value under the pointer or inverted axes. Future interaction tests should use the context's inverse mapping for their oracle.

### UI-09 — P2: Freeze measurement text is calculated but never attached to the component tree

Trigger: enable freeze mode and select two measurement points.

`handleFreezeMeasurementClick` updates `freezeMeasurementLabel` text and visibility, but the label is never added to the panel or overlay canvas and never assigned bounds. The BPM measurement and alarm marker can exist in the model-facing getter without appearing in the UI or exports.

Evidence: [ArberChartPanel.java:177](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L177), [:766](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L766), and [:1810](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1810).

`FreezeMeasurementTest` checks the label text for `!`, not attachment, dimensions, or painted output. No claim about historical freeze/playback fidelity is made by this finding; that needs a separate capture/playback integration test.

### UI-10 — P2: Panel locale does not change axis/crosshair/tooltip number formatting

Trigger: create the panel under one JVM default locale, then call `panel.setLocale(otherLocale)` or the equivalent builder setter.

The panel stores the new locale and updates the AWT component, but its non-null X/Y `AxisConfig` instances retain their original locale. Axis painting and fallback crosshair/tooltip formatting prefer those configs, bypassing the panel locale. Thus `setLocale(Locale.GERMANY)` can still produce `1.5` instead of `1,5`.

Evidence: [ArberChartPanel.java:1603](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1603), [:1052](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1052), and [:1931](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/platform/swing/ArberChartPanel.java#L1931); [ArberChartBuilder.java:312](../../../arbercharts-swing-bridge/src/main/java/com/arbergashi/charts/api/ArberChartBuilder.java#L312).

A future fix should specify whether an explicitly configured axis locale overrides the panel default. Tests should assert drawn/formatted values, not only `panel.getLocale()`.

## Verification and next acceptance gates

The existing tests inspected include `ChartPanelPaintSmokeTest`, `ChartUiExtendedTest`, `UiInteractionTest`, `TooltipExceptionTest`, `PreDataLayerBandTest`, `FreezeMeasurementTest`, `ChartExportServiceTest`, `LegendPositionTest`, `DockedLegendLayoutSmokeTest`, `DockedLegendSizePolicyTest`, `LegendVisibilityToggleTest`, `LegendSoloModeTest`, and `AxisConfigTest`. These provide useful execution, arithmetic, and state checks. They do not currently establish full presentation fidelity: dock hit coordinates, visible BPM text, nonblank preferred-size exports, matching axis/grid ticks, or per-series interactive legend identity.

Future acceptance should include: (1) deterministic headless chart images and geometry assertions at representative sizes, (2) event-level tests for ordinary/inverted/docked charts, (3) medical displacement/calibration tests with explicit logical/device density, (4) per-series legend interactions with repeated renderer types, (5) PNG plus installed optional SVG/PDF output with chart children, and (6) manual inspection on real 1x/fractional/2x displays and monitor transitions. Golden image comparisons should tolerate known text rasterizer differences while preserving geometry and semantic label assertions.

The separate backend primitive contract tests are necessary but cannot certify these complete-chart host behaviors. Existing renderers can remain unchanged while host layout, axes, event coordinates, and series metadata are corrected.

## Headless reproduction results

The retained [UI harness](harness/InspectUi.java) ran on the EDT with `java.awt.headless=true`, Java 25, and `jdk.incubator.vector`. It compiled against the baseline shaded demo JAR, without reading concurrently rebuilt `target/classes` directories. Production sources were not patched and no Maven build was launched by this audit. This was an executed behavioral investigation, not a replacement for repository tests.

| Probe | Observed result |
| --- | --- |
| Medical presets, 800×400 panel, density fallback `96 / 25.4` | Density `3.77952756 px/mm`; X `25 px/unit`, expected `94.48818898`; Y `10 px/unit`, expected `37.79527559`. |
| Left dock | Canvas `(244,0,556,400)`; plot remains `(48,32,728,332)` in the outer panel. |
| Right dock | Canvas `(0,0,542,400)`; plot still reaches X `776`. |
| Top dock | Canvas `(0,126,800,274)`; plot remains at Y `32` with height `332`. |
| Bottom dock | Canvas `(0,0,800,274)`; plot still reaches Y `364`. |
| Dock then theme switch | Overlay legend visibility changes `false → true` while docked mode remains configured. |
| Preferred-size PNG | Image `500×400`; actual panel `0×0`; nontransparent pixels `0`. |
| Small tick range | Values `[0.0, 0.001, 0.002, 0.003, 0.004]`; labels `[0, 0, 0, 0, 0]`. |
| X-grid disabled, Y-grid still enabled | Exact rendered pixel difference from both enabled: `0`. |
| Overlay legend click | `actions == null`; deepest component at a legend name is `HighPrecisionCrosshair`; directly dispatching click to the legend leaves visibility `true → true`. |
| Freeze measurement | Text `BPM: 99.3`; label parent `null`; bounds `(0,0,0,0)`. |
| Custom coordinate transform | `0` transform calls during chart paint and context mapping; ordinary Cartesian coordinates returned. |
| Secondary Y layer | Receives context `-1.0..1.0`, despite secondary configuration `-1000.0..1000.0`. |
| Inverted wheel zoom | Data value under fixed pixel X=130 changes `1.852197802 → 1.697252747`. |
| Locale change | Panel reports `de_DE`; X axis still reports `en_US`; formatted 1.5 remains `1.5`. |
| Outward-rounded ticks | For X range `0.1..1.9`, tick 0 maps to X `7.556`, tick 2 to X `816.444`; plot X range is `48..776` in an 800-pixel panel. |

The [retained axis-range image](assets/axis-bounds-baseline.png) was also opened and visually inspected: the `2` Y label appears above the plot frame, the `0` X label lies to the left of the plot, and the `2` X label falls outside the image. Other probes above use numeric/component-state or pixel-difference evidence; no claim of manual visual inspection is made for them.

Source-only sub-findings not separately exercised by that harness are long-label margin overflow, the `MIDDLE_LEFT` horizontal-anchor case, duplicate renderer-type legend IDs, unsupplied live legend focus, and inverted drag direction. Their stated execution paths are explicit in the cited code, but they should each receive a focused regression test before changes are merged.


The [baseline console output](results/ui-baseline.txt) is retained with this report. Use the [reproduction instructions](README.md#reproduce-the-probes) to run against the current built demo artifact; later fixes are expected to change individual outputs.
