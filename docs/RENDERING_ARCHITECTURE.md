# Rendering architecture

This document describes the **2.2.0** source tree. The dependency cleanup and API relocations below apply when upgrading from 2.1.0 to 2.2.0.

## Module boundaries

| Module | Responsibility | Platform dependencies |
| --- | --- | --- |
| `arbercharts-core` | Models, geometry, themes, renderers, spatial processing and rendering contracts | No production dependency on `java.desktop`, Swing, server bridges, Spring, Micrometer or UI frameworks |
| `arbercharts-swing-bridge` | Swing components, Java2D canvas, interaction, desktop export and AWT caches | `java.desktop` and optional export integrations |
| `arbercharts-server-bridge` | Headless raster rendering, PNG encoding and session lifecycle | Java2D, ImageIO and therefore `java.desktop` |
| `arbercharts-spring-boot-starter` | Server-service configuration and optional metrics integration | Server bridge and Spring Boot |
| `arbercharts-demo` / `arbercharts-spring-boot-demo` | Desktop gallery / local browser-based server demo | Their corresponding bridges and application frameworks |

Dependencies point from the bridges toward Core, and from the Spring Boot starter toward the server bridge. Core does not depend on a bridge. The whole production Core output is checked, including packages outside `com.arbergashi.charts.core`.

**Headless server rendering still requires `java.desktop`.** `ImageBufferCanvas` uses a `BufferedImage` and `Graphics2D` for clipping, stroke widths and source-over compositing. `-Djava.awt.headless=true` removes the need for a display server; it does not remove the JDK module. A custom runtime containing the server bridge must retain that module.

## Reusing renderers in another frontend

The normal renderer entry point is:

```java
renderer.render(canvas, model, context);
```

It accepts the platform-neutral `ArberCanvas`, `ChartModel` and `PlotContext` contracts. Another JVM frontend can supply a canvas adapter and reuse the existing renderer implementations. A browser frontend can use the server bridge, as the Spring Boot demo does; the Java renderers do not execute directly in JavaScript.

The host must provide more than drawing primitives:

1. Create and reset a rendering session, supply model-to-plot coordinates, and arrange chart layers.
2. Implement lines, polygons, rectangles, stroke widths, clipping, alpha compositing and voxel drawing according to `ArberCanvas`.
3. Integrate axes, text measurement, legends, tooltips, hit testing, zoom/pan and input events where the frontend requires them. Much of the current complete-chart composition lives in the Swing panel.
4. Supply device scaling, fonts and resource cleanup. Canvas instances belong to one session and are not required to be thread-safe; renderer instances can also contain mutable scratch state.
5. Handle `SpatialChunkRenderer` through its `renderSpatial()` and batch-consumer path. Calling only `ChartRenderer.render()` does not exercise every spatial renderer. The Swing and server hosts show the existing dispatch paths.

The architecture cleanup is accompanied by targeted renderer, grid and host corrections based on the [independent rendering audit](audits/2026-10-16-rendering/README.md). The retained audit describes the original baseline; regression tests document the corrected behavior. Frontend-specific types remain outside renderer algorithms.

## Canvas contract and current limits

| Area | Current contract or limitation |
| --- | --- |
| Geometry | Coordinates and stroke widths use canvas units. Point counts are bounded by the supplied arrays; insufficient points and non-positive fill sizes produce no output. |
| Color | ARGB channels are non-premultiplied. Raster backends composite with source-over alpha; antialiasing and edge rasterization need not be pixel-identical. |
| Clipping | `setClip` replaces the clip; `null` clears it. A host can save and restore the rectangle through `getClip`. |
| Voxels | Projected squares use the stroke width for their size. Per-voxel colors are temporary and preserve the current canvas color. |
| Text | Swing and server implement left-baseline text drawing, `ArberFont` selection and actual font metrics. Other adapters retain compatible optional defaults and documented approximate metrics. The command stream encodes text but has no font-metrics round trip; its consumer must implement drawing. |
| Transform | `getTransform()` exposes metadata. Existing canvas implementations return identity; this is not a common mutable transform stack or a guarantee that host/device transforms are represented. |
| Lifecycle | The host owns frame reset and device context disposal. `ImageBufferCanvas` and `ServerRenderService` are closeable; the service closes idle/replaced/overflow contexts and releases active contexts after shutdown when their renders finish. |

`ArberBridgeFactory` discovers `ArberCanvasProvider` implementations through `ServiceLoader`, using their ID, priority and availability. The provider interface currently has **no canvas creation method**; selection does not construct or connect a complete frontend. Display integration is a separate SPI: `ChartDisplayProvider`, used by `SimpleChart.show()`.

Contract tests cover primitive semantics, text metrics and the corrected spatial dispatch paths across Swing and server raster backends. They do not establish visual equivalence of every complete chart or replace real-display validation. See [Verification strategy](VERIFICATION_STRATEGY.md).

Derived renderers may provide `getPreferredXRange(model)` and `getPreferredYRange(model)` so hosts can autoscale the output domain, for example frequency rather than input time for FFT. Explicit axis limits and interactive view ranges take precedence. Grids use the context's independent X/Y visibility flags, and their Canvas and batch paths share the same geometry.

## Migration from 2.1.0

These classes move from Core to `arbercharts-swing-bridge` while retaining their fully qualified names and public signatures:

| Class | Reason |
| --- | --- |
| `com.arbergashi.charts.engine.allocation.ZeroAllocPool` | AWT color/stroke caching belongs to the desktop adapter. |
| `com.arbergashi.charts.engine.allocation.AllocationStats` | Statistics for the desktop allocation pool. |
| `com.arbergashi.charts.api.ViewportAuditExtractor` | PNG metadata extraction uses ImageIO; PDF metadata extraction belongs with desktop export. |

Applications using these APIs must include `arbercharts-swing-bridge` at the same version as Core. Existing imports remain valid once that bridge is present; applications depending only on Core must update their dependency declaration. Core no longer declares the optional FlatLaf dependencies.

The PDFBox reflection metadata moves with `ViewportAuditExtractor` to the Swing bridge. Core retains its playback reflection and localization/service resource metadata. See the [GraalVM metadata audit](GRAALVM_AUDIT.md) for scope and native-image validation limits.
