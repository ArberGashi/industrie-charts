# Changelog

## v1.7.0 LTS

- LTS hard cut: legacy `ui.*` compatibility removed.
- ArberCanvas render path is the baseline for platform-agnostic rendering.
- Renderer capability/affinity metadata finalized for spatial and transform-aware layers.
- Documentation consolidated for LTS (user guide, renderer catalog, compliance).

## v1.5.0

ARCHITECTURAL MILESTONE: THE DOCTRINE SHIFT

- High-Integrity Architecture Doctrine activated: domain/engine/render/platform are now the authoritative layers.
- Hard-cut executed for legacy ui.* implementations; only external bridge points remain.
- Domain is now fully headless (no AWT/Swing dependencies).
- Medical grid parameters externalized to properties (no hardcoded constants).
- Architecture Doctrine enforcement via ArchUnit added to core test suite.

## v1.3.0

- Added CircularChartModel for lock-free, fixed-capacity realtime streams.
- Added smooth theme transitions and legend solo-mode dimming.
- Fixed race conditions in concurrent reads with sequence-guarded snapshots.
- Optimized rendering by removing per-frame `new Color()` allocations via ColorRegistry.
- Improved grid rendering sharpness with sub-pixel snapping.
