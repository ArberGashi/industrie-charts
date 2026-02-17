package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.Matrix4x4;
import com.arbergashi.charts.engine.spatial.PerspectiveProjector;
import com.arbergashi.charts.engine.spatial.SpatialDataMapper;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicies;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.AbstractSpatialLayer;
import com.arbergashi.charts.util.ChartAssets;

/**
 * Spatial line renderer that treats Z as a first-class dimension.
 *
 * <p>2D is a special case with z=0. When weights are provided, they can be mapped
 * into the Z axis via {@link #setZScale(double)}.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class LineSpatialRenderer extends AbstractSpatialLayer {
    private final PerspectiveProjector projector = new PerspectiveProjector();
    private final Matrix4x4 transform = new Matrix4x4();
    private double zScale = 0.0;
    private double zOffset = 0.0;

    public LineSpatialRenderer() {
        super("line_spatial");
        transform.setIdentity();
        setSpatialProjector(projector);
        setSpatialTransform(transform);
        setSpatialDepthPolicy(SpatialDepthPolicies.getLayered());
        getSpatialDataMapper()
                .setZMode(SpatialDataMapper.ZMode.CUSTOM)
                .setZComponentProvider((index, x, y, weight) -> zOffset + weight * zScale);
    }

    public LineSpatialRenderer setZScale(double zScale) {
        this.zScale = zScale;
        return this;
    }

    public LineSpatialRenderer setZOffset(double zOffset) {
        this.zOffset = zOffset;
        return this;
    }

    @Override
    public void renderSpatial(ChartModel model, PlotContext context, com.arbergashi.charts.engine.spatial.SpatialChunkConsumer consumer) {
        if (context == null) {
            super.renderSpatial(model, null, consumer);
            return;
        }
        ArberRect bounds = context.getPlotBounds();
        double scale = Math.min(bounds.width(), bounds.height()) * 0.45;
        projector.setCenterX(bounds.centerX())
                .setCenterY(bounds.centerY())
                .setScale(scale)
                .setZBias(4.0);
        getSpatialPathBatchBuilder().setZMin(-2.0);
        ChartTheme theme = getResolvedTheme(context);
        int argb = (theme != null && theme.getAccentColor() != null)
                ? theme.getAccentColor().argb()
                : 0xFF6AA9FF;
        float stroke = ChartAssets.getFloat("Chart.lineSpatial.strokeWidth", 1.6f);
        getSpatialPathBatchBuilder().setStyleKey(SpatialStyleDescriptor.pack(argb, stroke, 0, 0));
        super.renderSpatial(model, context, consumer);
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        // Spatial renderer: rendering handled via renderSpatial() and spatial batch pipeline.
    }

    @Override
    public void accept(com.arbergashi.charts.engine.spatial.SpatialBuffer buffer, int count) {
        // No-op: spatial rendering handled via spatial batch pipeline.
    }
}
