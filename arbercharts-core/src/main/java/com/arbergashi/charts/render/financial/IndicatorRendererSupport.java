package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;

import java.awt.*;

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
     * <p>This uses the current {@link Graphics2D} clip as view bounds.
     * It intentionally uses a small overscan to avoid gaps when anti-aliasing is enabled.
     */
    static IndexRange visibleRange(Graphics2D g, PlotContext context, int pointCount, int overscan) {
        if (pointCount <= 0) return new IndexRange(0, 0);

        Rectangle clip = g.getClipBounds();
        Rectangle view = (clip != null) ? clip : context.plotBounds().getBounds();

        double w = context.plotBounds().getWidth();
        double step = (pointCount > 1) ? (w / (double) (pointCount - 1)) : w;
        if (!(step > 0.0)) {
            return new IndexRange(0, pointCount);
        }

        double leftX = view.getX();
        double rightX = view.getMaxX();

        int start = (int) Math.floor((leftX - context.plotBounds().getX()) / step) - overscan;
        int endExclusive = (int) Math.ceil((rightX - context.plotBounds().getX()) / step) + overscan;

        if (start < 0) start = 0;
        if (endExclusive > pointCount) endExclusive = pointCount;
        if (endExclusive < start) endExclusive = start;

        return new IndexRange(start, endExclusive);
    }

    /**
     * Returns the clip bounds as doubles; falls back to {@link PlotContext#plotBounds()}.
     *
     * <p>This avoids {@code context.plotBounds().getBounds()} allocations by reading the
     * underlying values directly.
     */
    static Viewport viewport(Graphics2D g, PlotContext context) {
        Rectangle clip = g.getClipBounds();
        if (clip != null) {
            return new Viewport(clip.getX(), clip.getY(), clip.getWidth(), clip.getHeight());
        }
        return new Viewport(
                context.plotBounds().getX(),
                context.plotBounds().getY(),
                context.plotBounds().getWidth(),
                context.plotBounds().getHeight()
        );
    }

    record IndexRange(int start, int endExclusive) {
    }

    record Viewport(double x, double y, double w, double h) {
        double maxX() {
            return x + w;
        }

        double maxY() {
            return y + h;
        }
    }
}
