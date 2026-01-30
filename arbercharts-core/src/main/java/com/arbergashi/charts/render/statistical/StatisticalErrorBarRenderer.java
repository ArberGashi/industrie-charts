package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Statistical Error Bar Renderer (headless).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public class StatisticalErrorBarRenderer extends BaseRenderer {

    private final double[] centerBuf = new double[2];
    private final double[] topBuf = new double[2];
    private final double[] bottomBuf = new double[2];
    private final double[] leftBuf = new double[2];
    private final double[] rightBuf = new double[2];
    private boolean showVertical = true;
    private boolean showHorizontal = false;
    private float capWidth = 8.0f;

    public StatisticalErrorBarRenderer() {
        super("errorbar");
    }

    public StatisticalErrorBarRenderer setShowVertical(boolean showVertical) {
        this.showVertical = showVertical;
        return this;
    }

    public StatisticalErrorBarRenderer setShowHorizontal(boolean showHorizontal) {
        this.showHorizontal = showHorizontal;
        return this;
    }

    public StatisticalErrorBarRenderer setCapWidth(float capWidth) {
        this.capWidth = capWidth;
        return this;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        canvas.setStroke(ChartScale.scale(1.0f));

        double scaledCap = ChartScale.scale(capWidth) / 2.0;
        final ArberColor color = getSeriesColor(model);
        int n = model.getPointCount();
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            context.mapToPixel(x, y, centerBuf);
            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : color;
            if (pointColor == null) pointColor = color;
            canvas.setColor(pointColor);

            if (showVertical) {
                context.mapToPixel(x, model.getMax(i), topBuf);
                context.mapToPixel(x, model.getMin(i), bottomBuf);

                drawLine(canvas, centerBuf[0], topBuf[1], centerBuf[0], bottomBuf[1]);
                drawLine(canvas, centerBuf[0] - scaledCap, topBuf[1], centerBuf[0] + scaledCap, topBuf[1]);
                drawLine(canvas, centerBuf[0] - scaledCap, bottomBuf[1], centerBuf[0] + scaledCap, bottomBuf[1]);
            }

            if (showHorizontal) {
                double errX = model.getWeight(i);
                if (errX > 0) {
                    context.mapToPixel(x - errX, y, leftBuf);
                    context.mapToPixel(x + errX, y, rightBuf);

                    drawLine(canvas, leftBuf[0], centerBuf[1], rightBuf[0], centerBuf[1]);
                    drawLine(canvas, leftBuf[0], centerBuf[1] - scaledCap, leftBuf[0], centerBuf[1] + scaledCap);
                    drawLine(canvas, rightBuf[0], centerBuf[1] - scaledCap, rightBuf[0], centerBuf[1] + scaledCap);
                }
            }

            double size = ChartScale.scale(8.0);
            canvas.setColor(ColorUtils.applyAlpha(pointColor, 0.5f));
            fillCircle(canvas, centerBuf[0], centerBuf[1], size);

            canvas.setColor(pointColor);
            fillCircle(canvas, centerBuf[0], centerBuf[1], size / 2);

            canvas.setColor(themeBackground(context));
            fillCircle(canvas, centerBuf[0], centerBuf[1], size / 4);

            canvas.setColor(pointColor);
        }
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "serr.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "serr.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double d) {
        double r = d / 2.0;
        int segments = 10;
        float[] xs = RendererAllocationCache.getFloatArray(this, "serr.cx", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "serr.cy", segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }
}
