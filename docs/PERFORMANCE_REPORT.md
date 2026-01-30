## Performance Report (v1.7.0 LTS Baseline Lock)

ArberCharts v1.7.0 LTS establishes the zero-allocation rendering baseline for the LTS cycle. The
render pipeline now operates without per-frame object creation in the hot paths, reducing
GC pressure and stabilizing p99 frame times for real-time workloads.

### Zero-Alloc Milestones

- **Grids (Medical/Analysis/Financial/Default)**: stroke allocations moved to `StrokeCache`.
- **Predictive layers**: alpha color generation moved to `ColorCache`, dash arrays reused.
- **Financial buffers**: `HighLowRenderer` and `WaterfallRenderer` now recycle rectangle buffers.
- **Statistical overlays**: histogram and box plot strokes centralized, dash arrays reused.
- **Specialized charts**: horizon palettes reused; chord diagram uses shared stroke cache.

### Hot-Path Allocation Policy

- No `new Color(...)`, `new BasicStroke(...)`, or per-frame geometry allocation in render loops.
- Reusable buffers and cached shapes are used for repeated geometry.
- Palettes are cached and reused; updates occur only on theme changes or capacity growth.

### Test Validation

- `mvn -pl arbercharts-core clean test`
- Architecture doctrine tests enforce layer isolation and headless domain purity.

### LTS Baseline Scope

This report freezes the v1.7.0 LTS performance baseline. Any change
that reintroduces per-frame allocations or violates the render hot-path policy must be
justified and benchmarked against this baseline.
