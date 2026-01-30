package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.api.types.ArberPoint;

/**
 * Chord flow renderer: grouped chord ribbons drawn with polyline approximations.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ChordFlowRenderer extends BaseRenderer {

    static {
        RendererRegistry.register(
                "chord_flow",
                new RendererDescriptor("chord_flow", "renderer.chord_flow", "/icons/chord_flow.svg"),
                ChordFlowRenderer::new
        );
    }

    // Group arrays (grow-on-demand)
    private String[] groupNames = new String[8];
    private double[] groupAngles = new double[8];
    private double[] groupSums = new double[8];
    private ArberColor[] groupBaseColor = new ArberColor[8];
    private ArberColor[] groupFillColor = new ArberColor[8];
    private int groupCount = 0;

    // Bounding boxes for hit-detection (per-point)
    private double[] bboxX = new double[128];
    private double[] bboxY = new double[128];
    private double[] bboxW = new double[128];
    private double[] bboxH = new double[128];

    private transient long lastContextHash = -1;
    private transient int hoveredIndex = -1;
    private int lastPointCount = 0;

    public ChordFlowRenderer() {
        super("chord_flow");
    }

    @Override
    public java.util.Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int count = this.lastPointCount;
        for (int i = 0; i < count; i++) {
            if (bboxW[i] <= 0 || bboxH[i] <= 0) continue;
            double x = bboxX[i], y = bboxY[i], w = bboxW[i], h = bboxH[i];
            if (pixel.x() >= x && pixel.x() <= x + w && pixel.y() >= y && pixel.y() <= y + h) {
                hoveredIndex = i;
                return java.util.Optional.of(i);
            }
        }
        hoveredIndex = -1;
        return java.util.Optional.empty();
    }

    @Override
    public void clearHover() {
        hoveredIndex = -1;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.x() + bounds.width() * 0.5;
        double cy = bounds.y() + bounds.height() * 0.5;
        double r = Math.min(bounds.width(), bounds.height()) * 0.35;

        long ctxHash = 7L;
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.x());
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.y());
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.width());
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.height());
        ctxHash = ctxHash * 31 + count;
        String n = model.getName();
        if (n != null) ctxHash = ctxHash * 31 + n.hashCode();

        groupCount = 0;
        for (int idx = 0; idx < count; idx++) {
            String lbl = model.getLabel(idx);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            String s = parts[0];
            String t = parts[1];
            double w = model.getWeight(idx);
            if (w == 0.0) w = model.getMax(idx);

            int si = findOrAddGroup(s);
            groupSums[si] += w;

            int ti = findOrAddGroup(t);
            groupSums[ti] += 0.0;
        }

        int groups = Math.max(1, groupCount);
        double step = 2 * Math.PI / groups;
        ArberColor baseColor = getSeriesColor(model);
        for (int i = 0; i < groupCount; i++) {
            groupAngles[i] = i * step;
            ArberColor base = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (base == null) base = baseColor;
            groupBaseColor[i] = base;
            groupFillColor[i] = ColorRegistry.applyAlpha(base, 0.55f);
        }

        boolean rebuild = (lastContextHash != ctxHash) || bboxX.length < count;
        if (rebuild && bboxX.length < count) {
            int newCap = Math.max(count, bboxX.length * 2);
            bboxX = RendererAllocationCache.getDoubleArray(this, "bboxX", newCap);
            bboxY = RendererAllocationCache.getDoubleArray(this, "bboxY", newCap);
            bboxW = RendererAllocationCache.getDoubleArray(this, "bboxW", newCap);
            bboxH = RendererAllocationCache.getDoubleArray(this, "bboxH", newCap);
        }

        int ptIndex = 0;
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) {
                ptIndex++;
                continue;
            }
            String[] parts = lbl.split(":");
            if (parts.length < 2) {
                ptIndex++;
                continue;
            }
            int si = indexOfGroup(parts[0]);
            int ti = indexOfGroup(parts[1]);
            if (si == -1 || ti == -1) {
                ptIndex++;
                continue;
            }

            double a1 = groupAngles[si];
            double a2 = groupAngles[ti];
            double x1 = cx + r * Math.cos(a1);
            double y1 = cy + r * Math.sin(a1);
            double x2 = cx + r * Math.cos(a2);
            double y2 = cy + r * Math.sin(a2);

            double innerR = Math.max(6.0, r * 0.6);
            double xi1 = cx + innerR * Math.cos(a1);
            double yi1 = cy + innerR * Math.sin(a1);
            double xi2 = cx + innerR * Math.cos(a2);
            double yi2 = cy + innerR * Math.sin(a2);

            double mxOuter = cx + (r * 0.22) * Math.cos((a1 + a2) / 2.0);
            double myOuter = cy + (r * 0.22) * Math.sin((a1 + a2) / 2.0);
            double mxInner = cx + (innerR * 0.22) * Math.cos((a1 + a2) / 2.0);
            double myInner = cy + (innerR * 0.22) * Math.sin((a1 + a2) / 2.0);

            ArberColor fill = groupFillColor[si];
            canvas.setColor(fill);
            fillRibbon(canvas, x1, y1, mxOuter, myOuter, x2, y2, xi2, yi2, mxInner, myInner, xi1, yi1);

            if (hoveredIndex == ptIndex) {
                canvas.setColor(ColorRegistry.applyAlpha(themeForeground(context), 0.35f));
                fillRibbon(canvas, x1, y1, mxOuter, myOuter, x2, y2, xi2, yi2, mxInner, myInner, xi1, yi1);
            }

            double minx = Math.min(Math.min(x1, x2), Math.min(xi1, xi2));
            double maxx = Math.max(Math.max(x1, x2), Math.max(xi1, xi2));
            double miny = Math.min(Math.min(y1, y2), Math.min(yi1, yi2));
            double maxy = Math.max(Math.max(y1, y2), Math.max(yi1, yi2));
            bboxX[ptIndex] = minx;
            bboxY[ptIndex] = miny;
            bboxW[ptIndex] = maxx - minx;
            bboxH[ptIndex] = maxy - miny;

            ptIndex++;
        }

        lastContextHash = ctxHash;
        this.lastPointCount = ptIndex;
    }

    private void fillRibbon(ArberCanvas canvas,
                            double x1, double y1, double mx1, double my1, double x2, double y2,
                            double xi2, double yi2, double mx2, double my2, double xi1, double yi1) {
        int steps = 16;
        int count = (steps + 1) * 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "chord.ribbon.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "chord.ribbon.y", count);
        int idx = 0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double inv = 1.0 - t;
            double x = inv * inv * x1 + 2.0 * inv * t * mx1 + t * t * x2;
            double y = inv * inv * y1 + 2.0 * inv * t * my1 + t * t * y2;
            xs[idx] = (float) x;
            ys[idx] = (float) y;
            idx++;
        }
        for (int i = 0; i <= steps; i++) {
            double t = (double) (steps - i) / (double) steps;
            double inv = 1.0 - t;
            double x = inv * inv * xi2 + 2.0 * inv * t * mx2 + t * t * xi1;
            double y = inv * inv * yi2 + 2.0 * inv * t * my2 + t * t * yi1;
            xs[idx] = (float) x;
            ys[idx] = (float) y;
            idx++;
        }
        canvas.fillPolygon(xs, ys, idx);
    }

    private int findOrAddGroup(String name) {
        int idx = indexOfGroup(name);
        if (idx != -1) return idx;
        idx = groupCount++;
        if (idx >= groupNames.length) {
            int newCap = groupNames.length * 2;
            String[] gn = (String[]) RendererAllocationCache.getArray(this, "groupNames", String.class, newCap);
            System.arraycopy(groupNames, 0, gn, 0, groupNames.length);
            groupNames = gn;
            double[] ga = RendererAllocationCache.getDoubleArray(this, "groupAngles", newCap);
            System.arraycopy(groupAngles, 0, ga, 0, groupAngles.length);
            groupAngles = ga;
            double[] gs = RendererAllocationCache.getDoubleArray(this, "groupSums", newCap);
            System.arraycopy(groupSums, 0, gs, 0, groupSums.length);
            groupSums = gs;
            ArberColor[] gbc = (ArberColor[]) RendererAllocationCache.getArray(this, "groupBaseColor", ArberColor.class, newCap);
            System.arraycopy(groupBaseColor, 0, gbc, 0, groupBaseColor.length);
            groupBaseColor = gbc;
            ArberColor[] gfc = (ArberColor[]) RendererAllocationCache.getArray(this, "groupFillColor", ArberColor.class, newCap);
            System.arraycopy(groupFillColor, 0, gfc, 0, groupFillColor.length);
            groupFillColor = gfc;
        }
        groupNames[idx] = name;
        groupSums[idx] = 0.0;
        return idx;
    }

    private int indexOfGroup(String name) {
        for (int i = 0; i < groupCount; i++) {
            if (groupNames[i] != null && groupNames[i].equals(name)) return i;
        }
        return -1;
    }
}
