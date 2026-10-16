package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.AnimationProfile;
import com.arbergashi.charts.api.AxisGapModel;
import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.util.NiceScale;

/** Preserves host presentation settings while applying an optional reversible transform. */
final class HostPlotContext implements PlotContext {
    private final PlotContext base;
    private final CoordinateTransformer transformer;
    private final boolean gridX;
    private final boolean gridY;
    private final double pixelsPerMillimeter;

    HostPlotContext(PlotContext base, CoordinateTransformer transformer, boolean gridX, boolean gridY,
                    double pixelsPerMillimeter) {
        this.base = base;
        this.transformer = transformer;
        this.gridX = gridX;
        this.gridY = gridY;
        this.pixelsPerMillimeter = pixelsPerMillimeter;
    }

    @Override public double getMinX() { return base.getMinX(); }
    @Override public double getMaxX() { return base.getMaxX(); }
    @Override public double getMinY() { return base.getMinY(); }
    @Override public double getMaxY() { return base.getMaxY(); }
    @Override public ArberRect getPlotBounds() { return base.getPlotBounds(); }
    @Override public NiceScale.ScaleMode getScaleModeX() { return base.getScaleModeX(); }
    @Override public NiceScale.ScaleMode getScaleModeY() { return base.getScaleModeY(); }
    @Override public boolean isLogarithmicY() { return base.isLogarithmicY(); }
    @Override public boolean isInvertedX() { return base.isInvertedX(); }
    @Override public boolean isInvertedY() { return base.isInvertedY(); }
    @Override public ChartRenderHints getRenderHints() { return base.getRenderHints(); }
    @Override public boolean isSignalLost() { return base.isSignalLost(); }
    @Override public AxisGapModel getGapModel() { return base.getGapModel(); }
    @Override public ChartTheme getTheme() { return base.getTheme(); }
    @Override public AnimationProfile getAnimationProfile() { return base.getAnimationProfile(); }
    @Override public int getRequestedTickCountX() { return base.getRequestedTickCountX(); }
    @Override public int getRequestedTickCountY() { return base.getRequestedTickCountY(); }
    @Override public boolean isGridVisibleX() { return gridX; }
    @Override public boolean isGridVisibleY() { return gridY; }
    @Override public double getPixelsPerMillimeter() { return pixelsPerMillimeter; }

    @Override
    public void mapToPixel(double x, double y, double[] out) {
        if (transformer == null) base.mapToPixel(x, y, out);
        else transformer.mapToPixel(base, x, y, out);
    }

    @Override
    public void mapToData(double x, double y, double[] out) {
        if (transformer == null) base.mapToData(x, y, out);
        else transformer.mapToData(base, x, y, out);
    }
}
