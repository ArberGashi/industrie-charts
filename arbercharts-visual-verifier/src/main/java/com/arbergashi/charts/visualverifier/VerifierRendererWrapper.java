package com.arbergashi.charts.visualverifier;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.util.ColorRegistry;

import java.util.Map;
import java.util.Optional;

final class VerifierRendererWrapper implements ChartRenderer {
    private final ChartRenderer delegate;
    private final boolean paperGrid;
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    VerifierRendererWrapper(ChartRenderer delegate, boolean paperGrid) {
        this.delegate = delegate;
        this.paperGrid = paperGrid;
    }

    @Override
    public void render(ArberCanvas canvas, ChartModel model, PlotContext context) {
        ChartTheme theme = context.getTheme();
        ArberRect bounds = context.getPlotBounds();

        // Background fill
        canvas.setColor(theme.getBackground());
        canvas.fillRect((float) bounds.x(), (float) bounds.y(), (float) bounds.width(), (float) bounds.height());

        if (paperGrid) {
            renderPaperGrid(canvas, bounds, theme);
        }

        delegate.render(canvas, model, context);
    }

    private void renderPaperGrid(ArberCanvas canvas, ArberRect bounds, ChartTheme theme) {
        double minX = bounds.x();
        double minY = bounds.y();
        double maxX = bounds.maxX();
        double maxY = bounds.maxY();

        // Medical paper style: subtle minor + stronger major lines.
        ArberColor base = theme.getGridColor();
        ArberColor minor = ColorRegistry.applyAlpha(base, 0.25f);
        ArberColor major = ColorRegistry.applyAlpha(base, 0.6f);

        float minorStep = 20.0f;
        float majorStep = 100.0f;

        for (double x = minX; x <= maxX; x += minorStep) {
            boolean isMajor = Math.abs((x - minX) % majorStep) < 0.0001;
            canvas.setColor(isMajor ? major : minor);
            lineX[0] = (float) x;
            lineY[0] = (float) minY;
            lineX[1] = (float) x;
            lineY[1] = (float) maxY;
            canvas.drawPolyline(lineX, lineY, 2);
        }

        for (double y = minY; y <= maxY; y += minorStep) {
            boolean isMajor = Math.abs((y - minY) % majorStep) < 0.0001;
            canvas.setColor(isMajor ? major : minor);
            lineX[0] = (float) minX;
            lineY[0] = (float) y;
            lineX[1] = (float) maxX;
            lineY[1] = (float) y;
            canvas.drawPolyline(lineX, lineY, 2);
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        return delegate.getPointAt(pixel, model, context);
    }

    @Override
    public String getTooltipText(int index, ChartModel model) {
        return delegate.getTooltipText(index, model);
    }

    @Override
    public Map<String, Object> getFocusValues(int index, ChartModel model, PlotContext context) {
        return delegate.getFocusValues(index, model, context);
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return delegate.getPreferredYRange(model);
    }

    @Override
    public boolean supportsEmptyState() {
        return delegate.supportsEmptyState();
    }

    @Override
    public void renderEmptyState(ArberCanvas canvas, ChartModel model, PlotContext context) {
        delegate.renderEmptyState(canvas, model, context);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isLegendRequired() {
        return delegate.isLegendRequired();
    }

    @Override
    public void clearHover() {
        delegate.clearHover();
    }
}
