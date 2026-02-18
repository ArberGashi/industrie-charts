# ArberCharts 2.0.0 Release Notes (GitHub Bundle)

## Release-Daten

- Produktrelease: **2026-02-14**
- GitHub-Bundle vorbereitet: **2026-02-18**

## Enthaltene Artefakte

- `artifacts/arbercharts-core-2.0.0.jar`
- `artifacts/arbercharts-demo-2.0.0.jar`
- `artifacts/arbercharts-starter-2.0.0.jar`
- `artifacts/run-demo.sh`
- `artifacts/run-demo.bat`
- `artifacts/SHA256SUMS.txt`

## Technische Highlights

- Java 25 Baseline
- Fokus auf aktive Plattformmodule (`core`, `swing-bridge`, `server-bridge`, `spring-boot-starter`, `starter`, `demo`)
- Zero-GC Rendering-Policy in Hot Paths
- 157 produktive Renderer

## Integritätsprüfung

```bash
cd artifacts
shasum -a 256 -c SHA256SUMS.txt
```

## Lizenzmodell

- Binärartefakte: MIT
- Source Code: proprietär

Referenzen im Repository-Root:
- `LICENSE`
- `NOTICE`
- `BINARY-LICENSE.md`
