package com.arbergashi.charts.render.common;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;

/**
 * Draws a labeled millimeter reference when physical scaling is enabled.
 *
 * <p>The marker uses {@link PlotContext#getPixelsPerMillimeter()} in logical canvas
 * units. A host must supply its calibrated density for a physically accurate
 * on-screen ruler; the context default is a 96-DPI layout estimate. Device and
 * export transforms are applied by the backend, without additional DPI scaling
 * in this renderer.</p>
 *
 * <p>{@code Chart.scale.physical.barMm} controls the length (default 10 mm),
 * {@code Chart.scale.physical.marginMm} controls the inset (default 4 mm), and
 * {@code Chart.scale.physical.y.enabled} adds an equally long vertical marker.
 * Markers are omitted when the configured length does not fit; their physical
 * length is never silently shortened to fit a smaller plot.</p>
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class PhysicalScaleRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.scale.physical.enabled";
    private float labeledMillimeters = Float.NaN;
    private String label = "";
    private Boolean enabled;

    /** Creates a physical scale overlay using the plot's logical density. */
    public PhysicalScaleRenderer() {
        super("physicalScale");
    }

    /**
     * Overrides the global activation setting for this overlay instance.
     * @param enabled whether to draw this physical scale marker
     * @return this renderer
     */
    public PhysicalScaleRenderer setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (context == null) return;
        boolean renderEnabled = enabled != null ? enabled : ChartAssets.getBoolean(KEY_ENABLED, false);
        if (!renderEnabled) {
            return;
        }
        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.getWidth() <= 1 || bounds.getHeight() <= 1) {
            return;
        }

        double pixelsPerMm = context.getPixelsPerMillimeter();
        float millimeters = ChartAssets.getFloat("Chart.scale.physical.barMm", 10f);
        float marginMm = ChartAssets.getFloat("Chart.scale.physical.marginMm", 4f);
        if (!Double.isFinite(pixelsPerMm) || pixelsPerMm <= 0
                || !Float.isFinite(millimeters) || millimeters <= 0
                || !Float.isFinite(marginMm) || marginMm < 0) return;

        ChartTheme theme = getResolvedTheme(context);
        canvas.setFont(theme.getBaseFont());
        if (labeledMillimeters != millimeters) {
            label = millimeters == Math.rint(millimeters) && millimeters < Integer.MAX_VALUE
                    ? ((int) millimeters) + " mm" : millimeters + " mm";
            labeledMillimeters = millimeters;
        }
        double length = millimeters * pixelsPerMm;
        double margin = marginMm * pixelsPerMm;
        double tick = Math.max(2, pixelsPerMm * 0.75);
        double lineHeight = canvas.getTextLineHeight();
        double x = bounds.x() + margin + tick;
        double y = bounds.maxY() - margin - lineHeight - tick - 3;
        double labelWidth = canvas.measureTextWidth(label);
        double labelX = Math.max(bounds.x() + margin, x + (length - labelWidth) * 0.5);
        if (!Double.isFinite(x + length) || !Double.isFinite(y)
                || x + length + tick > bounds.maxX() - margin
                || labelX + labelWidth > bounds.maxX() - margin
                || y - tick < bounds.y() + margin) return;

        ArberColor color = ChartAssets.getColor("Chart.scale.physical.color", theme.getAxisLabelColor());
        float alpha = ChartAssets.getFloat("Chart.scale.physical.alpha", 0.65f);
        if (!Float.isFinite(alpha)) alpha = 0.65f;
        canvas.setColor(ColorRegistry.applyAlpha(color, alpha));
        canvas.setStroke(1f);
        canvas.drawLine((float) x, (float) y, (float) (x + length), (float) y);
        canvas.drawLine((float) x, (float) (y - tick), (float) x, (float) (y + tick));
        canvas.drawLine((float) (x + length), (float) (y - tick), (float) (x + length), (float) (y + tick));
        if (ChartAssets.getBoolean("Chart.scale.physical.y.enabled", false)
                && y - length - tick >= bounds.y() + margin) {
            canvas.drawLine((float) x, (float) y, (float) x, (float) (y - length));
            canvas.drawLine((float) (x - tick), (float) y, (float) (x + tick), (float) y);
            canvas.drawLine((float) (x - tick), (float) (y - length), (float) (x + tick), (float) (y - length));
        }
        canvas.drawText((float) labelX, (float) (y + tick + 3 + canvas.getTextAscent()), label);
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
}
