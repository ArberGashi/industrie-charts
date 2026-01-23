package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.NiceScale;

import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Default {@link PlotContext} implementation.
 *
 * <p>This is a stable, public implementation that applications and renderers can use
 * without depending on {@code com.arbergashi.charts.internal.*} types.</p>
 *
 * <p><b>Performance:</b> The zero-allocation {@link #mapToPixel(double, double, double[])} and
 * {@link #mapToData(double, double, double[])} methods are designed for the render hot-path.</p>
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
        boolean invertedX,
        boolean invertedY,
        NiceScale.ScaleMode scaleModeX,
        NiceScale.ScaleMode scaleModeY,
        ChartTheme theme,
        ChartRenderHints renderHints
) implements PlotContext {

    public DefaultPlotContext {
        Objects.requireNonNull(bounds, "bounds");
        scaleModeX = (scaleModeX != null) ? scaleModeX : NiceScale.ScaleMode.LINEAR;
        scaleModeY = (scaleModeY != null) ? scaleModeY : NiceScale.ScaleMode.LINEAR;
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
        this(bounds, minX, maxX, minY, maxY, logarithmicY, false, false, scaleModeX, scaleModeY, null, null);
    }

    /**
     * Constructor that allows axis inversion without a theme or render hints.
     */
    public DefaultPlotContext(Rectangle2D bounds,
                              double minX,
                              double maxX,
                              double minY,
                              double maxY,
                              boolean logarithmicY,
                              boolean invertedX,
                              boolean invertedY,
                              NiceScale.ScaleMode scaleModeX,
                              NiceScale.ScaleMode scaleModeY) {
        this(bounds, minX, maxX, minY, maxY, logarithmicY, invertedX, invertedY, scaleModeX, scaleModeY, null, null);
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
                false,
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
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                null);
    }

    /**
     * Theme-aware constructor with render hints.
     */
    public DefaultPlotContext(Rectangle2D bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY,
                              ChartTheme theme, ChartRenderHints renderHints) {
        this(bounds,
                Double.isNaN(viewMinX) ? calculateMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? calculateMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? calculateMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? calculateMaxY(model) : viewMaxY,
                false,
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                renderHints);
    }

    @Override
    public Rectangle2D plotBounds() {
        return bounds;
    }

    @Override
    public void mapToPixel(double x, double y, double[] out) {
        Objects.requireNonNull(out, "out");
        if (out.length < 2) throw new IllegalArgumentException("out must have length >= 2");

        double bx = bounds.getX();
        double by = bounds.getY();
        double bw = Math.max(1e-9, bounds.getWidth());
        double bh = Math.max(1e-9, bounds.getHeight());

        double dx = (maxX - minX);
        double dy = (maxY - minY);
        if (dx == 0) dx = 1.0;
        if (dy == 0) dy = 1.0;

        double tX = (x - minX) / dx;
        double tY = (y - minY) / dy;

        if (invertedX) tX = 1.0 - tX;
        if (invertedY) tY = 1.0 - tY;

        out[0] = bx + tX * bw;
        out[1] = by + bh - tY * bh;
    }

    @Override
    public void mapToData(double pixelX, double pixelY, double[] dest) {
        Objects.requireNonNull(dest, "dest");
        if (dest.length < 2) throw new IllegalArgumentException("dest must have length >= 2");

        double bx = bounds.getX();
        double by = bounds.getY();
        double bw = Math.max(1e-9, bounds.getWidth());
        double bh = Math.max(1e-9, bounds.getHeight());

        double dx = (maxX - minX);
        double dy = (maxY - minY);
        if (dx == 0) dx = 1.0;
        if (dy == 0) dy = 1.0;

        double tX = (pixelX - bx) / bw;
        double tY = (by + bh - pixelY) / bh;

        if (invertedX) tX = 1.0 - tX;
        if (invertedY) tY = 1.0 - tY;

        dest[0] = minX + tX * dx;
        dest[1] = minY + tY * dy;
    }

    @Override
    public boolean isLogarithmicY() {
        return logarithmicY || scaleModeY == NiceScale.ScaleMode.LOGARITHMIC;
    }

    @Override
    public boolean isInvertedX() {
        return invertedX;
    }

    @Override
    public boolean isInvertedY() {
        return invertedY;
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
        return min > 0 ? 0.0 : min; // baseline 0 for positive-only datasets
    }

    private static double calculateMaxY(ChartModel m) {
        if (m == null) return 1.0;
        double max = m.getDataRange()[3];
        return max * 1.1; // 10% headroom
    }
}
