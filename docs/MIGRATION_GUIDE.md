# Migration Guide - v1.7.0 LTS (Summary)

This summary covers the migration into the v1.7.0 LTS architecture.
Full details are in `MIGRATION_GUIDE_v1.7.md`.

## Step 1 - Remove legacy ui.* imports
- `com.arbergashi.charts.ui.*` was removed in v1.7.0 LTS.
- Migrate to `platform.*`, `render.*`, and `domain.*` packages.

## Step 2 - Replace Deprecated Imports
Update imports:
- `com.arbergashi.charts.ui.*` -> `com.arbergashi.charts.platform.*`
- `com.arbergashi.charts.ui.grid.*` -> `com.arbergashi.charts.render.grid.*`
- `com.arbergashi.charts.ui.legend.*` -> `com.arbergashi.charts.domain.legend.*` or `render.legend.*`

## Step 3 - Adopt the Canvas Path
- Implement `render(ArberCanvas, ChartModel, PlotContext)` for custom renderers.
- Use the `PlotContext` transform and `setClip(...)` boundaries.

**Note:** v1.7.0 LTS is a hard cut. Validate builds against the new package structure.
