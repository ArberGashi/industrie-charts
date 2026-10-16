package com.arbergashi.charts.render.common;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.LatencyTracker;

/**
 * Renders a compact performance audit (p99/p99.9) for any chart.
 * Labels and clipping bounds are reused until their content or geometry changes.
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class PerformanceAuditRenderer {
    private static final String KEY_ENABLED = "Chart.performance.audit.enabled";

    private final LatencyTracker tracker;
    private final StringBuilder builder = new StringBuilder(64);
    private final char[] intBuf = new char[20];
    private String label = "";
    private ArberRect textClip;
    private Boolean enabled;

    /**
     * Creates a performance label for the supplied timing history.
     * @param tracker latency history, or {@code null} to disable the overlay
     */
    public PerformanceAuditRenderer(LatencyTracker tracker) {
        this.tracker = tracker;
    }

    /**
     * Overrides the global activation setting for this overlay instance.
     * @param enabled whether this overlay should draw when timing samples exist
     * @return this overlay
     */
    public PerformanceAuditRenderer setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Draws the enabled audit label at the top right of the plotting area.
     * @param canvas drawing backend
     * @param context current plot geometry and theme
     * @param theme explicit theme, or {@code null} to use the context theme
     */
    public void render(ArberCanvas canvas, PlotContext context, ChartTheme theme) {
        if (canvas == null || context == null) return;
        if (tracker == null || tracker.getSampleCount() == 0) return;
        boolean renderEnabled = enabled != null ? enabled : ChartAssets.getBoolean(KEY_ENABLED, false);
        if (!renderEnabled) return;
        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.getWidth() <= 1 || bounds.getHeight() <= 1) return;

        double p99 = tracker.getP99Millis();
        double p999 = tracker.getP999Millis();
        builder.setLength(0);
        builder.append("PERF p99: ");
        appendMillis(builder, p99);
        builder.append(" | p99.9: ");
        appendMillis(builder, p999);

        if (!label.contentEquals(builder)) label = builder.toString();
        ChartTheme resolved = theme != null ? theme : context.getTheme();
        if (resolved == null) resolved = ChartThemes.getDarkTheme();
        canvas.setFont(resolved.getBaseFont());
        canvas.setColor(resolved.getAxisLabelColor());
        float margin = 8f;
        if (bounds.height() < canvas.getTextLineHeight() + margin * 2) return;
        float x = (float) Math.max(bounds.x() + margin,
                bounds.maxX() - margin - canvas.measureTextWidth(label));
        float y = (float) bounds.y() + margin + canvas.getTextAscent();
        ArberRect previous = canvas.getClip();
        canvas.setClip(textClip(bounds, previous));
        try {
            canvas.drawText(x, y, label);
        } finally {
            canvas.setClip(previous);
        }
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
