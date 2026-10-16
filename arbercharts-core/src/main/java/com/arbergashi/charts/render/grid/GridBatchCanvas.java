package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;

/** Adapts the shared grid geometry to isolated spatial line segments. */
final class GridBatchCanvas implements ArberCanvas {
    private SpatialPathBatchBuilder builder;
    private double z;
    private int argb;
    private float stroke;
    private float penX;
    private float penY;

    void render(GridLayer layer, SpatialPathBatchBuilder target, PlotContext context, GridBatchConfig config) {
        if (target == null || context == null) return;
        builder = target;
        z = config.getGridZ();
        argb = 0xFF000000;
        stroke = 1.0f;
        penX = 0.0f;
        penY = 0.0f;
        long previousStyle = target.getStyleKey();
        target.setZMin(config.getZMin()).setClippingMode(config.getClippingMode());
        try {
            layer.renderGrid(this, context);
        } finally {
            target.setStyleKey(previousStyle);
            target.resetPathState();
            builder = null;
        }
    }

    @Override
    public void setColor(ArberColor color) { argb = color.argb(); }

    @Override
    public void setStroke(float width) { stroke = width; }

    @Override
    public void moveTo(float x, float y) {
        penX = x;
        penY = y;
    }

    @Override
    public void lineTo(float x, float y) {
        segment(penX, penY, x, y);
        moveTo(x, y);
    }

    @Override
    public void drawPolyline(float[] xs, float[] ys, int count) {
        if (xs == null || ys == null) return;
        int limit = Math.min(count, Math.min(xs.length, ys.length));
        for (int i = 1; i < limit; i++) {
            segment(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }
    }

    private void segment(float x1, float y1, float x2, float y2) {
        builder.setStyleKey(SpatialStyleDescriptor.pack(argb, stroke, 0, 0));
        builder.setLineSegment(x1, y1, z, x2, y2, z);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        segment(x, y, x + w, y);
        segment(x + w, y, x + w, y + h);
        segment(x + w, y + h, x, y + h);
        segment(x, y + h, x, y);
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        throw new UnsupportedOperationException("Grid batches support stroked geometry only");
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int count) {
        throw new UnsupportedOperationException("Grid batches support stroked geometry only");
    }

    @Override
    public void drawVoxelField(VoxelBuffer buffer) {
        throw new UnsupportedOperationException("Grid batches support stroked geometry only");
    }

    @Override
    public ArberMatrix getTransform() { return null; }

    @Override
    public void setClip(ArberRect clip) {
        if (clip != null) throw new UnsupportedOperationException("Grid geometry must be clipped before batching");
    }

    @Override
    public ArberRect getClip() { return null; }
}
