package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.*;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Optional;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Hexbin renderer: maps points into a hexagonal bin grid and draws density.
 * Implementation focuses on minimizing allocations in the draw path.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
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
    private transient Path2D cachedHexPath;
    private transient AffineTransform cachedTx;
    private transient double cachedHexH;
    private transient int alphaKey;
    private transient Composite[] alphaLut;

    public HexbinRenderer() {
        super("hexbin");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Rectangle clip = g2.getClipBounds();
        Rectangle bounds = context.plotBounds().getBounds();
        if (clip != null && !clip.intersects(bounds)) return;

        double width = bounds.getWidth();
        double height = bounds.getHeight();

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
        double bx = bounds.getX();
        double by = bounds.getY();
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
            cachedLayout = HexLayout.create(hexSize);
            cachedHexSize = hexSize;
            cachedHexH = Math.sqrt(3) * hexSize;

            // Build a single hexagon path around origin (0,0).
            double[] ox = cachedLayout.getOffsetsX();
            double[] oy = cachedLayout.getOffsetsY();
            Path2D p = RendererAllocationCache.getPath(this, "cachedHexPath");
            p.moveTo(ox[0], oy[0]);
            for (int i = 1; i < 6; i++) {
                p.lineTo(ox[i], oy[i]);
            }
            p.closePath();
            cachedHexPath = p;
            cachedTx = RendererAllocationCache.getAffineTransform(this, "cachedTx");
        }

        // determine maximum count for color ramp
        int max = 1;
        for (int i = 0; i < touchedSize; i++) {
            int c = counts[touched[i]];
            if (c > max) max = c;
        }

        Stroke prevStroke = g2.getStroke();
        Composite prevComposite = g2.getComposite();
        Color prevColor = g2.getColor();
        Object prevAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object prevStrokeControl = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        AffineTransform prevTransform = g2.getTransform();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color base = seriesOrBase(model, context, 0);
        g2.setStroke(getSeriesStroke());
        g2.setColor(base);

        ensureAlphaLut();

        // Draw only touched cells
        for (int ti = 0; ti < touchedSize; ti++) {
            int idx = touched[ti];
            int cnt = counts[idx];
            if (cnt <= 0) continue;

            int r = idx / cols;
            int c = idx - r * cols;

            double cx = bx + c * hexSize * 1.5;
            double cy = by + r * cachedHexH + ((c % 2 == 0) ? 0 : cachedHexH / 2);

            if (clip != null) {
                if (cx + hexSize < clip.getX() || cx - hexSize > clip.getX() + clip.getWidth()) continue;
                if (cy + hexSize < clip.getY() || cy - hexSize > clip.getY() + clip.getHeight()) continue;
            }

            float t = Math.min(1f, (float) cnt / (float) max);
            int ai = Math.min(ALPHA_LEVELS - 1, (int) (t * (ALPHA_LEVELS - 1)));
            g2.setComposite(alphaLut[ai]);
            Color fill = isMultiColor() ? themeSeries(context, ai) : base;
            if (fill == null) fill = base;
            g2.setColor(fill);

            cachedTx.setToTranslation(cx, cy);
            g2.setTransform(cachedTx);
            g2.fill(cachedHexPath);
        }

        g2.setTransform(prevTransform);
        g2.setColor(prevColor);
        g2.setComposite(prevComposite);
        g2.setStroke(prevStroke);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, prevStrokeControl);
    }

    private void ensureAlphaLut() {
        int key = System.identityHashCode(UIManager.getDefaults());
        if (alphaLut != null && alphaKey == key) return;
        alphaKey = key;
        Composite[] lut = new Composite[ALPHA_LEVELS];
        for (int i = 0; i < ALPHA_LEVELS; i++) {
            float a = 0.15f + 0.6f * (i / (float) (ALPHA_LEVELS - 1));
            lut[i] = CompositePool.get(a);
        }
        alphaLut = lut;
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        return HitTestUtils.nearestPointIndex(pixel, model, context);
    }
}
