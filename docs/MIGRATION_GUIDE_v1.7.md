# Migration Guide v1.7.0 (Doctrine Architecture)

This guide covers the migration from the legacy `ui.*` packages to the new
Doctrine architecture. The `ui.*` bridge remains available in v1.5.x, but it is
removed in v1.7.0 LTS.

## 1) The Great Renaming

| Legacy Package | New Package |
| --- | --- |
| `com.arbergashi.charts.ui` | `com.arbergashi.charts.platform.swing` |
| `com.arbergashi.charts.ui.export` | `com.arbergashi.charts.platform.export` |
| `com.arbergashi.charts.ui.legend` | `com.arbergashi.charts.render.legend` |
| `com.arbergashi.charts.ui.grid` | `com.arbergashi.charts.render.grid` |
| `com.arbergashi.charts.medical` | `com.arbergashi.charts.domain.medical` |
| `com.arbergashi.charts.predictive` | `com.arbergashi.charts.engine.predictive` |
| `com.arbergashi.charts.forensic` | `com.arbergashi.charts.engine.forensic` |

## 2) Upgrade Path (Recommended)

1. **Update to v1.5.x**  
   The `ui.*` bridge remains available. Compilation works, but the IDE will show
   deprecation warnings. This is expected.
2. **Replace Imports**  
   Move imports from `ui.*` to the new `platform.*`, `render.*`, and `domain.*`
   packages. No behavioral change should occur.
3. **Prepare for v1.7.0**  
   Remove all `ui.*` imports and verify the build. v1.7.0 removes all bridges.

## 3) Cache Integration (Renderer Authors)

If you own custom renderers, connect to the shared caches:

- `com.arbergashi.charts.platform.render.StrokeCache`
- `com.arbergashi.charts.platform.render.ColorCache`

This ensures zero-alloc behavior inside `paint()` methods.

## 4) Renderer Migration (No-Op -> ArberCanvas)

The ArberCanvas render path is the platform-agnostic baseline in v1.7.0-LTS. Renderer authors
should implement `render(ArberCanvas, ChartModel, PlotContext)` so the same renderer can run in
Swing, Compose, or headless/server environments without UI dependencies.

### 4.1 Sanity-Check Snippet (Renderer Authors)

```java
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.api.PlotContext;

/**
 * Sanity-Check for renderer authors: validates clipping and transform integrity.
 */
public static void validateRenderer(ArberCanvas canvas, PlotContext context) {
    // 1) Clipping boundary test
    ArberRect bounds = context.getPlotBounds();
    canvas.setClip(bounds);

    // 2) Transform verification (mapping check)
    double midX = (context.getMinX() + context.getMaxX()) * 0.5;
    double midY = (context.getMinY() + context.getMaxY()) * 0.5;
    double[] out = new double[2];
    context.mapToPixel(midX, midY, out);

    // Sanity: pixel result must fall inside the plot bounds
    double px = out[0];
    double py = out[1];
    if (px < bounds.x() || px > (bounds.x() + bounds.width())
            || py < bounds.y() || py > (bounds.y() + bounds.height())) {
        throw new IllegalStateException("Renderer Error: Logic-to-Pixel mapping exceeds plot bounds.");
    }
}
```

**ACHTUNG:** Renderer, die `setClip(...)` ignorieren oder manuelle Offsets ausserhalb der
`mapToPixel(...)`-Abbildung berechnen, erzeugen fehlerhafte Darstellungen in Multi-Panel-Layouts
und verletzen die ISO-Konformitaet des Frameworks.

## 5) Headless Advantage

The `domain.*` layer is now AWT/Swing-free. You can test core logic in headless
environments (CI, Docker, servers) without a display.

## 6) Key Bridge Points

The only remaining legacy entry points during v1.5.x:

- `com.arbergashi.charts.ui.ArberChartPanel` (bridge to `platform.swing`)
- `com.arbergashi.charts.ui.ChartExportService` (bridge to `platform.export`)

Both are removed in v1.7.0 LTS.

## 7) Final Note

v1.7.0 is a hard cut. Plan your migration early and validate builds against the
new package structure before upgrading.
