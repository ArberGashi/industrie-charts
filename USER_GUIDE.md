# ArberCharts User Guide (Core 1.3.0)

This guide covers the supported public API in `arbercharts-core`. It is written for developers integrating the framework into a Swing or headless Java application.

ArberCharts ships with **139 renderer families** across standard, financial, statistical,
specialized, medical, and analysis domains.

Related docs:
- `PUBLIC_API.md` for the stable API map
- `RENDERER_CATALOG.md` for renderer IDs and icon keys
- `QUALITY_REPORT.md` for baseline results
- `LICENSING.md` for commercial licensing terms
- `PRICING.md` for current price tiers

---

## 1) Installation (Maven)

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-core</artifactId>
    <version>1.3.0</version>
</dependency>
```

Optional UI integration:
- `com.formdev:flatlaf` (for UIManager theme defaults)
- `com.formdev:flatlaf-extras` (optional extras)

---

## 2) Quickstart (Fluent API)

```java
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

DefaultChartModel model = new DefaultChartModel("Temperature");
for (int i = 0; i < 100; i++) {
    model.addXY(i, Math.sin(i * 0.1) * 20 + 20);
}

ArberChartPanel panel = ArberChartBuilder.create()
    .withTitle("Sensor Readings")
    .withLegend(true)
    .withTooltips(true)
    .addLayer(model, new LineRenderer())
    .build();
```

---

## 3) Data Models

### 3.1 ChartModel contract
- Use `getPointCount()` as the logical size.
- Arrays returned by `getXData()` / `getYData()` may be larger than the logical size.

Recommended model for general use: `DefaultChartModel`.

### 3.2 High-frequency streams
For real-time medical data use:
- `FastMedicalModel`
- `CircularFastMedicalModel` (sweep-erase monitors)
- `CircularChartModel` for lock-free streams outside the medical presets

---

## 4) Renderers

Renderers are chart types. The framework ships with 139 renderer families. Most extend `BaseRenderer` and can be mixed as layers.

Renderer IDs and icon keys live in `RENDERER_CATALOG.md`.
If you need dynamic creation, use:

```java
RendererRegistry.createOptional("line");
RendererRegistry.require("candlestick");
```

---

## 5) Themes and Styling

Use `ChartTheme` to define colors and fonts. Defaults are available via:
- `ChartThemes.defaultDark()`
- `ChartThemes.defaultLight()`

Apply themes with:
```java
panel.withTheme(ChartThemes.defaultLight());
```

Theme transitions and alpha variants are backed by the Zero-GC color registry for
stable performance during live theme switching.

---

## 6) Locale and i18n

### 6.1 Axis and tooltip formatting
```java
panel.withLocale(Locale.US);
```

### 6.2 I18N fallback and default locale
```java
ChartI18N.setDefaultLocale(Locale.US);
String label = ChartI18N.getString("chart.title");
```

---

## 7) Legends and Crosshair

Legends are on by default and can be configured:
```java
panel.withLegend(true)
     .withLegendConfig(LegendConfig.overlay(LegendPosition.TOP_LEFT));
```

Crosshair is provided by `HighPrecisionCrosshair` and is managed by `ArberChartPanel`.

---

## 8) Grid Layers

Pick a grid layer suited to your domain:
- `DefaultGridLayer` for general charts
- `MedicalGridLayer` for clinical waveforms
- `FinancialGridLayer` for OHLC/candlestick
- `AnalysisGridLayer` for analysis/FFT

```java
panel.withGridLayer(new FinancialGridLayer());
```

---

## 9) Exporting Charts

Core provides export without UI dialogs:
```java
ChartExportService.exportPng(panel, new File("chart.png"));
ChartExportService.exportSvg(panel, new File("chart.svg")); // requires jfreesvg
ChartExportService.exportPdf(panel, new File("chart.pdf")); // requires pdfbox
```

Use `ChartExportHandler` if your app needs custom workflows.

---

## 10) Performance Notes

- Renderers follow a zero-allocation contract in `drawData`.
- Use primitive arrays for throughput.
- Avoid allocating objects inside render loops.
- For performance baselines, see `QUALITY_REPORT.md`.

---

## 11) What Is Not Public API

Do not depend on:
- `com.arbergashi.charts.internal.*`
- `com.arbergashi.charts.tools.*`
- Anything in `arbercharts-demo`

Use `PUBLIC_API.md` for the definitive list.

---

## 12) Demo Application (Optional)

Build and run the demo app:
```bash
mvn -pl arbercharts-demo package
java --enable-native-access=ALL-UNNAMED -jar arbercharts-demo/target/arbercharts-demo-1.3.0.jar
```

---

## 13) Packaged Demo (Java 25)

To ship the demo with a bundled Java 25 runtime, use the packaging scripts:
- `docs/PACKAGING.md` for requirements and commands
- Output artifacts live under `dist/` per OS

---

## 14) API Documentation

Generated Javadocs are stored in:
- `docs/javadoc/index.html`

---

## 15) Licensing Overview

ArberCharts is proprietary software with:
- A free, unlimited Developer License for evaluation and development
- A paid Distribution/Runtime License for end-customer delivery

See `LICENSING.md` and `PRICING.md` for details.
