package com.arbergashi.charts.visualverifier;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.financial.VolumeRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.NiceScale;

import java.util.Optional;

final class FinancialDashboardRenderer implements ChartRenderer {
    private final CandlestickRenderer candlestick = new CandlestickRenderer();
    private final VolumeRenderer volume = new VolumeRenderer();
    private final NiceScale yScale = new NiceScale(0, 1);
    private final NiceScale xScale = new NiceScale(0, 1);
    private final double[] px = new double[2];

    @Override
    public void render(ArberCanvas canvas, ChartModel model, PlotContext context) {
        ArberRect bounds = context.getPlotBounds();
        double gap = Math.max(6, bounds.height() * 0.03);
        double mainHeight = bounds.height() * 0.72;
        double volumeHeight = bounds.height() - mainHeight - gap;

        ArberRect mainBounds = new ArberRect(bounds.x(), bounds.y(), bounds.width(), mainHeight);
        ArberRect volumeBounds = new ArberRect(bounds.x(), bounds.y() + mainHeight + gap, bounds.width(), volumeHeight);

        DefaultPlotContext mainCtx = new DefaultPlotContext(
                mainBounds,
                context.getMinX(),
                context.getMaxX(),
                context.getMinY(),
                context.getMaxY(),
                false,
                false,
                false,
                context.getScaleModeX(),
                context.getScaleModeY(),
                context.getTheme(),
                context.getRenderHints(),
                context.getGapModel(),
                context.getAnimationProfile()
        );

        double maxVolume = getMaxVolume(model);
        DefaultPlotContext volumeCtx = new DefaultPlotContext(
                volumeBounds,
                context.getMinX(),
                context.getMaxX(),
                0,
                Math.max(1.0, maxVolume * 1.12),
                false,
                false,
                false,
                context.getScaleModeX(),
                context.getScaleModeY(),
                context.getTheme(),
                context.getRenderHints(),
                context.getGapModel(),
                context.getAnimationProfile()
        );

        renderGrid(canvas, mainCtx);
        candlestick.render(canvas, model, mainCtx);
        volume.render(canvas, model, volumeCtx);
        renderSeparator(canvas, bounds, mainBounds.maxY(), context.getTheme());
        renderAxes(canvas, mainCtx, volumeBounds.maxY());
        renderCrosshair(canvas, model, mainCtx, bounds);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "financial-dashboard";
    }

    private void renderGrid(ArberCanvas canvas, PlotContext context) {
        ChartTheme theme = context.getTheme();
        ArberColor grid = ColorRegistry.applyAlpha(theme.getGridColor(), 0.22f);
        ArberRect bounds = context.getPlotBounds();
        yScale.setRange(context.getMinY(), context.getMaxY()).setMaxTicks(6);
        canvas.setColor(grid);
        canvas.setStroke(ChartScale.scale(0.7f));
        for (double tick : yScale.getTicks()) {
            context.mapToPixel(context.getMinX(), tick, px);
            float y = (float) context.snapPixel(px[1]);
            canvas.drawLine((float) bounds.x(), y, (float) bounds.maxX(), y);
        }

        xScale.setRange(context.getMinX(), context.getMaxX()).setMaxTicks(6);
        for (double tick : xScale.getTicks()) {
            context.mapToPixel(tick, context.getMinY(), px);
            float x = (float) context.snapPixel(px[0]);
            canvas.drawLine(x, (float) bounds.y(), x, (float) bounds.maxY());
        }
    }

    private void renderSeparator(ArberCanvas canvas, ArberRect bounds, double y, ChartTheme theme) {
        ArberColor axis = theme != null ? theme.getAxisLabelColor() : ColorRegistry.of(140, 160, 180, 255);
        canvas.setColor(ColorRegistry.applyAlpha(axis, 0.45f));
        canvas.setStroke(ChartScale.scale(0.8f));
        float lineY = (float) y;
        canvas.drawLine((float) bounds.x(), lineY, (float) bounds.maxX(), lineY);
    }

    private void renderAxes(ArberCanvas canvas, PlotContext context, double axisBaseY) {
        ChartTheme theme = context.getTheme();
        ArberColor axisColor = theme.getAxisLabelColor();
        ArberRect bounds = context.getPlotBounds();

        canvas.setColor(axisColor);
        canvas.setStroke(ChartScale.scale(0.8f));
        float baseY = (float) axisBaseY;
        canvas.drawLine((float) bounds.x(), baseY, (float) bounds.maxX(), baseY);
        canvas.drawLine((float) bounds.maxX(), (float) bounds.y(), (float) bounds.maxX(), baseY);

        yScale.setRange(context.getMinY(), context.getMaxY()).setMaxTicks(6);
        for (double tick : yScale.getTicks()) {
            context.mapToPixel(context.getMaxX(), tick, px);
            float y = (float) context.snapPixel(px[1]);
            canvas.drawLine((float) bounds.maxX() - 6, y, (float) bounds.maxX(), y);
            canvas.drawText((float) bounds.maxX() - 46, y - 4, formatPrice(tick));
        }

        xScale.setRange(context.getMinX(), context.getMaxX()).setMaxTicks(6);
        for (double tick : xScale.getTicks()) {
            context.mapToPixel(tick, context.getMinY(), px);
            float x = (float) context.snapPixel(px[0]);
            canvas.drawLine(x, baseY, x, baseY + 5);
            canvas.drawText(x - 12, baseY + 18, formatAxis(tick));
        }
    }

    private void renderCrosshair(ArberCanvas canvas, ChartModel model, PlotContext context, ArberRect fullBounds) {
        int n = model.getPointCount();
        if (n <= 0) return;
        int idx = Math.min(n - 1, Math.max(0, (int) (n * 0.65)));
        double xVal = model.getX(idx);
        double yVal = model.getY(idx);

        context.mapToPixel(xVal, yVal, px);
        float x = (float) context.snapPixel(px[0]);
        float y = (float) context.snapPixel(px[1]);

        ChartTheme theme = context.getTheme();
        ArberColor cross = ColorRegistry.applyAlpha(theme.getAccentColor(), 0.85f);
        canvas.setColor(cross);
        canvas.setStroke(ChartScale.scale(0.9f));
        canvas.drawLine(x, (float) fullBounds.y(), x, (float) fullBounds.maxY());
        canvas.drawLine((float) fullBounds.x(), y, (float) fullBounds.maxX(), y);

        canvas.setColor(ColorRegistry.applyAlpha(theme.getAxisLabelColor(), 0.95f));
        canvas.drawText(x + 8, y - 8, "PX " + formatPrice(yVal));
    }

    private double getMaxVolume(ChartModel model) {
        if (model instanceof FinancialChartModel fin) {
            double[] vols = fin.getVolumeData();
            int count = Math.min(fin.getPointCount(), vols.length);
            double max = 0.0;
            for (int i = 0; i < count; i++) {
                if (vols[i] > max) max = vols[i];
            }
            return max;
        }
        return 0.0;
    }

    private String formatPrice(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String formatAxis(double value) {
        return String.format(java.util.Locale.US, "%.0f", value);
    }
}
