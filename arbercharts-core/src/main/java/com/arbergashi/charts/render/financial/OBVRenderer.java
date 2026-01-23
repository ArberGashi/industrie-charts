package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * <h1>OBVRenderer - On-Balance Volume</h1>
 *
 * <p>Professional OBV indicator renderer for volume-based momentum analysis.
 * Measures buying and selling pressure as a cumulative indicator.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>OBV Line:</b> Cumulative volume indicator</li>
 *   <li><b>Trend Identification:</b> Rising OBV = bullish, falling OBV = bearish</li>
 *   <li><b>Divergence Detection:</b> Visual comparison with price</li>
 *   <li><b>Zero Line:</b> Reference baseline</li>
 * </ul>
 *
 * <h2>Data Requirements:</h2>
 * <p>Requires price (close) and volume data.</p>
 *
 * <h2>Calculation:</h2>
 * <pre>
 * If Close &gt; PrevClose: OBV = PrevOBV + Volume
 * If Close &lt; PrevClose: OBV = PrevOBV - Volume
 * If Close = PrevClose: OBV = PrevOBV
 * </pre>
 *
 * <h2>Interpretation:</h2>
 * <ul>
 *   <li>Rising OBV = Accumulation (bullish)</li>
 *   <li>Falling OBV = Distribution (bearish)</li>
 *   <li>OBV confirms trend if it moves with price</li>
 *   <li>Divergence = potential reversal signal</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class OBVRenderer extends BaseRenderer {

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private final Path2D obvPath = new Path2D.Double();
    private final Path2D posArea = new Path2D.Double();
    /**
     * Cached local clip rectangles (mutated per paint, avoids allocations).
     */
    private final Rectangle2D.Double clipRect = new Rectangle2D.Double();
    // Cached indicator arrays
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient double[] xValues;
    private transient double[] obvValues;
    private final double[] preferredRange = new double[2];
    private boolean preferredRangeValid;

    public OBVRenderer() {
        super("obv");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        ensureCache(model);
        if (cachedPointCount <= 0 || xValues == null || obvValues == null) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(g, context, cachedPointCount, 2);
        final int start = range.start();
        final int endExclusive = range.endExclusive();
        if (endExclusive <= start) return;

        final ChartTheme theme = resolveTheme(context);
        final Color obvColor = theme.getAccentColor();
        final Color positiveColor = ColorUtils.withAlpha(theme.getBullishColor(), 0.18f);
        final Color negativeColor = ColorUtils.withAlpha(theme.getBearishColor(), 0.18f);

        // map baseline y once
        context.mapToPixel(xValues[start], 0, pxA);
        final double baseY = pxA[1];

        // Draw zero line (visible slice)
        context.mapToPixel(xValues[start], 0, pxA);
        context.mapToPixel(xValues[endExclusive - 1], 0, pxB);
        g.setColor(resolveTheme(context).getGridColor());
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));

        // Build line + area path (area closed to baseline) for visible slice
        obvPath.reset();
        posArea.reset();

        context.mapToPixel(xValues[start], obvValues[start], pxB);
        obvPath.moveTo(pxB[0], pxB[1]);

        posArea.moveTo(pxB[0], baseY);
        posArea.lineTo(pxB[0], pxB[1]);

        for (int i = start + 1; i < endExclusive; i++) {
            context.mapToPixel(xValues[i], obvValues[i], pxB);
            obvPath.lineTo(pxB[0], pxB[1]);
            posArea.lineTo(pxB[0], pxB[1]);
        }

        // close area back to baseline at last x
        context.mapToPixel(xValues[endExclusive - 1], 0, pxA);
        final double lastX = pxA[0];
        posArea.lineTo(lastX, baseY);
        posArea.closePath();

        // Fill above/below baseline using clip in local coordinates.
        final Shape oldClip = g.getClip();
        final AffineTransform oldTx = g.getTransform();
        try {
            // translate so that baseline is y=0 in local coordinates
            g.translate(0, baseY);

            final double bx = context.plotBounds().getX();
            final double by = context.plotBounds().getY();
            final double bw = context.plotBounds().getWidth();
            final double bh = context.plotBounds().getHeight();

            // clip rectangles must be in translated coordinates, hence y -= baseY
            final double localY = by - baseY;

            // positive part: y < 0 (above baseline)
            final double posH = -localY;
            if (posH > 0.0) {
                clipRect.setRect(bx, localY, bw, posH);
                g.setClip(clipRect);
                g.setColor(positiveColor);
                g.fill(posArea);
            }

            // negative part: y >= 0 (below baseline)
            final double negH = bh + Math.max(0.0, localY);
            if (negH > 0.0) {
                clipRect.setRect(bx, 0.0, bw, negH);
                g.setClip(clipRect);
                g.setColor(negativeColor);
                g.fill(posArea);
            }
        } finally {
            g.setTransform(oldTx);
            g.setClip(oldClip);
        }

        // Draw line
        g.setColor(obvColor);
        g.setStroke(getCachedStroke(ChartScale.scale(2.5f)));
        g.draw(obvPath);
    }

    /**
     * Calculates OBV from price (y) and volume (weight) data.
     * OBV values are cached in primitive arrays for performance.
     */
    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n < 2) {
            cachedModel = model;
            cachedPointCount = 0;
            preferredRangeValid = false;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && xValues != null && obvValues != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        preferredRangeValid = false;

        if (xValues == null || xValues.length < n || obvValues == null || obvValues.length < n) {
            xValues = new double[n];
            obvValues = new double[n];
        }

        // help static analysis; arrays are guaranteed after the allocation block
        final double[] xs = xValues;
        final double[] obvs = obvValues;

        for (int i = 0; i < n; i++) {
            xs[i] = model.getX(i);
        }

        double obv = 0.0;
        obvs[0] = obv;

        for (int i = 1; i < n; i++) {
            double volume = model.getWeight(i);
            double close = model.getY(i);
            double prevClose = model.getY(i - 1);

            if (close > prevClose) obv += volume;
            else if (close < prevClose) obv -= volume;

            obvs[i] = obv;
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double v = obvValues[i];
            if (!Double.isFinite(v)) continue;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (min != Double.POSITIVE_INFINITY && max != Double.NEGATIVE_INFINITY) {
            min = Math.min(0.0, min);
            if (min == max) {
                min -= 1.0;
                max += 1.0;
            }
            preferredRange[0] = min;
            preferredRange[1] = max;
            preferredRangeValid = true;
        }
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        ensureCache(model);
        return preferredRangeValid ? preferredRange : null;
    }
}
