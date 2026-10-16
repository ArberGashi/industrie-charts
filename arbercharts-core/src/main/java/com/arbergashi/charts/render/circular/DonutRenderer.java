package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.tools.RendererAllocationCache;
import java.util.Optional;
/**
 * <h2>Modern Donut Renderer</h2>
 * <p>
 * Draws an interactive donut chart with measured, customizable center text.
 * Drawing buffers are reused; text layout, font fitting and aggregation may allocate.
 * Center text is fitted into the inner circle. When both lines cannot fit at a
 * readable size, the primary line takes precedence; very small holes omit text.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Center Content:</b> API to display text (e.g., total, title) in the center hole.</li>
 *     <li><b>Smart Labels:</b> Inherits intelligent external labeling from {@link PieRenderer}.</li>
 *     <li><b>Hover Effect:</b> Inherits segment highlighting from {@link PieRenderer}.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @since 2025-06-01
 *
 */
public final class DonutRenderer extends PieRenderer {

    private static final double INNER_FACTOR = 0.55;
    private static final int INNER_SEGMENTS = 48;
    private static final float MIN_CENTER_FONT_SIZE = 8f;
    private static final ArberFont DEFAULT_CENTER_FONT = new ArberFont("SansSerif", ArberFont.PLAIN, 12f);

    private double lastCx, lastCy, lastOuterR;
    private String centerText;
    private String centerSubText;
    
    public DonutRenderer() {
        super("donut");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        super.drawData(canvas, model, context);

        ArberRect plot = context.getPlotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        double diameter = Math.min(plot.getWidth(), plot.getHeight()) * 0.7;
        if (diameter <= 1) return;

        double cx = plot.centerX();
        double cy = plot.centerY();

        lastCx = cx;
        lastCy = cy;
        lastOuterR = diameter * 0.5;

        double innerDiameter = diameter * INNER_FACTOR;
        if (innerDiameter <= 0) return;

        ChartTheme t = getResolvedTheme(context);
        ArberColor bg = t.getBackground();
        canvas.setColor(bg);
        drawCircleFill(canvas, cx, cy, innerDiameter / 2.0);
        drawCenterText(canvas, cx, cy, innerDiameter, t);
    }

    private void drawCenterText(ArberCanvas canvas, double cx, double cy, double innerDiameter, ChartTheme theme) {
        boolean main = centerText != null && !centerText.isBlank();
        boolean sub = centerSubText != null && !centerSubText.isBlank();
        if (!main && !sub) return;

        String firstLine = main ? centerText : centerSubText;
        String secondLine = main && sub ? centerSubText : null;
        int lines = secondLine == null ? 1 : 2;
        double radius = innerDiameter * 0.5 * Math.cos(Math.PI / INNER_SEGMENTS)
                - Math.min(2.0, innerDiameter * 0.05);
        if (!(radius > 0)) return;

        ArberFont font = theme.getBaseFont() == null ? DEFAULT_CENTER_FONT : theme.getBaseFont();
        float minimumSize = Math.min(MIN_CENTER_FONT_SIZE, font.size());
        double heightLimit = radius * Math.sqrt(2.0);
        boolean fits = false;
        for (int attempt = 0; attempt < 8; attempt++) {
            canvas.setFont(font);
            double height = lines * canvas.getTextLineHeight();
            if (height <= heightLimit) {
                fits = true;
                break;
            }
            if (font.size() > minimumSize) {
                float size = Math.max(minimumSize, (float) (font.size() * heightLimit / height * 0.95));
                font = new ArberFont(font.name(), font.style(), size);
            } else if (lines == 2) {
                // Preserve the primary value when two readable lines cannot fit.
                secondLine = null;
                lines = 1;
            } else {
                return;
            }
        }
        if (!fits) return;

        double lineHeight = canvas.getTextLineHeight();
        double blockHeight = lines * lineHeight;
        double width = 2 * Math.sqrt(radius * radius - blockHeight * blockHeight / 4);
        double top = cy - blockHeight / 2;
        double baseline = top + canvas.getTextAscent();
        double left = cx - width / 2;
        double right = cx + width / 2;
        double bottom = top + blockHeight;
        ArberRect previousClip = canvas.getClip();
        if (previousClip != null) {
            left = Math.max(left, previousClip.minX());
            right = Math.min(right, previousClip.maxX());
            top = Math.max(top, previousClip.minY());
            bottom = Math.min(bottom, previousClip.maxY());
        }
        if (left >= right || top >= bottom) return;
        // The inscribed clip also contains glyph overhang beyond measured advance.
        canvas.setClip(new ArberRect(left, top, right - left, bottom - top));
        canvas.setColor(theme.getForeground());
        try {
            String text = fitLabel(canvas, firstLine, width);
            canvas.drawText((float) (cx - canvas.measureTextWidth(text) / 2), (float) baseline, text);
            if (secondLine != null) {
                text = fitLabel(canvas, secondLine, width);
                canvas.drawText((float) (cx - canvas.measureTextWidth(text) / 2), (float) (baseline + lineHeight), text);
            }
        } finally {
            canvas.setClip(previousClip);
        }
    }


    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (lastOuterR <= 0) return Optional.empty();

        double dx = pixel.x() - lastCx;
        double dy = pixel.y() - lastCy;
        double d2 = dx * dx + dy * dy;

        double innerR = lastOuterR * INNER_FACTOR;
        if (d2 < innerR * innerR) {
            setHoverIndex(-1);
            return Optional.empty();
        }

        return super.getPointAt(pixel, model, context);
    }

    // --- Public API for Center Content ---

    public DonutRenderer setCenterText(String text){
        this.centerText = text;
        return this;
        
    }

    public DonutRenderer setCenterSubText(String text){
        this.centerSubText = text;
        return this;
        
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = INNER_SEGMENTS;
        float[] xs = RendererAllocationCache.getFloatArray(this, "donut.inner.x", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "donut.inner.y", segments);
        for (int i = 0; i < segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }
}
