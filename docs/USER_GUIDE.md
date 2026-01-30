# ArberCharts User Guide (Core 1.7.0-LTS)

This guide covers the supported public API in `arbercharts-core` and the official bridges.
It is written for developers integrating the framework into Swing, Compose Desktop, or
headless/server deployments.

Related docs:
- `EXECUTIVE_SUMMARY.md`
- `PERFORMANCE_REPORT.md`
- `RENDERER_CATALOG.md`
- `COMPLIANCE.md`
- `DOCTRINE_POLICY.md`
- `MIGRATION_GUIDE_v1.7.md`
- `LICENSING.md`
- `PRICING.md`

---

## 1) Installation (Maven)

Core:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-core</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

Swing bridge:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

Server bridge (headless rendering):

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-server-bridge</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

Spring Boot starter:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

Compose Desktop bridge (Kotlin):

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-compose-bridge</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

Legacy note: `arbercharts-starter` is a 1.3.0-era convenience bundle and is not recommended
for 1.7.0-LTS deployments.

---

## 2) Big Picture Flow (Model -> Context -> Engine -> Canvas)

```text
ChartModel  ->  PlotContext  ->  ChartRenderer  ->  ArberCanvas  ->  Bridge Target
  (data)        (scale/map)      (draw logic)      (commands)     (Swing/Compose/Server)
```

Key points:
- `ChartModel` supplies zero-allocation primitives (arrays and counters).
- `PlotContext` defines the data range, pixel bounds, and coordinate transforms.
- `ChartRenderer` draws using the headless `ArberCanvas` abstraction.
- A bridge (Swing, Compose, Server) provides the concrete canvas implementation.

---

## 3) Quickstart (Swing)

```java
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.platform.swing.ArberChartPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

SwingUtilities.invokeLater(() -> {
    DefaultChartModel model = new DefaultChartModel("Temperature");
    for (int i = 0; i < 100; i++) {
        model.setXY(i, Math.sin(i * 0.1) * 20 + 20);
    }

    ArberChartPanel panel = ArberChartBuilder.of()
        .setTitle("Sensor Readings")
        .setTooltips(true)
        .setLegend(true)
        .setLayer(model, new LineRenderer())
        .build();

    JFrame frame = new JFrame("ArberCharts");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(panel);
    frame.setSize(800, 500);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
});
```

Note: `ArberChartBuilder.build()` must run on the EDT.

---

## 4) Data Models

### 4.1 ChartModel contract
- Use `getPointCount()` as the logical size.
- Arrays returned by `getXData()` / `getYData()` may be larger than the logical size.

### 4.2 High-frequency streams
For real-time data:
- `CircularChartModel` for lock-free streaming.
- `FastMedicalModel` and `CircularFastMedicalModel` for medical waveforms.

---

## 5) PlotContext and Scaling

`PlotContext` is the authoritative mapping between data space and pixel space. For regulated
use cases, scaling formulas and calibration policy are defined in `COMPLIANCE.md`.

Use `ChartRenderHints` for per-pass tuning (anti-aliasing, stroke width). Axis formatting
and scale rules are configured via `AxisConfig`.

---

## 6) Themes and Styling

Themes are per-panel and must not be treated as global state. Default fallbacks are:
- `ChartThemes.getDarkTheme()`
- `ChartThemes.getLightTheme()`

If you must use `ChartAssets`, treat it as boot-time configuration only. Runtime changes
should flow through `PlotContext` and `ChartTheme`. See `DOCTRINE_POLICY.md`.

---

## 7) Spatial Pipeline Integration

Spatial-capable renderers implement `SpatialChunkRenderer`. Coordinate transforms are resolved
via `SpatialTransformRegistry`. For custom transforms, implement `CoordinateTransformProvider`.

This enables spatial batching, depth policies, and projection-aware rendering without coupling
to any UI framework.

---

## 8) Renderer Registry

Use the public registry facade:

```java
import com.arbergashi.charts.platform.render.RendererRegistry;

RendererRegistry.getOptionalRenderer("line");
RendererRegistry.getRequiredRenderer("candlestick");
RendererRegistry.metadata();
```

Renderer capability flags are available via:
`RendererRegistry.getRendererCapabilities(id)`.

---

## 9) Legends, Crosshair, and Interaction

Legend placement is configured with `LegendConfig` (overlay or docked). Crosshair and tooltips
are managed by the Swing bridge (`ArberChartPanel`) and can be toggled per panel.

---

## 10) Exporting Charts

Exports are handled by the Swing bridge and are safe in headless environments:

```java
import com.arbergashi.charts.platform.export.ChartExportService;

ChartExportService.exportPng(panel, new File("chart.png"));
ChartExportService.exportSvg(panel, new File("chart.svg")); // requires jfreesvg
ChartExportService.exportPdf(panel, new File("chart.pdf")); // requires pdfbox
```

---

## 11) Forensic Playback and Integrity

Deterministic playback and watchdog monitoring are part of the forensic toolchain.
See `COMPLIANCE.md` for operational constraints and audit policies.

---

## 12) What Is Not Public API

Do not depend on:
- `com.arbergashi.charts.internal.*`
- `com.arbergashi.charts.tools.*`
- Any demo or verification modules

---

## 13) AI-Assisted Engineering Note

ArberCharts may use AI assistance for documentation and refactoring, but final changes are
reviewed by maintainers and validated by tests and doctrine checks. There are no AI runtime
dependencies in the core rendering pipeline.

---

## 14) API Documentation

Generated Javadocs are stored under:
- `site/javadoc/index.html`
