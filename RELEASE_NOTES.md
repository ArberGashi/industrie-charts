# ArberCharts v1.7.0 LTS - The Doctrine Architecture Release

This release locks in the Architecture Doctrine and delivers the headless, zero-allocation
rendering platform for industrial-grade visualization in the LTS line.

## Performance Certificate
- **Signal stress test:** 10,000,000 points, 16 channels, 4K render surface.
- **Warm render time:** ~2.7s headless (JIT-optimized pass).
- **Heap stability:** ~2.3 GB / 3.9 GB (steady), no GC spikes in the render path.
- **Zero-alloc rendering:** StrokeCache / ColorCache enforced across hot paths.

## Architecture Doctrine
- **Headless core:** domain.* and engine.* are AWT/Swing-free and server-ready.
- **Layer isolation:** strict separation of domain / engine / render / platform.
- **Legacy bridges:** ui.* removed in v1.7.0 LTS.

## Grid Doctrine (100% mapped)
Every renderer demo is now explicitly wired to its domain-correct grid layer (Financial/Medical/Statistical/Polar/Smith/Ternary/Geo/Isometric/Logarithmic/Default). Mapping is self-documented via `docs/DEMO_GRID_MAPPING.md`.

## Release Status
- **Baseline locked:** v1.7.0-LTS (internal milestone).
- **Core freeze:** active. No further changes to core until next LTS planning completes.

---
This is the industrial-grade foundation for the v1.7.0 LTS cycle.
