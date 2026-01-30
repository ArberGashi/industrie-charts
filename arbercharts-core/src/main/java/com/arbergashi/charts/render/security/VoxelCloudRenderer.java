package com.arbergashi.charts.render.security;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.Matrix4x4;
import com.arbergashi.charts.engine.spatial.PerspectiveProjector;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicies;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.AbstractSpatialLayer;
import com.arbergashi.charts.api.types.ArberColor;

/**
 * 3D voxel cloud renderer for security/anomaly visualization.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class VoxelCloudRenderer extends AbstractSpatialLayer {
    private final PerspectiveProjector projector = new PerspectiveProjector();
    private final Matrix4x4 transform = new Matrix4x4();
    private final double[] tmpA = new double[3];
    private final double[] tmpB = new double[3];
    private final double[] singleIn = new double[3];
    private final double[] singleOut = new double[3];
    private double voxelSize = 0.035;
    private double anomalyThreshold = 0.75;

    public VoxelCloudRenderer() {
        super("voxel_cloud");
        transform.setIdentity();
        setSpatialProjector(projector);
        setSpatialTransform(transform);
        setSpatialDepthPolicy(SpatialDepthPolicies.getSortedBackToFront());
    }

    public VoxelCloudRenderer setVoxelSize(double voxelSize) {
        this.voxelSize = voxelSize;
        return this;
    }

    public VoxelCloudRenderer setAnomalyThreshold(double anomalyThreshold) {
        this.anomalyThreshold = anomalyThreshold;
        return this;
    }

    @Override
    public void renderSpatial(ChartModel model, PlotContext context, com.arbergashi.charts.engine.spatial.SpatialChunkConsumer consumer) {
        if (model == null || context == null) {
            return;
        }

        var bounds = context.getPlotBounds();
        double scale = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.45;
        projector.setCenterX(bounds.getCenterX())
                .setCenterY(bounds.getCenterY())
                .setScale(scale)
                .setZBias(4.0);
        getSpatialPathBatchBuilder().setZMin(-2.0);

        ChartTheme theme = getResolvedTheme(context);
        ArberColor base = theme.getAccentColor();
        ArberColor hot = theme.getBearishColor();
        int baseColor = (base != null) ? base.argb() : 0xFF33C7FF;
        int hotColor = (hot != null) ? hot.argb() : 0xFFFF4D4D;

        int count = model.getPointCount();
        if (count <= 0) {
            return;
        }

        double size = Math.max(0.01, voxelSize);
        double threshold = anomalyThreshold;

        for (int i = 0; i < count; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            double z = model.getWeight(i);
            double intensity = model.getMax(i);

            float t = 0f;
            if (intensity > threshold) {
                double range = 1.0 - threshold;
                t = (range <= 0.0) ? 1f : (float) ((intensity - threshold) / range);
                if (t > 1f) t = 1f;
                if (t < 0f) t = 0f;
            }
            int color = lerpArgb(baseColor, hotColor, t);
            long styleKey = SpatialStyleDescriptor.pack(color, 1.1f + 0.5f * t, 0, 0);
            getSpatialPathBatchBuilder().setStyleKey(styleKey);

            double half = size * 0.5;
            double x0 = x - half;
            double x1 = x + half;
            double y0 = y - half;
            double y1 = y + half;
            double z0 = z - half;
            double z1 = z + half;

            // front face
            drawLine(x0, y0, z0, x1, y0, z0);
            drawLine(x1, y0, z0, x1, y1, z0);
            drawLine(x1, y1, z0, x0, y1, z0);
            drawLine(x0, y1, z0, x0, y0, z0);

            // back face
            drawLine(x0, y0, z1, x1, y0, z1);
            drawLine(x1, y0, z1, x1, y1, z1);
            drawLine(x1, y1, z1, x0, y1, z1);
            drawLine(x0, y1, z1, x0, y0, z1);

            // connecting edges
            drawLine(x0, y0, z0, x0, y0, z1);
            drawLine(x1, y0, z0, x1, y0, z1);
            drawLine(x1, y1, z0, x1, y1, z1);
            drawLine(x0, y1, z0, x0, y1, z1);
        }
    }

    @Override
    protected void drawData(com.arbergashi.charts.core.rendering.ArberCanvas canvas, ChartModel model, PlotContext context) {
        // Spatial renderer: rendering handled via renderSpatial() and spatial batch pipeline.
    }

    @Override
    public void accept(com.arbergashi.charts.engine.spatial.SpatialBuffer buffer, int count) {
        // No-op: spatial rendering handled through batch pipeline.
    }

    private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        projectPoint(x1, y1, z1, tmpA);
        projectPoint(x2, y2, z2, tmpB);
        getSpatialPathBatchBuilder().setLineSegment(
                tmpA[0], tmpA[1], tmpA[2],
                tmpB[0], tmpB[1], tmpB[2]
        );
    }

    private void projectPoint(double x, double y, double z, double[] out) {
        singleIn[0] = x;
        singleIn[1] = y;
        singleIn[2] = z;
        transform.getCalculatedTransformBatch(singleIn, singleOut, 1, getSpatialBuffer().getScratch());
        projector.getCalculatedProjectionBatch(singleOut, singleIn, 1);
        out[0] = singleIn[0];
        out[1] = singleIn[1];
        out[2] = singleIn[2];
    }

    private static int lerpArgb(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF;
        int aR = (a >>> 16) & 0xFF;
        int aG = (a >>> 8) & 0xFF;
        int aB = a & 0xFF;

        int bA = (b >>> 24) & 0xFF;
        int bR = (b >>> 16) & 0xFF;
        int bG = (b >>> 8) & 0xFF;
        int bB = b & 0xFF;

        int oA = (int) (aA + (bA - aA) * t);
        int oR = (int) (aR + (bR - aR) * t);
        int oG = (int) (aG + (bG - aG) * t);
        int oB = (int) (aB + (bB - aB) * t);

        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }
}
