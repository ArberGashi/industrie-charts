# ArberCharts v2.0.1

**Release Date:** July 23, 2026

## 🚀 Runs Without JVM Flags

ArberCharts 2.0.1 no longer requires `--add-modules jdk.incubator.vector` at runtime.
The incubating Vector API is now detected once at startup:

- **SIMD available** (flag set): the vectorized kernels run exactly as before.
- **SIMD unavailable** (no flag): scalar fallbacks take over transparently, with
  numerically identical results (verified by `SpatialSimdFallbackParityTest`).

This makes the library a drop-in dependency for Spring Boot services and any
standard JVM deployment — no custom launch configuration needed. When the Vector
API leaves incubation in a future JDK, the fast path activates everywhere
automatically.

### Details

- All `jdk.incubator.vector` usage is isolated in one internal kernel class
  (`SpatialSimdKernels`), guarded by a one-time runtime probe in `VectorIntrinsics`.
- New API: `VectorIntrinsics.isAvailable()`; `VectorIntrinsics.getLaneCount()`
  returns `1` when SIMD is unavailable.
- SIMD can be disabled explicitly with `-Darbercharts.simd.disabled=true`
  (benchmarking/diagnostics).
- Zero-GC behavior is preserved on both paths.
- Fixed a redundant tail recomputation in `Matrix4x4.getCalculatedTransformBatch`
  on the SIMD path.

## 📦 Spring Boot Quick Start

The README now documents server-side PNG rendering with
`arbercharts-spring-boot-starter` — one dependency, auto-configured
`ServerRenderService`, no JVM flags.

## 🔧 Build

- `central-publishing-maven-plugin` updated to 0.11.0.

## Artifacts

Published to Maven Central under `com.arbergashi`:
`arbercharts-core`, `arbercharts-server-bridge`, `arbercharts-spring-boot-starter`,
`arbercharts-swing-bridge` (and `arber-charts-parent`).
