package com.arbergashi.charts.render.military;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Overlay that indicates a lost/stale signal state.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SignalIntegrityLayer extends BaseRenderer {
    public SignalIntegrityLayer() {
        super("signalIntegrity");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean("Chart.signal.integrity.enabled", true)) return;
        if (context == null || !context.isSignalLost()) return;

        ArberRect bounds = context.getPlotBounds();
        if (bounds == null) return;

        ArberColor status = ChartAssets.getColor("Chart.signal.integrity.statusColor", ColorRegistry.ofArgb(0xFFF87171));
        float alpha = ChartAssets.getFloat("Chart.signal.integrity.lostAlpha", 0.4f);

        float padding = 8.0f;
        float x = (float) (bounds.x() + padding);
        float y = (float) (bounds.y() + padding);
        float w = Math.max(64f, (float) (bounds.width() * 0.3));
        float h = 14f;
        canvas.setColor(ColorRegistry.applyAlpha(status, Math.min(1f, Math.max(0.05f, alpha)) * 0.45f));
        canvas.fillRect(x, y, w, h);
    }
}
