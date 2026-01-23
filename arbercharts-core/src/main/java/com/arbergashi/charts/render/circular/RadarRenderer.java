package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * <h1>Modern Radar/Spider Renderer</h1>
 * <p>
 * Draws a professional, interactive radar chart with a spider-web grid, filled area, and hover effects,
 * adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Volumetric Fill:</b> The chart area is filled with a transparent gradient for better visualization.</li>
 *     <li><b>Data Nodes:</b> Each data point is highlighted with a distinct node.</li>
 *     <li><b>Hover Effect:</b> Nodes and the chart outline glow on mouse hover.</li>
 *     <li><b>Smart Labels:</b> Axis labels are automatically positioned for readability.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class RadarRenderer extends BaseRenderer {

    private int hoverIndex = -1;

    public RadarRenderer() {
        super("radar");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 3) return;
        
        Rectangle2D b = context.plotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;
        
        double angleStep = 360.0 / n;
        Color accent = getSeriesColor(model);

        drawSpiderWeb(g, context, n, angleStep, model);
        drawRadarPath(g, model, context, n, angleStep, accent);
        drawDataNodes(g, model, context, n, angleStep, accent);
    }

    private void drawSpiderWeb(Graphics2D g, PlotContext context, int segments, double angleStep, ChartModel model) {
        ChartTheme theme = resolveTheme(context);
        g.setColor(theme.getGridColor());
        g.setStroke(getCachedStroke(1.0f));

        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        // Draw concentric polygons (grid rings)
        int ringCount = 5;
        for (int i = 1; i <= ringCount; i++) {
            double radius = maxRadius * ((double) i / ringCount);
            Path2D ring = getPathCache();
            ring.reset();
            for (int s = 0; s < segments; s++) {
                double angleRad = Math.toRadians(s * angleStep - 90); // Start from top
                mapPolarToCartesian(cx, cy, radius, angleRad, pBuffer());
                if (s == 0) ring.moveTo(pBuffer()[0], pBuffer()[1]);
                else ring.lineTo(pBuffer()[0], pBuffer()[1]);
            }
            ring.closePath();
            g.draw(ring);
        }

        // Draw radial axes and labels
        Font font = getCachedFont(10f, Font.PLAIN);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < segments; i++) {
            double angleRad = Math.toRadians(i * angleStep - 90);
            mapPolarToCartesian(cx, cy, maxRadius, angleRad, pBuffer());
            g.draw(getLine(cx, cy, pBuffer()[0], pBuffer()[1]));

            String label = model.getLabel(i);
            if (label != null && !label.isEmpty()) {
                double labelRadius = maxRadius + ChartScale.scale(10);
                mapPolarToCartesian(cx, cy, labelRadius, angleRad, pBuffer());
                float tx = (float) pBuffer()[0];
                float ty = (float) pBuffer()[1];

                // Adjust alignment based on quadrant
                if (Math.abs(tx - cx) < 1) { // Top/Bottom
                    tx -= fm.stringWidth(label) / 2.0f;
                } else if (tx < cx) { // Left
                    tx -= fm.stringWidth(label);
                }
                if (Math.abs(ty - cy) > 1 && ty > cy) { // Bottom half
                    ty += fm.getAscent() / 2f;
                }

                g.setColor(theme.getAxisLabelColor());
                g.drawString(label, tx, ty);
            }
        }
    }


    private void drawRadarPath(Graphics2D g, ChartModel model, PlotContext context, int n, double angleStep, Color accent) {
        Path2D path = getPathCache();
        path.reset();
        
        for (int i = 0; i < n; i++) {
            mapModelToCartesian(i, model, context, angleStep, pBuffer());
            if (i == 0) path.moveTo(pBuffer()[0], pBuffer()[1]);
            else path.lineTo(pBuffer()[0], pBuffer()[1]);
        }
        path.closePath();

        // Fill
        g.setColor(ColorUtils.withAlpha(accent, 0.25f));
        g.fill(path);

        // Stroke
        float strokeWidth = (hoverIndex != -1) ? 2.5f : 1.5f;
        Color strokeColor = (hoverIndex != -1) ? accent.brighter() : accent;
        g.setStroke(getCachedStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(strokeColor);
        g.draw(path);
    }

    private void drawDataNodes(Graphics2D g, ChartModel model, PlotContext context, int n, double angleStep, Color accent) {
        for (int i = 0; i < n; i++) {
            mapModelToCartesian(i, model, context, angleStep, pBuffer());
            
            double nodeRadius = (i == hoverIndex) ? ChartScale.scale(5f) : ChartScale.scale(3f);
            g.setColor(accent);
            g.fill(getEllipse(pBuffer()[0] - nodeRadius, pBuffer()[1] - nodeRadius, nodeRadius * 2, nodeRadius * 2));
            g.setColor(themeBackground(context));
            g.fill(getEllipse(pBuffer()[0] - nodeRadius/2, pBuffer()[1] - nodeRadius/2, nodeRadius, nodeRadius));
        }
    }
    
    private void mapPolarToCartesian(double cx, double cy, double radius, double angleRad, double[] dest) {
        dest[0] = cx + Math.cos(angleRad) * radius;
        dest[1] = cy + Math.sin(angleRad) * radius;
    }
    
    private void mapModelToCartesian(int index, ChartModel model, PlotContext context, double angleStep, double[] dest) {
        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;
        
        double value = model.getY(index);
        double radius = maxRadius * (value / context.maxY());
        double angleRad = Math.toRadians(index * angleStep - 90);
        
        mapPolarToCartesian(cx, cy, radius, angleRad, dest);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 3) return Optional.empty();

        double angleStep = 360.0 / n;
        double thresholdSq = Math.pow(ChartScale.scale(10), 2);

        for (int i = 0; i < n; i++) {
            mapModelToCartesian(i, model, context, angleStep, pBuffer());
            
            double dx = pixel.getX() - pBuffer()[0];
            double dy = pixel.getY() - pBuffer()[1];

            if ((dx * dx + dy * dy) < thresholdSq) {
                this.hoverIndex = i;
                return Optional.of(i);
            }
        }
        
        this.hoverIndex = -1;
        return Optional.empty();
    }
    
    @Override
    public void clearHover() {
        this.hoverIndex = -1;
    }
}
