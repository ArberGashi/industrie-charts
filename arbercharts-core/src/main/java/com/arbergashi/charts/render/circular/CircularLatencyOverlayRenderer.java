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
 * The label is centered along the bottom edge without covering the chart center.
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class CircularLatencyOverlayRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.circular.performance.enabled";

    private final LatencyTracker tracker;
    private final StringBuilder builder = new StringBuilder(48);
    private final char[] intBuf = new char[20];
    private String label = "";
    private ArberRect textClip;

    /**
     * Creates a latency label for a circular chart.
     * @param tracker timing history, or {@code null} to disable the overlay
     */
    public CircularLatencyOverlayRenderer(LatencyTracker tracker) {
        super("circularLatency");
        this.tracker = tracker;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (context == null) return;
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

        if (!label.contentEquals(builder)) label = builder.toString();
        ChartTheme theme = getResolvedTheme(context);
        canvas.setFont(theme.getBaseFont());
        canvas.setColor(theme.getAxisLabelColor());
        float margin = 8f;
        float lineHeight = canvas.getTextLineHeight();
        if (bounds.height() < lineHeight + margin * 2) return;
        float x = (float) Math.max(bounds.x() + margin,
                bounds.centerX() - canvas.measureTextWidth(label) * 0.5);
        float y = (float) bounds.maxY() - margin - lineHeight + canvas.getTextAscent();
        ArberRect previous = canvas.getClip();
        canvas.setClip(textClip(bounds, previous));
        try {
            canvas.drawText(x, y, label);
        } finally {
            canvas.setClip(previous);
        }
    }

    @Override
    public boolean supportsEmptyState() {
        return true;
    }

    @Override
    public void renderEmptyState(ArberCanvas canvas, ChartModel model, PlotContext context) {
        render(canvas, model, context);
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    private ArberRect textClip(ArberRect bounds, ArberRect previous) {
        if (previous == null) return bounds;
        double x = Math.max(bounds.x(), previous.x());
        double y = Math.max(bounds.y(), previous.y());
        double w = Math.max(0, Math.min(bounds.maxX(), previous.maxX()) - x);
        double h = Math.max(0, Math.min(bounds.maxY(), previous.maxY()) - y);
        if (textClip == null || textClip.x() != x || textClip.y() != y
                || textClip.width() != w || textClip.height() != h) {
            textClip = new ArberRect(x, y, w, h);
        }
        return textClip;
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
