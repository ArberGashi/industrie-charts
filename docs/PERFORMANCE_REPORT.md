## Performance Report (v2.0.0)

ArberCharts 2.0.0 defines the current performance baseline for production use.
The render pipeline targets deterministic frame time and zero-allocation behavior in hot paths.

### Baseline Principles

- No per-frame `new Color(...)` or `new BasicStroke(...)` in render loops.
- Reusable buffers and caches for repeated geometry.
- Theme and palette reuse instead of transient object creation.

### Validation

- `mvn -pl arbercharts-core test`
- `mvn -pl arbercharts-swing-bridge test`
- Architecture and zero-GC rules enforced via test suite.

### Scope

This baseline applies to ArberCharts 2.0.0 only.
