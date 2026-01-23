package com.arbergashi.charts.internal;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.NiceScale;

import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Default PlotContext implementation.
 * <p>
 * Computes high-precision transformations between data and pixel spaces.
 * The allocation-free {@link #mapToPixel(double, double, double[])} method is the hot-path.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public record DefaultPlotContext(
        Rectangle2D bounds,
        double minX,
        double maxX,
        double minY,
        double maxY,
        boolean logarithmicY,
        NiceScale.ScaleMode scaleModeX,
        NiceScale.ScaleMode scaleModeY,
        ChartTheme theme,
        com.arbergashi.charts.api.ChartRenderHints renderHints
) implements PlotContext {

    public DefaultPlotContext {
        Objects.requireNonNull(bounds, "Bounds cannot be null");
        scaleModeX = (scaleModeX != null) ? scaleModeX : NiceScale.ScaleMode.LINEAR;
        scaleModeY = (scaleModeY != null) ? scaleModeY : NiceScale.ScaleMode.LINEAR;
        // Framework contract: theme must never be null during rendering.
        theme = (theme != null) ? theme : ChartThemes.defaultDark();
    }

    /**
     * Backwards-compatible constructor without theme.
     */
    public DefaultPlotContext(Rectangle2D bounds,
                              double minX,
                              double maxX,
                              double minY,
                              double maxY,
                              boolean logarithmicY,
                              NiceScale.ScaleMode scaleModeX,
                              NiceScale.ScaleMode scaleModeY) {
        this(bounds, minX, maxX, minY, maxY, logarithmicY, scaleModeX, scaleModeY, null, null);
    }

    /**
     * Constructor that derives scaling from the model with optional view overrides.
     */
    public DefaultPlotContext(Rectangle2D bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY) {
        this(bounds,
                Double.isNaN(viewMinX) ? calculateMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? calculateMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? calculateMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? calculateMaxY(model) : viewMaxY,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                null,
                null);
    }

    /**
     * Theme-aware constructor that derives scaling from the model with optional view overrides.
     */
    public DefaultPlotContext(Rectangle2D bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY, ChartTheme theme) {
        this(bounds,
                Double.isNaN(viewMinX) ? calculateMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? calculateMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? calculateMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? calculateMaxY(model) : viewMaxY,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                null);
    }

    public DefaultPlotContext(Rectangle2D bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY,
                              ChartTheme theme, com.arbergashi.charts.api.ChartRenderHints renderHints) {
        this(bounds,
                Double.isNaN(viewMinX) ? calculateMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? calculateMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? calculateMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? calculateMaxY(model) : viewMaxY,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                renderHints);
    }


    private static double calculateMinX(ChartModel m) {
        if (m == null) return 0.0;
        return m.getDataRange()[0];
    }

    private static double calculateMaxX(ChartModel m) {
        if (m == null) return 1.0;
        return m.getDataRange()[1];
    }

    private static double calculateMinY(ChartModel m) {
        if (m == null) return 0.0;
        double min = m.getDataRange()[2];
        return min > 0 ? 0.0 : min; /* baseline 0 for positive-only datasets */
    }

    private static double calculateMaxY(ChartModel m) {
        if (m == null) return 1.0;
        double max = m.getDataRange()[3];
        return max * 1.1; /* 10% headroom for aesthetics */
    }

    @Override
    public NiceScale.ScaleMode scaleModeX() {
        return scaleModeX;
    }

    @Override
    public NiceScale.ScaleMode scaleModeY() {
        return scaleModeY;
    }

    @Override
    public boolean isLogarithmicY() {
        return logarithmicY || scaleModeY == NiceScale.ScaleMode.LOGARITHMIC;
    }

    /* Scaling helpers */

    @Override
    public void mapToPixel(double dataX, double dataY, double[] dest) {
        // Fast, allocation-free write into dest.
        double bx = bounds.getX();
        double by = bounds.getY();
        double bw = bounds.getWidth();
        double bh = bounds.getHeight();

        /* Guard against division by zero when min and max are identical */
        double dx = (maxX - minX == 0) ? 1.0 : (maxX - minX);
        double invDx = 1.0 / dx;

        double effectiveY = transformY(dataY);
        double effectiveMinY = transformY(minY);
        double effectiveMaxY = transformY(maxY);

        double dy = (effectiveMaxY - effectiveMinY == 0) ? 1.0 : (effectiveMaxY - effectiveMinY);
        double invDy = 1.0 / dy;

        /* JDK 25 performance: use Math.fma for precise and fast transform */
        dest[0] = Math.fma((dataX - minX) * invDx, bw, bx);
        dest[1] = Math.fma((effectiveMaxY - effectiveY) * invDy, bh, by);
    }

    private double transformY(double dataY) {
        if (isLogarithmicY()) {
            if (dataY <= 0) return Math.log10(Double.MIN_NORMAL);
            return Math.log10(dataY);
        }
        return dataY;
    }

    @Override
    public void mapToData(double pixelX, double pixelY, double[] dest) {
        double dx = (maxX - minX == 0) ? 1.0 : (maxX - minX);

        double effectiveMinY2 = transformY(minY);
        double effectiveMaxY2 = transformY(maxY);
        double dy = (effectiveMaxY2 - effectiveMinY2 == 0) ? 1.0 : (effectiveMaxY2 - effectiveMinY2);

        double dataX = minX + ((pixelX - bounds.getX()) / bounds.getWidth()) * dx;
        double transformedY = effectiveMaxY2 - ((pixelY - bounds.getY()) / bounds.getHeight()) * dy;

        dest[0] = dataX;
        dest[1] = isLogarithmicY() ? Math.pow(10, transformedY) : transformedY;
    }

    @Override
    public Rectangle2D plotBounds() {
        return bounds;
    }
}
