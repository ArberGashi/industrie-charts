# Changelog

## v1.3.0

- Added CircularChartModel for lock-free, fixed-capacity realtime streams.
- Added smooth theme transitions and legend solo-mode dimming.
- Fixed race conditions in concurrent reads with sequence-guarded snapshots.
- Optimized rendering by removing per-frame `new Color()` allocations via ColorRegistry.
- Improved grid rendering sharpness with sub-pixel snapping.
