# ArberCharts Compliance Matrix (V1.0)

This document records the verified system specifications for ArberCharts v1.7.0 LTS and maps them to validation evidence. It is intended for technical audits, procurement reviews, and regulated deployments.

## 1) Core Performance

| Parameter | Specification | Status | Evidence |
| --- | --- | --- | --- |
| Throughput | Up to 1,000,000 points per chart instance | Verified | PerformanceBaselineReportTest |
| Latency | < 16 ms render time (60 FPS target) | Verified | PerformanceBaselineReportTest |
| Memory stability | < 1 MB drift after 15 min stress (post‑warmup) | Verified | MemoryStabilityTest (warm baseline 12 MB → 13 MB) |
| GC behavior | Zero Full GC during active interaction | Verified | GC log shows only explicit System.gc() at start/end |

## 2) Integration

### Java Swing
- Component: `ArberChartPanel` extends `JPanel`.
- Threading: UI updates on EDT.
- Repaint model: region‑limited invalidation for tooltip, crosshair, zoom, and pan.
- Look‑and‑Feel: FlatLaf, Nimbus, Metal; live refresh via `SwingThemeListener`.

### Spring Boot / Properties
- Core is dependency‑free.
- Integration provided by `arbercharts-ext-properties`.
- Key namespace: `arbercharts.theme.*` → `Chart.*` via adapter.

## 3) Security & Forensics

- Audit trail: bounded ring buffer (default 10,000 events) with compaction.
- Export sealing: PNG iTXt and PDF metadata include audit JSON.
- Integrity indicator: watermark overlay when audit logger active.
- Export transparency: `audit_truncated` flag included when the trail rotates.

## 3.1) AI-Assisted Engineering Governance

ArberCharts may use AI assistance for documentation and code modernization. The following
governance rules are mandatory:

- Human review is required for all AI-assisted changes before release.
- Architecture Doctrine checks and unit tests remain the source of validation.
- No AI runtime services are embedded into the core rendering pipeline.
- Traceability is maintained via code review and release notes.

## 4) Visual Theming

- Standard palette: Obsidian (#0c0d0e), Spring Green (#6db33f), neutral text (#f4f5f7).
- AA policy: UI elements anti‑aliased; data layer AA toggleable.
- Numeric alignment: monospace font binding via `Chart.font.mono`.

## 5) Medical & Scientific Baselines

- Medical grid defaults: X minor 0.04, X major 0.2, Y minor 0.1, Y major 0.5.
- Scaling: fixed units‑per‑pixel supported; DPI policy centralized in `ChartScale`.

## 6) Scaling & Medical Calibration (IEC 60601-2-25)

This section defines the exact scaling physics for regulated use cases. Custom renderers must adhere
to these formulas. Deviations can invalidate medical certification in regulated environments.

### 6.1 Logical Transformation (PlotContext)

The linear mapping from data space to pixel space is:

```text
x_pixel = (x_data - x_min) * (width_canvas / (x_max - x_min))
y_pixel = height_canvas - ((y_data - y_min) * (height_canvas / (y_max - y_min)))
```

Note: The Y axis is inverted in the default plot context to map the cartesian origin (bottom-left)
to the screen origin (top-left).

### 6.2 Medical Calibration (PhysicalScaleProfiles)

For ISO-conform EKG (e.g., 25 mm/s and 10 mm/mV), the pixels-per-millimeter is:

```text
PX_mm = DPI / 25.4
```

Horizontal (time) scaling:

```text
x_offset = T_seconds * Velocity_mm_per_s * PX_mm
```

Vertical (amplitude) scaling:

```text
y_offset = A_mV * Gain_mm_per_mV * PX_mm
```

Implementation references:
- `com.arbergashi.charts.util.ChartScale`
- `com.arbergashi.charts.util.PhysicalScaleProfiles`

### 6.3 "First Call Wins" Policy (DPI Determinism)

To prevent scaling jitter across multi-monitor setups, the scaling factor is frozen on first use:

```text
Scale_final = Scale_current  if Scale_stored is null
Scale_final = Scale_stored   otherwise
```

Custom renderers must treat this as a deterministic global policy.

## 7) Predictive Analytics Baseline

- Render order: grid → predictive shadow (pre-data band) → live data.
- Predictor: harmonic oscillator enabled by default for predictive shadow.
- Verified defaults:
  - `settleWindow = 32`
  - `periodAlpha = 0.80`
  - `resetThreshold = 0.25`
  - shadow alpha `= 0.26`
- Evidence: `PredictiveShadowStressTest`
  - clean RMSE ≈ 0.03
  - noisy RMSE ≈ 0.15 (15% noise injection)

## 8) Competitive Positioning Matrix

| Feature Front | ArberCharts (v1.7.0 LTS) | Market Baseline (typical) | Status |
| --- | --- | --- | --- |
| Grid architecture | Multi-geometry (Smith, Geo, Isometric, Polar, Ternary, Log) | Mostly linear/log only | Superior |
| DPI integrity | Central `snapPixel` + dynamic DPI scale per paint | Static scaling, inconsistent crispness | Superior |
| Memory footprint | Zero-GC render-path discipline | Object churn under interaction | Dominant |
| Integration ergonomics | Fluent API (`.asSmithChart()` etc.) | Larger JSON/config surfaces | Superior |
| Real-time stability | 60 FPS target with million-point scenarios validated | Noticeable degradation at high volumes | Dominant |

## 9) Demo Grid Registry (Renderer → Grid)

The demo panels map each renderer panel to a grid layer. This registry is used for audit and UI validation.

```json
{
  "Line Chart": "DefaultGridLayer",
  "Bar Chart": "DefaultGridLayer",
  "Stacked Bar": "DefaultGridLayer",
  "Grouped Bar": "DefaultGridLayer",
  "Area Chart": "DefaultGridLayer",
  "Step Area": "DefaultGridLayer",
  "Baseline Area": "DefaultGridLayer",
  "Range Area": "DefaultGridLayer",
  "Pie Chart": "DefaultGridLayer",
  "Donut Chart": "DefaultGridLayer",
  "Semi Donut": "DefaultGridLayer",
  "Polar Chart": "PolarGridLayer",
  "Polar Line": "PolarGridLayer",
  "Radar Chart": "PolarGridLayer",
  "Nightingale Rose": "PolarGridLayer",
  "Radial Bar": "DefaultGridLayer",
  "Radial Stacked": "DefaultGridLayer",
  "Gauge": "DefaultGridLayer",
  "Gauge Bands": "DefaultGridLayer",
  "Scatter Plot": "DefaultGridLayer",
  "Bubble Chart": "DefaultGridLayer",
  "Candlestick": "FinancialGridLayer",
  "Hollow Candlestick": "FinancialGridLayer",
  "High Low": "FinancialGridLayer",
  "Heikin Ashi": "FinancialGridLayer",
  "Renko": "FinancialGridLayer",
  "Waterfall": "FinancialGridLayer",
  "Kagi": "FinancialGridLayer",
  "Point & Figure": "FinancialGridLayer",
  "Volume": "FinancialGridLayer",
  "MACD": "FinancialGridLayer",
  "Stochastic": "FinancialGridLayer",
  "ADX": "FinancialGridLayer",
  "ATR": "FinancialGridLayer",
  "Bollinger Bands": "FinancialGridLayer",
  "Parabolic SAR": "FinancialGridLayer",
  "Ichimoku": "FinancialGridLayer",
  "Fibonacci": "FinancialGridLayer",
  "Pivot Points": "FinancialGridLayer",
  "OBV": "FinancialGridLayer",
  "ECG": "MedicalGridLayer",
  "EEG": "MedicalGridLayer",
  "EMG": "MedicalGridLayer",
  "PPG": "MedicalGridLayer",
  "Spirometry": "MedicalGridLayer",
  "Capnography": "MedicalGridLayer",
  "NIRS": "MedicalGridLayer",
  "Ventilator": "MedicalGridLayer",
  "IBP": "MedicalGridLayer",
  "Ultrasound M-Mode": "MedicalGridLayer",
  "VCG": "MedicalGridLayer",
  "EOG": "MedicalGridLayer",
  "Medical Sweep": "MedicalGridLayer",
  "Sweep EKG": "MedicalGridLayer",
  "Heart Rate Variability": "MedicalGridLayer",
  "Box Plot": "StatisticalGridLayer",
  "Violin Plot": "StatisticalGridLayer",
  "Histogram": "DefaultGridLayer",
  "KDE": "StatisticalGridLayer",
  "QQ Plot": "DefaultGridLayer",
  "ECDF": "DefaultGridLayer",
  "Error Bar": "DefaultGridLayer",
  "Statistical Error Bar": "DefaultGridLayer",
  "Confidence Interval": "DefaultGridLayer",
  "Band": "DefaultGridLayer",
  "Ridge Line": "DefaultGridLayer",
  "Hexbin": "DefaultGridLayer",
  "Sunburst": "DefaultGridLayer",
  "Sankey": "DefaultGridLayer",
  "Chord Diagram": "DefaultGridLayer",
  "Chernoff Faces": "DefaultGridLayer",
  "Joyplot": "DefaultGridLayer",
  "Lollipop": "DefaultGridLayer",
  "Heatmap": "DefaultGridLayer",
  "Streamgraph": "DefaultGridLayer",
  "Voronoi": "DefaultGridLayer",
  "Delaunay": "DefaultGridLayer",
  "Dependency Wheel": "PolarGridLayer",
  "Parallel Coordinates": "DefaultGridLayer",
  "Marimekko": "DefaultGridLayer",
  "Alluvial": "DefaultGridLayer",
  "Wind Rose": "PolarGridLayer",
  "Bullet Chart": "DefaultGridLayer",
  "Network": "DefaultGridLayer",
  "Arc Diagram": "DefaultGridLayer",
  "Dendrogram": "DefaultGridLayer",
  "Pareto": "DefaultGridLayer",
  "Smith Chart": "SmithChartGridLayer",
  "Geo Tactical": "GeoGridLayer",
  "Isometric Blueprint": "IsometricGridLayer",
  "Ternary Phase": "TernaryGridLayer",
  "Ternary Contour": "TernaryGridLayer",
  "Gantt Resource": "DefaultGridLayer",
  "Horizon": "DefaultGridLayer",
  "Vector Field": "AnalysisGridLayer",
  "FFT": "LogarithmicGridLayer",
  "Spectrogram": "LogarithmicGridLayer",
  "Regression": "AnalysisGridLayer",
  "Polynomial Regression": "AnalysisGridLayer",
  "Autocorrelation": "AnalysisGridLayer",
  "Change Point": "AnalysisGridLayer",
  "Outlier Detection": "AnalysisGridLayer",
  "Slope": "AnalysisGridLayer",
  "Adaptive Function": "AnalysisGridLayer",
  "Control Chart": "AnalysisGridLayer",
  "Gantt": "DefaultGridLayer"
}
```

Notes:
- Financial gap bands are disabled for Renko and Point & Figure (event‑based X axis).

## 10) Advanced Geometries (Specialized Grids)

- **PolarGridLayer**: concentric rings + radial spokes. Used for Polar/Radar/Wind Rose/Dependency Wheel.
- **TernaryGridLayer**: equilateral triangle grid (60° axes) for phase diagrams.
- **LogarithmicGridLayer**: decade/ratio ticks on any logarithmic axis (X and/or Y).
- **StatisticalGridLayer**: emphasized quantile lines for distribution visualizations.
- **SmithChartGridLayer**: unit circle with resistance/reactance loci for impedance analysis.
- **Smith Transform**: normalized unit-circle mapping; stable under zoom within the unit circle bounds.
- **Smith Light Mode**: dedicated minor/major colors for technical‑drawing contrast.
- **GeoGridLayer**: Mercator projection grid (meridians/parallels) for lat/lon displays.
- **Mercator Transform**: projection-aware mapping for renderer/grid alignment.
- **IsometricGridLayer**: 30°/60° axonometric grid for pseudo‑3D engineering views.

## Verification Notes

- Memory stability test uses warmup baseline (`-Darbercharts.stress.warmupMinutes=2`).
- Full GC count from `/tmp/arbercharts-memory-gc.log` is limited to explicit `System.gc()` calls.
