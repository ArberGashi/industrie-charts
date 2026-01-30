package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.LatencyTracker;

/**
 * Renders a compact p99/p99.9 latency report for circular dashboards.
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class CircularLatencyOverlayRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.circular.performance.enabled";

    private final LatencyTracker tracker;
    private final StringBuilder builder = new StringBuilder(48);
    private final char[] intBuf = new char[20];

    public CircularLatencyOverlayRenderer(LatencyTracker tracker) {
        super("circularLatency");
        this.tracker = tracker;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (tracker == null || tracker.getSampleCount() == 0) return;
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;

        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.getWidth() <= 1 || bounds.getHeight() <= 1) return;

        double p99 = tracker.getP99Millis();
        double p999 = tracker.getP999Millis();
        builder.setLength(0);
        builder.append("p99: ");
        appendMillis(builder, p99);
        builder.append(" | p99.9: ");
        appendMillis(builder, p999);

        // Text rendering lives in the UI bridges. Core only computes the message.
        ChartTheme theme = (context.getTheme() != null) ? context.getTheme() : getTheme();
        if (theme == null) return;
    }

    private void appendMillis(StringBuilder sb, double ms) {
        if (!Double.isFinite(ms)) {
            sb.append("n/a");
            return;
        }
        long scaled = Math.round(ms * 100.0);
        if (scaled < 0) {
            sb.append('-');
            scaled = -scaled;
        }
        long intPart = scaled / 100L;
        long fracPart = scaled % 100L;
        appendUnsigned(sb, intPart);
        sb.append('.');
        if (fracPart < 10) sb.append('0');
        appendUnsigned(sb, fracPart);
        sb.append("ms");
    }

    private void appendUnsigned(StringBuilder sb, long value) {
        if (value == 0) {
            sb.append('0');
            return;
        }
        int pos = intBuf.length;
        long v = value;
        while (v > 0) {
            long q = v / 10;
            int digit = (int) (v - q * 10);
            intBuf[--pos] = (char) ('0' + digit);
            v = q;
        }
        sb.append(intBuf, pos, intBuf.length - pos);
    }
}
