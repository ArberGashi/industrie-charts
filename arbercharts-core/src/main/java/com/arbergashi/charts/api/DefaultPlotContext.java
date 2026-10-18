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
 * <p>Logarithmic axes require positive, finite bounds and data values. An
 * invalid logarithmic coordinate maps to {@code NaN} on that axis so renderers
 * can omit it without inventing a value at the plot boundary.</p>
 *
 * <p><b>Performance:</b> The zero-allocation {@link #mapToPixel(double, double, double[])} and
 * {@link #mapToData(double, double, double[])} methods are designed for the render hot-path.</p>
 *
 * @author Arber Gashi
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

        double tX = fraction(x, minX, maxX, scaleModeX == NiceScale.ScaleMode.LOGARITHMIC);
        double tY = fraction(y, minY, maxY, isLogarithmicY());

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

        double tX = (pixelX - bx) / bw;
        double tY = (by + bh - pixelY) / bh;

        if (invertedX) tX = 1.0 - tX;
        if (invertedY) tY = 1.0 - tY;

        dest[0] = valueAt(tX, minX, maxX, scaleModeX == NiceScale.ScaleMode.LOGARITHMIC);
        dest[1] = valueAt(tY, minY, maxY, isLogarithmicY());
    }

    private static double fraction(double value, double min, double max, boolean logarithmic) {
        if (!Double.isFinite(value) || !Double.isFinite(min) || !Double.isFinite(max)) return Double.NaN;
        if (logarithmic) {
            if (value <= 0 || min <= 0 || max <= 0) return Double.NaN;
            double span = logRatio(max, min);
            return logRatio(value, min) / (span == 0 ? 1 : span);
        }
        double span = max - min;
        if (span == 0) return value - min;
        // Halving first preserves a finite ratio for opposite finite extremes.
        return Double.isFinite(span) ? (value - min) / span
                : (value * 0.5 - min * 0.5) / (max * 0.5 - min * 0.5);
    }

    private static double valueAt(double fraction, double min, double max, boolean logarithmic) {
        if (!Double.isFinite(fraction) || !Double.isFinite(min) || !Double.isFinite(max)) return Double.NaN;
        if (logarithmic && (min <= 0 || max <= 0)) return Double.NaN;
        if (fraction == 0) return min;
        if (fraction == 1 && min != max) return max;
        if (logarithmic) {
            double span = logRatio(max, min);
            double offset = fraction * (span == 0 ? 1 : span);
            // Multiplication retains precision while zoomed into a narrow domain;
            // adding logarithms avoids overflow across hundreds of decades.
            return Math.abs(offset) < 1 ? Math.fma(min, Math.expm1(offset), min) : Math.exp(Math.log(min) + offset);
        }
        double span = max - min;
        if (span == 0) span = 1;
        return Double.isFinite(span) ? Math.fma(fraction, span, min)
                : (1 - fraction) * min + fraction * max;
    }

    private static double logRatio(double value, double reference) {
        double relative = (value - reference) / reference;
        return Math.abs(relative) < 0.5 ? Math.log1p(relative) : Math.log(value) - Math.log(reference);
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
        double[] range = m.getDataRange();
        return range[0] == range[1] ? paddedLower(range[0], constantPadding(range[0])) : range[0];
    }

    private static double getCalculatedMaxX(ChartModel m) {
        if (m == null) return 1.0;
        double[] range = m.getDataRange();
        return range[0] == range[1] ? paddedUpper(range[1], constantPadding(range[1])) : range[1];
    }

    private static double getCalculatedMinY(ChartModel m) {
        if (m == null) return 0.0;
        if (m instanceof com.arbergashi.charts.model.FinancialChartModel fin) {
            double[] range = getFinancialRange(fin);
            if (range != null) {
                return range[0];
            }
        }
        double[] range = m.getDataRange();
        double min = range[2];
        if (min > 0) return 0; // baseline 0 for positive-only datasets
        return min == range[3] ? paddedLower(min, constantPadding(min)) : min;
    }

    private static double getCalculatedMaxY(ChartModel m) {
        if (m == null) return 1.0;
        if (m instanceof com.arbergashi.charts.model.FinancialChartModel fin) {
            double[] range = getFinancialRange(fin);
            if (range != null) {
                return range[1];
            }
        }
        double[] range = m.getDataRange();
        double max = range[3];
        double padding = range[2] == max ? constantPadding(max) : Math.max(Math.abs(max) * 0.1, Math.ulp(max));
        return paddedUpper(max, padding);
    }

    private static double constantPadding(double value) {
        return value == 0 ? 1 : Math.max(Math.abs(value) * 0.1, Math.ulp(value));
    }

    private static double paddedLower(double value, double padding) {
        return Math.max(-Double.MAX_VALUE, value - padding);
    }

    private static double paddedUpper(double value, double padding) {
        return Math.min(Double.MAX_VALUE, value + padding);
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
        double pad = min == max ? constantPadding(min)
                : Math.max(max * 0.05 - min * 0.05, Math.max(Math.ulp(min), Math.ulp(max)));
        return new double[]{paddedLower(min, pad), paddedUpper(max, pad)};
    }
}
