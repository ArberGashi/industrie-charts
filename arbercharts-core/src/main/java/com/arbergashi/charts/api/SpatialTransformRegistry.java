package com.arbergashi.charts.api;

import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.specialized.SmithChartTransform;

/**
 * Registry for resolving coordinate transforms for spatial-capable layers.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 2.0.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class SpatialTransformRegistry {
    private static final CoordinateTransformer LINEAR = new LinearTransform();
    private static volatile CoordinateTransformer mercatorTransform;
    private static volatile CoordinateTransformer smithTransform;

    private SpatialTransformRegistry() {
    }

    public static CoordinateTransformer getLinearTransform() {
        return LINEAR;
    }

    public static CoordinateTransformer getMercatorTransform() {
        CoordinateTransformer current = mercatorTransform;
        if (current == null) {
            synchronized (SpatialTransformRegistry.class) {
                if (mercatorTransform == null) {
                    mercatorTransform = new MercatorTransform();
                }
                current = mercatorTransform;
            }
        }
        return current;
    }

    public static CoordinateTransformer getSmithTransform() {
        CoordinateTransformer current = smithTransform;
        if (current == null) {
            synchronized (SpatialTransformRegistry.class) {
                if (smithTransform == null) {
                    smithTransform = new SmithChartTransform();
                }
                current = smithTransform;
            }
        }
        return current;
    }

    /**
     * Resolves a coordinate transform for the given renderer.
     *
     * @param renderer renderer instance
     * @param override explicit override (may be null)
     * @return resolved transformer
     */
    public static CoordinateTransformer getResolvedTransform(ChartRenderer renderer, CoordinateTransformer override) {
        if (override != null) {
            return override;
        }
        if (renderer instanceof CoordinateTransformProvider provider) {
            CoordinateTransformer transformer = provider.getCoordinateTransformer();
            if (transformer != null) {
                return transformer;
            }
        }
        return LINEAR;
    }
}
