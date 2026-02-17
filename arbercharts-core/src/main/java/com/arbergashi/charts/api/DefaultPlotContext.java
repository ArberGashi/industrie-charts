package com.arbergashi.charts.api;

import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.NiceScale;

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
 * @since 2.0.0
 */
public final class DefaultPlotContext implements PlotContext {
    private ArberRect bounds;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private boolean logarithmicY;
    private boolean invertedX;
    private boolean invertedY;
    private NiceScale.ScaleMode scaleModeX;
    private NiceScale.ScaleMode scaleModeY;
    private ChartTheme theme;
    private ChartRenderHints renderHints;
    private AxisGapModel gapModel;
    private AnimationProfile animationProfile;
    private int requestedTickCountX = 10;
    private int requestedTickCountY = 8;

    public DefaultPlotContext(ArberRect bounds,
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
                              ChartRenderHints renderHints,
                              AxisGapModel gapModel,
                              AnimationProfile animationProfile) {
        Objects.requireNonNull(bounds, "bounds");
        this.bounds = bounds;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.logarithmicY = logarithmicY;
        this.invertedX = invertedX;
        this.invertedY = invertedY;
        this.scaleModeX = (scaleModeX != null) ? scaleModeX : NiceScale.ScaleMode.LINEAR;
        this.scaleModeY = (scaleModeY != null) ? scaleModeY : NiceScale.ScaleMode.LINEAR;
        this.theme = (theme != null) ? theme : ChartThemes.getDarkTheme();
        this.renderHints = renderHints;
        this.gapModel = gapModel;
        this.animationProfile = (animationProfile != null) ? animationProfile : AnimationProfile.ACADEMIC;
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
        this(bounds, minX, maxX, minY, maxY, logarithmicY, false, false, scaleModeX, scaleModeY, null, null, null, null);
    }

    /**
     * Constructor that allows axis inversion without a theme or render hints.
     */
    public DefaultPlotContext(ArberRect bounds,
                              double minX,
                              double maxX,
                              double minY,
                              double maxY,
                              boolean logarithmicY,
                              boolean invertedX,
                              boolean invertedY,
                              NiceScale.ScaleMode scaleModeX,
                              NiceScale.ScaleMode scaleModeY) {
        this(bounds, minX, maxX, minY, maxY, logarithmicY, invertedX, invertedY, scaleModeX, scaleModeY, null, null, null, null);
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
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                null,
                null,
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
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                null,
                null,
                null);
    }

    /**
     * Theme-aware constructor with render hints.
     */
    public DefaultPlotContext(ArberRect bounds, ChartModel model, double viewMinX, double viewMaxX, double viewMinY, double viewMaxY,
                              ChartTheme theme, ChartRenderHints renderHints) {
        this(bounds,
                Double.isNaN(viewMinX) ? getCalculatedMinX(model) : viewMinX,
                Double.isNaN(viewMaxX) ? getCalculatedMaxX(model) : viewMaxX,
                Double.isNaN(viewMinY) ? getCalculatedMinY(model) : viewMinY,
                Double.isNaN(viewMaxY) ? getCalculatedMaxY(model) : viewMaxY,
                false,
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                theme,
                renderHints,
                null,
                null);
    }

    @Override
    public ArberRect getPlotBounds() {
        return bounds;
    }

    public DefaultPlotContext setPlotBounds(ArberRect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
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
        this.scaleModeX = scaleModeX != null ? scaleModeX : NiceScale.ScaleMode.LINEAR;
        return this;
    }

    @Override
    public NiceScale.ScaleMode getScaleModeY() {
        return scaleModeY;
    }

    public DefaultPlotContext setScaleModeY(NiceScale.ScaleMode scaleModeY) {
        this.scaleModeY = scaleModeY != null ? scaleModeY : NiceScale.ScaleMode.LINEAR;
        return this;
    }

    @Override
    public ChartTheme getTheme() {
        return theme;
    }

    public DefaultPlotContext setTheme(ChartTheme theme) {
        this.theme = theme != null ? theme : ChartThemes.getDarkTheme();
        return this;
    }

    @Override
    public ChartRenderHints getRenderHints() {
        return renderHints;
    }

    public DefaultPlotContext setRenderHints(ChartRenderHints renderHints) {
        this.renderHints = renderHints;
        return this;
    }

    @Override
    public AxisGapModel getGapModel() {
        return gapModel;
    }

    public DefaultPlotContext setGapModel(AxisGapModel gapModel) {
        this.gapModel = gapModel;
        return this;
    }

    @Override
    public AnimationProfile getAnimationProfile() {
        return animationProfile;
    }

    public DefaultPlotContext setAnimationProfile(AnimationProfile animationProfile) {
        this.animationProfile = animationProfile != null ? animationProfile : AnimationProfile.ACADEMIC;
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

    @Override
    public void mapToPixel(double x, double y, double[] out) {
        Objects.requireNonNull(out, "out");
        if (out.length < 2) throw new IllegalArgumentException("out must have length >= 2");

        double bx = bounds.x();
        double by = bounds.y();
        double bw = Math.max(1e-9, bounds.width());
        double bh = Math.max(1e-9, bounds.height());

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

        double bx = bounds.x();
        double by = bounds.y();
        double bw = Math.max(1e-9, bounds.width());
        double bh = Math.max(1e-9, bounds.height());

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
        if (m instanceof com.arbergashi.charts.model.FinancialChartModel fin) {
            double[] range = getFinancialRange(fin);
            if (range != null) {
                return range[0];
            }
        }
        double min = m.getDataRange()[2];
        return min > 0 ? 0.0 : min; // baseline 0 for positive-only datasets
    }

    private static double getCalculatedMaxY(ChartModel m) {
        if (m == null) return 1.0;
        if (m instanceof com.arbergashi.charts.model.FinancialChartModel fin) {
            double[] range = getFinancialRange(fin);
            if (range != null) {
                return range[1];
            }
        }
        double max = m.getDataRange()[3];
        return max * 1.1; // 10% headroom
    }

    private static double[] getFinancialRange(com.arbergashi.charts.model.FinancialChartModel fin) {
        double[] lows = fin.getLowData();
        double[] highs = fin.getHighData();
        if (lows == null || highs == null) return null;
        int count = Math.min(fin.getPointCount(), Math.min(lows.length, highs.length));
        if (count <= 0) return null;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            double low = lows[i];
            double high = highs[i];
            if (Double.isFinite(low)) {
                min = Math.min(min, low);
            }
            if (Double.isFinite(high)) {
                max = Math.max(max, high);
            }
        }
        if (min == Double.POSITIVE_INFINITY || max == Double.NEGATIVE_INFINITY) return null;
        double span = Math.max(1e-9, max - min);
        double pad = span * 0.05;
        return new double[]{min - pad, max + pad};
    }
}
