# Javadoc Update Report (Architecture Doctrine)

Note: This report documents the v1.5.0 doctrine pass. For v1.7.0 LTS, use the same
validation flow with updated version strings and targets.

## Scope
- Modules: `arbercharts-core`, `arbercharts-ext-properties`, `arbercharts-starter` (demo excluded)
- Targets: public classes, protected methods, package-level docs
- Doctrine requirements:
  - `@since 1.5.0` on public classes and protected methods (v1.5.0 baseline)
  - Renderers: “Part of the Zero-Allocation Render Path. High-frequency execution safe.”
  - Domain/Engine: “Platform-independent and headless-certified. No AWT/Swing dependencies.”
  - Updated `@see` / `@link` references away from legacy `ui` paths

## Changes Applied
- Automated pass injected/updated Javadocs for public classes and protected methods in core + ext-properties.
- Package docs updated with doctrine notes and `@since 1.5.0`:
  - `com.arbergashi.charts.api`
  - `com.arbergashi.charts.internal`
  - `com.arbergashi.charts.util`
  - `com.arbergashi.charts.model`
  - `com.arbergashi.charts.render`
- Legacy `ui` references verified; only remaining `ui` packages are the two bridge classes (`ArberChartPanel`, `ChartExportService`).

## Notes
- `arbercharts-starter` currently contains no Java sources; no direct Javadoc changes were required there.
- `arbercharts-ext-properties` contains two public classes; both were updated by the automated pass.

## Next Validation Steps
- Regenerate Javadocs for the three modules:
  ```bash
  mvn clean install -DskipTests -pl arbercharts-core,arbercharts-ext-properties,arbercharts-starter
  mvn javadoc:javadoc -pl arbercharts-core,arbercharts-ext-properties,arbercharts-starter \
    -Dbottom="Copyright &#169; 2023-2026 com.arbergashi. All Rights Reserved." \
    -Ddoctitle="ArberCharts v1.7.0 LTS - Architecture Doctrine Edition"
  ```
- Inspect `arbercharts-core/target/site/apidocs/index.html` for group integrity and legacy warnings.
