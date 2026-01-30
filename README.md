# ArberCharts

![ci-core](https://github.com/ArberGashi/charts/actions/workflows/ci-core.yml/badge.svg?branch=main)
![ci-compose](https://github.com/ArberGashi/charts/actions/workflows/ci-compose.yml/badge.svg?branch=main)
![ci-qt-swift](https://github.com/ArberGashi/charts/actions/workflows/ci-qt-swift.yml/badge.svg?branch=main)

ArberCharts is a premium Java charting framework built for production systems that demand
deterministic rendering, high-end visuals, and developer-friendly APIs. It delivers
**158 renderers** across Standard, Financial, Statistical, Specialized, Medical, and Analysis domains.

DOCTRINE COMPLIANT (v1.7.0-LTS)  
Layer Isolation: Certified | Headless Core: Verified | Legacy Bridges: Isolated

## Index

- Strategy & Policy:
  - `docs/EXECUTIVE_SUMMARY.md`
  - `docs/DOCTRINE_POLICY.md`
- Demo Mapping:
  - `docs/DEMO_GRID_MAPPING.md`
- Performance & Stability:
  - `docs/PERFORMANCE_REPORT.md`
- Upgrade & Migration:
  - `docs/MIGRATION_GUIDE_v1.7.md`
  - `docs/v1.7.0_BREAKING_CHANGES.md`

## Important Note

This public repository is used for releases and documentation.  
The source code is proprietary and distributed under license.

## Product Strengths

- **Zero-GC rendering mindset**: allocation-free render loops to avoid micro-stutters.
- **CircularChartModel**: lock-free ring buffer for real-time data (EKG/finance).
- **Zero-GC theme pipeline**: color flyweights and smooth theme transitions.
- **Fluent API with ArberChartBuilder**: assemble charts in a few readable lines.
- **158 renderers**: deep coverage for Financial, Medical, Statistical, and Specialized charts.
- **Presentation-grade UI features**: legends, tooltips, crosshair, grid layers.
- **Export pipeline**: PNG/SVG/PDF for reporting and distribution.
- **Java Swing native integration**: built for desktop UI with precise rendering control.
- **Spring Boot friendly**: clean integration for services + desktop delivery workflows.
- **JDK 25 optimized**: tuned for modern runtime performance.
- **Demo app**: a complete showcase for evaluation and client presentations.

## Use Cases

- **Medical & Life Sciences**: real-time ECG/EEG with sweep-erase and high-precision crosshair.
- **Financial Engineering**: candlesticks, Ichimoku, Renko, Volume Profile, technical overlays.
- **Advanced Analytics**: Sankey, Network, Hexbin, FFT, regression and anomaly detection.
- **Industrial & Engineering**: control charts, heatmaps, Delaunay/Voronoi, multivariate analysis.

## Documentation

All documentation now lives under `docs/`.

- `docs/PERFORMANCE_REPORT.md`
- `docs/EXECUTIVE_SUMMARY.md`
- `docs/AUDIT_RESPONSE_v1.7.md`
- `docs/AUDIT_CLOSURE_MATRIX_v1.7.md`
- `docs/API_NAMING_REVIEW_v1.7.md`
- `docs/BRIDGE_VERIFICATION.md`
- `docs/CI_SETUP.md`
- `docs/MIGRATION_GUIDE_v1.7.md`
- `docs/v1.7.0_BREAKING_CHANGES.md`
- `docs/DOCTRINE_POLICY.md`
- `docs/DEMO_GRID_MAPPING.md`
- `docs/USER_GUIDE.md`
- `docs/RENDERER_CATALOG.md`

To refresh the demo grid mapping during builds, run the docs profile:

```bash
mvn clean install -Pdocs
```

## Downloads

Release assets are published on GitHub Releases:
https://github.com/ArberGashi/charts/releases

- Core JAR: `arbercharts-core-1.7.0-LTS.jar`
- Demo JAR: `arbercharts-demo-1.7.0-LTS.jar`
- Installers: macOS (.dmg), Windows (.msi), Linux (.deb)

## Quick Start (Demo)

```bash
java --enable-native-access=ALL-UNNAMED -jar arbercharts-demo-1.7.0-LTS.jar
```

## System Requirements

- Java 25 recommended

## License (Proprietary)

- Individual Developer License: free for development & evaluation
- Team License: required for small and large teams
- Runtime/Distribution License: required for commercial products shipped to end customers

## Support

gashi@pro-business.ch  
https://www.arbergashi.com
