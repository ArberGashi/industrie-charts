package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;

/**
 * Internal helper for indicator renderers.
 *
 * <p>Goals:
 * <ul>
 *   <li>Compute visible index range using clip bounds and uniform X step.</li>
 *   <li>Provide small, allocation-free utilities reusable across RSI/MACD/Stochastic/etc.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
final class IndicatorRendererSupport {

    private IndicatorRendererSupport() {
    }

    /**
     * Computes an index range [start, endExclusive) for a uniformly distributed series.
     *
     * <p>This uses the plot bounds as view bounds and applies a small overscan to avoid gaps.</p>
     */
    static IndexRange visibleRange(PlotContext context, int pointCount, int overscan) {
        if (pointCount <= 0) return new IndexRange(0, 0);

        ArberRect view = context.getPlotBounds();

        double w = view.width();
        double step = (pointCount > 1) ? (w / (double) (pointCount - 1)) : w;
        if (!(step > 0.0)) {
            return new IndexRange(0, pointCount);
        }

        double leftX = view.x();
        double rightX = view.maxX();

        int start = (int) Math.floor((leftX - view.x()) / step) - overscan;
        int endExclusive = (int) Math.ceil((rightX - view.x()) / step) + overscan;

        if (start < 0) start = 0;
        if (endExclusive > pointCount) endExclusive = pointCount;
        if (endExclusive < start) endExclusive = start;

        return new IndexRange(start, endExclusive);
    }

    /**
     * Returns the plot bounds as doubles.
     */
    static Viewport viewport(PlotContext context) {
        ArberRect view = context.getPlotBounds();
        return new Viewport(view.x(), view.y(), view.width(), view.height());
    }

    static final class IndexRange {
        private int start;
        private int endExclusive;

        IndexRange(int start, int endExclusive) {
            this.start = start;
            this.endExclusive = endExclusive;
        }

        int getStart() {
            return start;
        }

        void setStart(int start) {
            this.start = start;
        }

        int getEndExclusive() {
            return endExclusive;
        }

        void setEndExclusive(int endExclusive) {
            this.endExclusive = endExclusive;
        }
    }

    static final class Viewport {
        private double x;
        private double y;
        private double w;
        private double h;

        Viewport(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        double getW() {
            return w;
        }

        double getH() {
            return h;
        }

        double getMaxX() {
            return x + w;
        }

        double getMaxY() {
            return y + h;
        }
    }
}
