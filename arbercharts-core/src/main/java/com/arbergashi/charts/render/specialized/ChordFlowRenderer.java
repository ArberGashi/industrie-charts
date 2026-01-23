package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.LabelCache;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Chord flow renderer: grouped chord ribbons drawn with pooled Path2D and label caching.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class ChordFlowRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("chord_flow", new RendererDescriptor("chord_flow", "renderer.chord_flow", "/icons/chord_flow.svg"), ChordFlowRenderer::new);
    }

    private final transient Path2D.Double path = new Path2D.Double();
    private final transient Path2D.Double borderPath = new Path2D.Double();
    private final transient LabelCache labelCache = new LabelCache();

    // Group arrays (grow-on-demand)
    private String[] groupNames = new String[8];
    private double[] groupAngles = new double[8];
    private double[] groupSums = new double[8];
    private Color[] groupBaseColor = new Color[8];
    private Color[] groupFillColor = new Color[8];
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
    public java.util.Optional<Integer> getPointAt(java.awt.geom.Point2D pixel, com.arbergashi.charts.model.ChartModel model, com.arbergashi.charts.api.PlotContext context) {
        // Hit test using bounding boxes to avoid Shape allocations
        int count = this.lastPointCount;
        for (int i = 0; i < count; i++) {
            if (bboxW[i] <= 0 || bboxH[i] <= 0) continue;
            double x = bboxX[i], y = bboxY[i], w = bboxW[i], h = bboxH[i];
            if (pixel.getX() >= x && pixel.getX() <= x + w && pixel.getY() >= y && pixel.getY() <= y + h) {
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
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ChartTheme theme = resolveTheme(context);
        Rectangle2D bounds = context.plotBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double r = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.35;
        // compute a lightweight context hash for caching: bounds + point count + model name
        long ctxHash = 7L;
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.getX());
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.getY());
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.getWidth());
        ctxHash = ctxHash * 31 + Double.doubleToLongBits(bounds.getHeight());
        ctxHash = ctxHash * 31 + count;
        String n = model.getName();
        if (n != null) ctxHash = ctxHash * 31 + n.hashCode();

        // Reset groups
        groupCount = 0;
        // collect group names and aggregate weights using arrays (avoid maps)
        for (int idx = 0; idx < count; idx++) {
            String lbl = model.getLabel(idx);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            String s = parts[0];
            String t = parts[1];
            double w = model.getWeight(idx);
            if (w == 0.0) w = model.getMax(idx);

            // find or add source group
            int si = -1;
            for (int i = 0; i < groupCount; i++)
                if (groupNames[i] != null && groupNames[i].equals(s)) {
                    si = i;
                    break;
                }
            if (si == -1) {
                si = groupCount++;
                if (si >= groupNames.length) {
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
                    Color[] gbc = (Color[]) RendererAllocationCache.getArray(this, "groupBaseColor", Color.class, newCap);
                    System.arraycopy(groupBaseColor, 0, gbc, 0, groupBaseColor.length);
                    groupBaseColor = gbc;
                    Color[] gfc = (Color[]) RendererAllocationCache.getArray(this, "groupFillColor", Color.class, newCap);
                    System.arraycopy(groupFillColor, 0, gfc, 0, groupFillColor.length);
                    groupFillColor = gfc;
                }
                groupNames[si] = s;
                groupSums[si] = 0.0;
            }
            groupSums[si] += w;

            // find or add target group
            int ti = -1;
            for (int i = 0; i < groupCount; i++)
                if (groupNames[i] != null && groupNames[i].equals(t)) {
                    ti = i;
                    break;
                }
                if (ti == -1) {
                    ti = groupCount++;
                    if (ti >= groupNames.length) {
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
                        Color[] gbc = (Color[]) RendererAllocationCache.getArray(this, "groupBaseColor", Color.class, newCap);
                        System.arraycopy(groupBaseColor, 0, gbc, 0, groupBaseColor.length);
                        groupBaseColor = gbc;
                        Color[] gfc = (Color[]) RendererAllocationCache.getArray(this, "groupFillColor", Color.class, newCap);
                        System.arraycopy(groupFillColor, 0, gfc, 0, groupFillColor.length);
                        groupFillColor = gfc;
                    }
                    groupNames[ti] = t;
                    groupSums[ti] = 0.0;
                }
            groupSums[ti] += 0.0; // target's own weight is not incremented here
        }

        // assign angles evenly by groups and precompute colors
        int groups = Math.max(1, groupCount);
        double step = 2 * Math.PI / groups;
        Color baseColor = getSeriesColor(model);
        for (int i = 0; i < groupCount; i++) {
            groupAngles[i] = i * step;
            Color base = isMultiColor() ? theme.getSeriesColor(i) : baseColor;
            if (base == null) base = baseColor;
            groupBaseColor[i] = base;
            groupFillColor[i] = com.arbergashi.charts.util.ColorUtils.withAlpha(base, 0.55f);
        }

        // determine maximum single-flow weight for scaling stroke width
        double maxWeight = 1.0;
        for (int i = 0; i < count; i++) {
            double w = model.getWeight(i);
            if (w == 0.0) w = model.getMax(i);
            if (Double.isFinite(w)) maxWeight = Math.max(maxWeight, Math.abs(w));
        }

        // precompute group colors (HSB cycle)
        // draw ribbons as filled translucent stroked shapes between group arcs
        boolean rebuild = (lastContextHash != ctxHash) || bboxX.length < count;
        if (rebuild) {
            // ensure bbox arrays capacity
            int need = count;
            if (bboxX.length < need) {
                int newCap = Math.max(need, bboxX.length * 2);
                double[] nx = RendererAllocationCache.getDoubleArray(this, "bboxX", newCap);
                System.arraycopy(bboxX, 0, nx, 0, bboxX.length);
                bboxX = nx;
                double[] ny = RendererAllocationCache.getDoubleArray(this, "bboxY", newCap);
                System.arraycopy(bboxY, 0, ny, 0, bboxY.length);
                bboxY = ny;
                double[] nw = RendererAllocationCache.getDoubleArray(this, "bboxW", newCap);
                System.arraycopy(bboxW, 0, nw, 0, bboxW.length);
                bboxW = nw;
                double[] nh = RendererAllocationCache.getDoubleArray(this, "bboxH", newCap);
                System.arraycopy(bboxH, 0, nh, 0, bboxH.length);
                bboxH = nh;
            }
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
            String s = parts[0];
            String t = parts[1];
            int si = -1, ti = -1;
            for (int j = 0; j < groupCount; j++) {
                if (groupNames[j] != null && groupNames[j].equals(s)) si = j;
                if (groupNames[j] != null && groupNames[j].equals(t)) ti = j;
                if (si != -1 && ti != -1) break;
            }
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

            path.reset();
            path.moveTo(x1, y1);
            double mx = cx + (r * 0.22) * Math.cos((a1 + a2) / 2.0);
            double my = cy + (r * 0.22) * Math.sin((a1 + a2) / 2.0);
            path.quadTo(mx, my, x2, y2);

            double w = model.getWeight(i);
            if (w == 0.0) w = model.getMax(i);

            Color base = groupBaseColor[si];
            Color fill = groupFillColor[si];

            // build a closed ribbon polygon: outer quad curve + inner quad curve back
            double innerR = Math.max(6.0, r * 0.6); // inner radius for the return curve (prevents zero-size)

            double xi1 = cx + innerR * Math.cos(a1);
            double yi1 = cy + innerR * Math.sin(a1);
            double xi2 = cx + innerR * Math.cos(a2);
            double yi2 = cy + innerR * Math.sin(a2);

            double mxOuter = cx + (r * 0.22) * Math.cos((a1 + a2) / 2.0);
            double myOuter = cy + (r * 0.22) * Math.sin((a1 + a2) / 2.0);
            double mxInner = cx + (innerR * 0.22) * Math.cos((a1 + a2) / 2.0);
            double myInner = cy + (innerR * 0.22) * Math.sin((a1 + a2) / 2.0);

            // Use cached path for ribbon
            path.moveTo(x1, y1);
            path.quadTo(mxOuter, myOuter, x2, y2);
            path.lineTo(xi2, yi2);
            path.quadTo(mxInner, myInner, xi1, yi1);
            path.closePath();

            g2.setColor(fill);
            g2.fill(path);

            // draw soft border around outer edge
            g2.setColor(base.darker());
            Stroke old = g2.getStroke();
            g2.setStroke(getCachedStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            borderPath.reset();
            borderPath.moveTo(x1, y1);
            borderPath.quadTo(mxOuter, myOuter, x2, y2);
            g2.draw(borderPath);
            g2.setStroke(old);

            // store bounding box for hit-testing
            double minx = Math.min(Math.min(x1, x2), Math.min(xi1, xi2));
            double maxx = Math.max(Math.max(x1, x2), Math.max(xi1, xi2));
            double miny = Math.min(Math.min(y1, y2), Math.min(yi1, yi2));
            double maxy = Math.max(Math.max(y1, y2), Math.max(yi1, yi2));
            bboxX[ptIndex] = minx;
            bboxY[ptIndex] = miny;
            bboxW[ptIndex] = maxx - minx;
            bboxH[ptIndex] = maxy - miny;

            // highlight on hover
            if (hoveredIndex == ptIndex) {
                g2.setColor(com.arbergashi.charts.util.ColorUtils.withAlpha(theme.getForeground(), 0.35f));
                g2.fill(path);
                g2.setColor(theme.getForeground());
                g2.setStroke(getCachedStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(borderPath);
                g2.setStroke(old);
            }
            ptIndex++;
        }

        // draw group nodes and labels with theme-aware text color
        Color labelColor = theme.getForeground();
        // draw group nodes and labels using arrays
        for (int gi = 0; gi < groupCount; gi++) {
            double ang = groupAngles[gi];
            double x = cx + r * Math.cos(ang);
            double y = cy + r * Math.sin(ang);
            g2.setColor(theme.getGridColor());
            g2.fill(getEllipse(x - 6, y - 6, 12, 12));
            labelCache.drawLabel(g2, groupNames[gi], g2.getFont(), labelColor, (float) x + 8f, (float) y + 8f);
        }

        // draw a compact legend with group sums on top-left of bounds
        int lx = Math.round((float) (bounds.getX() + com.arbergashi.charts.util.ChartScale.scale(8)));
        int ly = Math.round((float) (bounds.getY() + com.arbergashi.charts.util.ChartScale.scale(8)));
        int box = Math.round(com.arbergashi.charts.util.ChartScale.scale(10));
        int gap = Math.round(com.arbergashi.charts.util.ChartScale.scale(6));
        int lineH = Math.max(box, g2.getFontMetrics().getHeight());
        for (int gi = 0; gi < groupCount; gi++) {
            String gname = groupNames[gi];
            Color gc = groupBaseColor[gi] != null ? groupBaseColor[gi] : theme.getGridColor();
            double sum = groupSums[gi];
            String text = String.format("%s: %.0f", gname, sum);
            int yoff = ly + gi * (lineH + gap);
            g2.setColor(gc);
            g2.fillRect(lx, yoff, box, box);
            g2.setColor(labelColor);
            g2.drawString(text, lx + box + gap, yoff + g2.getFontMetrics().getAscent());
            // limit legend size to avoid overflow
            if (gi > 8) break;
        }
        // update cached context hash and last point count
        lastContextHash = ctxHash;
        // lastPointCount indicates how many bboxes are valid (ptIndex)
        this.lastPointCount = ptIndex;
    }
}
