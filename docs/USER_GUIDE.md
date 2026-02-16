# ArberCharts 1.7.0-LTS User Guide

ArberCharts is a high‑performance Java charting framework built for mission‑critical systems.
This guide is written for developers integrating the framework into JVM desktop or server applications,
and for teams using native bridges (Qt/Swift) where available.

## Downloads

All official artifacts are published on GitHub Releases:
https://github.com/ArberGashi/charts/releases

- **JVM Starter (Core + Swing + Server)**: `arbercharts-starter-1.7.0-LTS.jar`
- **Compose Bridge**: `arbercharts-compose-bridge-1.7.0-LTS.jar`
- **Qt Bridge (macOS arm64)**: `arbercharts-qt-bridge-macos-1.7.0-LTS.zip`
- **Swift Bridge (macOS arm64)**: `arbercharts-swift-bridge-macos-1.7.0-LTS.zip`
  - Includes `.xcframework` + `arbercharts-core.dylib`

## Platform Matrix

- **JVM Desktop/Server**: Starter JAR (Core + Swing + Server)
- **Compose Desktop**: Starter JAR + Compose Bridge
- **Qt (native)**: macOS arm64 ZIP (Windows/Linux planned)
- **Swift (native)**: macOS arm64 only in v1.7.0‑LTS

## Quick Start (JVM)

Use the **starter JAR** for all JVM integrations. It contains the core plus Swing and Server bridges.

Minimal setup:
- Add the starter JAR to your classpath.
- Create a `ChartModel`, choose a renderer, and attach it to a Swing panel or server renderer.

## Compose Desktop

Compose integrations require:
- `arbercharts-starter-1.7.0-LTS.jar`
- `arbercharts-compose-bridge-1.7.0-LTS.jar`

The Compose bridge is a thin adapter; it does not bundle core or Kotlin runtime dependencies.

## Qt Bridge (macOS arm64)

The Qt bridge is shipped as a native ZIP for macOS arm64. It includes the native binary and headers.

## Swift Bridge (macOS arm64)

The Swift bridge is shipped as a macOS arm64 `.xcframework` packaged in the ZIP.
It requires `arbercharts-core.dylib` (included in the ZIP).

## Data Model Contract (Important)

- Always use `getPointCount()` as the logical size.
- Arrays from `getXData()` / `getYData()` may be larger than the logical size.
- For high‑frequency streams, use `CircularChartModel`.

## Scaling & Compliance

For regulated environments, use the scaling and calibration rules defined in `COMPLIANCE.md`.

## Renderer Catalog

A full renderer catalog is available in `RENDERER_CATALOG.md`.

## Licensing

Licensing terms and usage rights are defined in `LICENSING.md`.

## Support

gashi@pro-business.ch
https://www.arbergashi.com
