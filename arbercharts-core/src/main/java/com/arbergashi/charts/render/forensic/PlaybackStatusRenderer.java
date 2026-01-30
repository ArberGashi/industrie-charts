package com.arbergashi.charts.render.forensic;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.geometry.TextAnchor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Displays a playback status label during deterministic replays.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
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
    private final com.arbergashi.charts.api.types.ArberPoint anchorBuf = new com.arbergashi.charts.api.types.ArberPoint();

    public PlaybackStatusRenderer(PlaybackController playbackManager) {
        super("playbackStatus");
        this.playbackManager = playbackManager;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (playbackManager == null || !playbackManager.isPlaybackActive()) return;
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;

        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.width() <= 0.0 || bounds.height() <= 0.0) return;

        playbackManager.appendStatus(builder);
        if (builder.length() == 0) return;

        ChartTheme theme = (context.getTheme() != null) ? context.getTheme() : getTheme();
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
                    builder.insert(0, prefix + " ");
                }
            } else if ("WARN".equals(level)) {
                base = ChartAssets.getColor(KEY_WATCHDOG_COLOR_WARN, base);
                String prefix = ChartAssets.getString(KEY_WATCHDOG_PREFIX, "");
                if (!prefix.isEmpty()) {
                    builder.insert(0, prefix + " ");
                }
            }
        }

        float margin = ChartScale.scale(ChartAssets.getFloat(KEY_MARGIN, 8f));
        anchorPoint(bounds, TextAnchor.TOP_LEFT, margin, margin, anchorBuf);
        float x = (float) anchorBuf.x();
        float y = (float) anchorBuf.y();
        float w = Math.max(ChartScale.scale(48f), ChartScale.scale(6f) * builder.length());
        float h = ChartScale.scale(6f);
        canvas.setColor(ColorRegistry.applyAlpha(base, alpha));
        canvas.fillRect(x, y, w, h);
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
