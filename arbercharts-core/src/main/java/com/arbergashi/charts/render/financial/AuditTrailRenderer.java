package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.ProvenanceFlags;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.TooltipContentProvider;
import com.arbergashi.charts.render.TooltipContext;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Overlay renderer that marks non-original ticks for audit/provenance tracking.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class AuditTrailRenderer extends BaseRenderer implements TooltipContentProvider {
    public AuditTrailRenderer() {
        super("auditTrail");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (model == null || context == null) return;
        if (!ChartAssets.getBoolean("Chart.audit.enabled", true)) return;
        int count = model.getPointCount();
        if (count <= 0) return;

        int size = Math.max(2, ChartAssets.getInt("Chart.audit.marker.size", 3));
        double yBase = context.isInvertedY() ? context.getMaxY() : context.getMinY();
        double[] px = PIXEL_BUF.get();

        for (int i = 0; i < count; i++) {
            byte flag = model.getProvenanceFlag(i);
            if (flag == ProvenanceFlags.ORIGINAL) continue;
            double x = model.getX(i);
            if (x < context.getMinX() || x > context.getMaxX()) continue;
            context.mapToPixel(x, yBase, px);
            float ix = (float) Math.round(px[0]) - size / 2.0f;
            float iy = (float) Math.round(px[1]) - size / 2.0f;
            canvas.setColor(getResolvedFlagColor(flag));
            canvas.fillRect(ix, iy, size, size);
        }
    }

    private ArberColor getResolvedFlagColor(byte flag) {
        return switch (flag) {
            case ProvenanceFlags.MANUAL -> ChartAssets.getColor("Chart.audit.flagColor.edited", ColorRegistry.of(255, 0, 0, 255));
            case ProvenanceFlags.SYNTHETIC -> ChartAssets.getColor("Chart.audit.flagColor.synthetic", ColorRegistry.of(255, 0, 255, 255));
            case ProvenanceFlags.SMOOTHED -> ChartAssets.getColor("Chart.audit.flagColor.smoothed", ColorRegistry.of(255, 165, 0, 255));
            default -> ChartAssets.getColor("Chart.audit.flagColor.unknown", ColorRegistry.of(128, 128, 128, 255));
        };
    }

    @Override
    public void getContent(StringBuilder target, TooltipContext ctx) {
        if (target == null || ctx == null) return;
        ChartModel model = ctx.getModel();
        int idx = ctx.getIndex();
        if (model == null || idx < 0) return;

        byte flag = model.getProvenanceFlag(idx);
        if (flag == ProvenanceFlags.ORIGINAL) return;

        target.append("Provenance: ").append(ProvenanceFlags.label(flag));
        short sourceId = model.getSourceId(idx);
        if (sourceId != 0) {
            target.append('\n').append("SourceId: ").append(sourceId);
        }
        long ts = model.getTimestampNanos(idx);
        if (ts != 0L) {
            target.append('\n').append("TimestampNs: ").append(ts);
        }
    }
}
