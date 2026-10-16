package com.arbergashi.charts.render.forensic;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Displays a playback status label during deterministic replays.
 * The label uses backend font metrics and preserves the caller's clipping area.
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class PlaybackStatusRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.playback.status.enabled";
    private static final String KEY_COLOR = "Chart.playback.status.color";
    private static final String KEY_ALPHA = "Chart.playback.status.alpha";
    private static final String KEY_MARGIN = "Chart.playback.status.margin";
    private static final String KEY_WATCHDOG_ENABLED = "Chart.watchdog.enabled";
    private static final String KEY_WATCHDOG_LEVEL = "Chart.watchdog.level";
    private static final String KEY_WATCHDOG_COLOR_WARN = "Chart.watchdog.color.warn";
    private static final String KEY_WATCHDOG_COLOR_CRIT = "Chart.watchdog.color.crit";
    private static final String KEY_WATCHDOG_PREFIX = "Chart.watchdog.message";
    private static final String KEY_WATCHDOG_PULSE_ENABLED = "Chart.watchdog.pulse.enabled";
    private static final String KEY_WATCHDOG_PULSE_RATE = "Chart.watchdog.pulse.hz";
    private static final String KEY_WATCHDOG_PULSE_MIN_ALPHA = "Chart.watchdog.pulse.minAlpha";

    private final PlaybackController playbackManager;
    private final StringBuilder builder = new StringBuilder(64);
    private String label = "";
    private ArberRect textClip;

    /**
     * Creates an overlay for a playback controller.
     * @param playbackManager playback status source, or {@code null} to disable the overlay
     */
    public PlaybackStatusRenderer(PlaybackController playbackManager) {
        super("playbackStatus");
        this.playbackManager = playbackManager;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (context == null) return;
        if (playbackManager == null || !playbackManager.isPlaybackActive()) return;
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;

        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.width() <= 0.0 || bounds.height() <= 0.0) return;

        builder.setLength(0);
        playbackManager.appendStatus(builder);
        if (builder.length() == 0) return;

        ChartTheme theme = getResolvedTheme(context);
        ArberColor base = ChartAssets.getColor(KEY_COLOR,
                theme != null ? theme.getAxisLabelColor() : ColorRegistry.of(160, 160, 160, 255));
        float alpha = ChartAssets.getFloat(KEY_ALPHA, 0.75f);
        if (ChartAssets.getBoolean(KEY_WATCHDOG_ENABLED, false)) {
            String level = ChartAssets.getString(KEY_WATCHDOG_LEVEL, "OK");
            if ("CRITICAL".equals(level)) {
                base = ChartAssets.getColor(KEY_WATCHDOG_COLOR_CRIT, ColorRegistry.of(255, 0, 0, 255));
                if (ChartAssets.getBoolean(KEY_WATCHDOG_PULSE_ENABLED, true)) {
                    float pulse = pulseAlpha();
                    alpha = Math.min(1.0f, Math.max(alpha, pulse));
                }
                String prefix = ChartAssets.getString(KEY_WATCHDOG_PREFIX, "");
                if (!prefix.isEmpty()) {
                    builder.insert(0, ' ').insert(0, prefix);
                }
            } else if ("WARN".equals(level)) {
                base = ChartAssets.getColor(KEY_WATCHDOG_COLOR_WARN, base);
                String prefix = ChartAssets.getString(KEY_WATCHDOG_PREFIX, "");
                if (!prefix.isEmpty()) {
                    builder.insert(0, ' ').insert(0, prefix);
                }
            }
        }

        float margin = ChartScale.scale(ChartAssets.getFloat(KEY_MARGIN, 8f));
        if (!Float.isFinite(margin) || margin < 0f) margin = 8f;
        if (!Float.isFinite(alpha)) alpha = 0.75f;
        canvas.setFont(theme.getBaseFont());
        if (bounds.height() < canvas.getTextLineHeight() + margin * 2) return;
        if (!label.contentEquals(builder)) label = builder.toString();
        float x = (float) bounds.x() + margin;
        float y = (float) bounds.y() + margin + canvas.getTextAscent();
        canvas.setColor(ColorRegistry.applyAlpha(base, alpha));
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

    private float pulseAlpha() {
        float rate = ChartAssets.getFloat(KEY_WATCHDOG_PULSE_RATE, 1.5f);
        float min = ChartAssets.getFloat(KEY_WATCHDOG_PULSE_MIN_ALPHA, 0.35f);
        long now = System.currentTimeMillis();
        double phase = (now / 1000.0) * rate * Math.PI * 2.0;
        float pulse = (float) ((Math.sin(phase) + 1.0) * 0.5);
        return min + (1.0f - min) * pulse;
    }
}
