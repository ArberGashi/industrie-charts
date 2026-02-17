package com.arbergashi.charts.internal;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.NiceScale;
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
public final class DefaultPlotContext implements PlotContext {
    private ArberRect bounds;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private boolean logarithmicY;
    private NiceScale.ScaleMode scaleModeX;
    private NiceScale.ScaleMode scaleModeY;
    private ChartTheme theme;
    private com.arbergashi.charts.api.ChartRenderHints renderHints;
    private int requestedTickCountX = 10;
    private int requestedTickCountY = 8;

    public DefaultPlotContext(ArberRect bounds,
                              double minX,
                              double maxX,
                              double minY,
                              double maxY,
                              boolean logarithmicY,
                              NiceScale.ScaleMode scaleModeX,
                              NiceScale.ScaleMode scaleModeY,
                              ChartTheme theme,
                              com.arbergashi.charts.api.ChartRenderHints renderHints) {
        Objects.requireNonNull(bounds, "Bounds cannot be null");
        this.bounds = bounds;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.logarithmicY = logarithmicY;
        this.scaleModeX = (scaleModeX != null) ? scaleModeX : NiceScale.ScaleMode.LINEAR;
        this.scaleModeY = (scaleModeY != null) ? scaleModeY : NiceScale.ScaleMode.LINEAR;
        this.theme = (theme != null) ? theme : ChartThemes.getDarkTheme();
        this.renderHints = renderHints;
    }

    /**
     * Backwards-compatible constructor without theme.
     */
    public DefaultPlotContext(ArberRect bounds,
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
    public DefaultPlotContext(ArberRect bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY) {
        this(bounds,
                Double.isNaN(viewMinX) ? getCalculatedMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? getCalculatedMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? getCalculatedMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? getCalculatedMaxY(model) : viewMaxY,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                null,
                null);
    }

    /**
     * Theme-aware constructor that derives scaling from the model with optional view overrides.
     */
    public DefaultPlotContext(ArberRect bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY, ChartTheme theme) {
        this(bounds,
                Double.isNaN(viewMinX) ? getCalculatedMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? getCalculatedMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? getCalculatedMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? getCalculatedMaxY(model) : viewMaxY,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                null);
    }

    public DefaultPlotContext(ArberRect bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY,
                              ChartTheme theme, com.arbergashi.charts.api.ChartRenderHints renderHints) {
        this(bounds,
                Double.isNaN(viewMinX) ? getCalculatedMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? getCalculatedMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? getCalculatedMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? getCalculatedMaxY(model) : viewMaxY,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                renderHints);
    }


    private static double getCalculatedMinX(ChartModel m) {
        if (m == null) return 0.0;
        return m.getDataRange()[0];
    }

    private static double getCalculatedMaxX(ChartModel m) {
        if (m == null) return 1.0;
        return m.getDataRange()[1];
    }

    private static double getCalculatedMinY(ChartModel m) {
        if (m == null) return 0.0;
        double min = m.getDataRange()[2];
        return min > 0 ? 0.0 : min; /* baseline 0 for positive-only datasets */
    }

    private static double getCalculatedMaxY(ChartModel m) {
        if (m == null) return 1.0;
        double max = m.getDataRange()[3];
        return max * 1.1; /* 10% headroom for aesthetics */
    }

    @Override
    public boolean isLogarithmicY() {
        return logarithmicY || scaleModeY == NiceScale.ScaleMode.LOGARITHMIC;
    }

    /* Scaling helpers */

    @Override
    public void mapToPixel(double dataX, double dataY, double[] dest) {
        // Fast, allocation-free write into dest.
        double bx = bounds.x();
        double by = bounds.y();
        double bw = bounds.width();
        double bh = bounds.height();

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

        double dataX = minX + ((pixelX - bounds.x()) / bounds.width()) * dx;
        double transformedY = effectiveMaxY2 - ((pixelY - bounds.y()) / bounds.height()) * dy;

        dest[0] = dataX;
        dest[1] = isLogarithmicY() ? Math.pow(10, transformedY) : transformedY;
    }

    @Override
    public ArberRect getPlotBounds() {
        return bounds;
    }

    public DefaultPlotContext setPlotBounds(ArberRect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "Bounds cannot be null");
        return this;
    }

    @Override
    public double getMinX() {
        return minX;
    }

    public DefaultPlotContext setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    @Override
    public double getMaxX() {
        return maxX;
    }

    public DefaultPlotContext setMaxX(double maxX) {
        this.maxX = maxX;
        return this;
    }

    @Override
    public double getMinY() {
        return minY;
    }

    public DefaultPlotContext setMinY(double minY) {
        this.minY = minY;
        return this;
    }

    @Override
    public double getMaxY() {
        return maxY;
    }

    public DefaultPlotContext setMaxY(double maxY) {
        this.maxY = maxY;
        return this;
    }

    @Override
    public NiceScale.ScaleMode getScaleModeX() {
        return scaleModeX;
    }

    public DefaultPlotContext setScaleModeX(NiceScale.ScaleMode scaleModeX) {
        this.scaleModeX = (scaleModeX != null) ? scaleModeX : NiceScale.ScaleMode.LINEAR;
        return this;
    }

    @Override
    public NiceScale.ScaleMode getScaleModeY() {
        return scaleModeY;
    }

    public DefaultPlotContext setScaleModeY(NiceScale.ScaleMode scaleModeY) {
        this.scaleModeY = (scaleModeY != null) ? scaleModeY : NiceScale.ScaleMode.LINEAR;
        return this;
    }

    @Override
    public ChartTheme getTheme() {
        return theme;
    }

    public DefaultPlotContext setTheme(ChartTheme theme) {
        this.theme = (theme != null) ? theme : ChartThemes.getDarkTheme();
        return this;
    }

    @Override
    public com.arbergashi.charts.api.ChartRenderHints getRenderHints() {
        return renderHints;
    }

    public DefaultPlotContext setRenderHints(com.arbergashi.charts.api.ChartRenderHints renderHints) {
        this.renderHints = renderHints;
        return this;
    }

    @Override
    public int getRequestedTickCountX() {
        return requestedTickCountX;
    }

    public DefaultPlotContext setRequestedTickCountX(int requestedTickCountX) {
        this.requestedTickCountX = Math.max(2, requestedTickCountX);
        return this;
    }

    @Override
    public int getRequestedTickCountY() {
        return requestedTickCountY;
    }

    public DefaultPlotContext setRequestedTickCountY(int requestedTickCountY) {
        this.requestedTickCountY = Math.max(2, requestedTickCountY);
        return this;
    }
}
