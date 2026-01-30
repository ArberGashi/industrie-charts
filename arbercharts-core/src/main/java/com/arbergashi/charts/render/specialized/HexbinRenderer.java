package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.HexLayout;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Optional;

/**
 * Hexbin renderer: maps points into a hexagonal bin grid and draws density.
 * Implementation focuses on minimizing allocations in the draw path.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class HexbinRenderer extends BaseRenderer {

    private static final int ALPHA_LEVELS = 12;

    static {
        RendererRegistry.register("hexbin", new RendererDescriptor("hexbin", "renderer.hexbin", "/icons/hexbin.svg"), HexbinRenderer::new);
    }

    // Reusable mapping buffer to avoid allocations in the hot loop.
    private final double[] pix = new double[2];
    // counters for each hex cell
    private int[] counts = new int[0];
    // Track which bins were touched so we can clear only those (avoids Arrays.fill on large grids).
    private int[] touched = new int[0];
    private int touchedSize = 0;
    // cached layout for current hex size (transient to avoid serialization issues)
    private transient HexLayout cachedLayout;
    private transient double cachedHexSize = -1;
    private transient double cachedHexH;
    private transient float[] hexX;
    private transient float[] hexY;

    public HexbinRenderer() {
        super("hexbin");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ArberRect bounds = context.getPlotBounds();
        double width = bounds.width();
        double height = bounds.height();

        // determine hex size based on target density; keep a minimum
        double hexSize = Math.max(6.0, width / 80.0);
        double hexH = Math.sqrt(3) * hexSize;

        int cols = (int) Math.ceil(width / (hexSize * 1.5)) + 2;
        int rows = (int) Math.ceil(height / hexH) + 2;
        int total = cols * rows;

        if (counts.length < total) {
            counts = RendererAllocationCache.getIntArray(this, "counts", total);
        }
        if (touched.length < total) {
            // Worst-case: every bin can be touched (e.g., very noisy data). Allocate full capacity for correctness.
            // We still clear only the touched bins each frame, so this is about correctness, not per-frame cost.
            touched = RendererAllocationCache.getIntArray(this, "touched", total);
        }

        // Clear only touched bins from previous frame.
        for (int i = 0; i < touchedSize; i++) {
            counts[touched[i]] = 0;
        }
        touchedSize = 0;

        // Map points into grid (hot loop)
        double bx = bounds.x();
        double by = bounds.y();
        double invCol = 1.0 / (hexSize * 1.5);
        double invRow = 1.0 / hexH;

        for (int pi = 0; pi < count; pi++) {
            context.mapToPixel(xData[pi], yData[pi], pix);
            double x = pix[0] - bx;
            double y = pix[1] - by;
            int col = (int) (x * invCol);
            int row = (int) (y * invRow);
            if (col < 0 || row < 0 || col >= cols || row >= rows) continue;
            int idx = row * cols + col;
            if (counts[idx] == 0) {
                // record touched index (buffer is sized to 'total', so no growth needed)
                touched[touchedSize++] = idx;
            }
            counts[idx]++;
        }

        // prepare cached layout + cached hex path for this hex size
        if (cachedLayout == null || cachedHexSize != hexSize) {
            cachedLayout = HexLayout.of(hexSize);
            cachedHexSize = hexSize;
            cachedHexH = Math.sqrt(3) * hexSize;
            hexX = RendererAllocationCache.getFloatArray(this, "hex.x", 6);
            hexY = RendererAllocationCache.getFloatArray(this, "hex.y", 6);
        }

        // determine maximum count for color ramp
        int max = 1;
        for (int i = 0; i < touchedSize; i++) {
            int c = counts[touched[i]];
            if (c > max) max = c;
        }

        ArberColor base = seriesOrBase(model, context, 0);
        canvas.setStroke(getSeriesStrokeWidth());

        double[] ox = cachedLayout.getOffsetsX();
        double[] oy = cachedLayout.getOffsetsY();

        // Draw only touched cells
        for (int ti = 0; ti < touchedSize; ti++) {
            int idx = touched[ti];
            int cnt = counts[idx];
            if (cnt <= 0) continue;

            int r = idx / cols;
            int c = idx - r * cols;

            double cx = bx + c * hexSize * 1.5;
            double cy = by + r * cachedHexH + ((c % 2 == 0) ? 0 : cachedHexH / 2);

            if (cx + hexSize < bx || cx - hexSize > bx + width) continue;
            if (cy + hexSize < by || cy - hexSize > by + height) continue;

            float t = Math.min(1f, (float) cnt / (float) max);
            float alpha = 0.15f + 0.6f * t;
            ArberColor fill = isMultiColor() ? themeSeries(context, Math.min(ALPHA_LEVELS - 1, (int) (t * (ALPHA_LEVELS - 1)))) : base;
            if (fill == null) fill = base;
            canvas.setColor(ColorUtils.applyAlpha(fill, alpha));

            for (int i = 0; i < 6; i++) {
                hexX[i] = (float) (cx + ox[i]);
                hexY[i] = (float) (cy + oy[i]);
            }
            canvas.fillPolygon(hexX, hexY, 6);
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        return HitTestUtils.nearestPointIndex(pixel, model, context);
    }
}
