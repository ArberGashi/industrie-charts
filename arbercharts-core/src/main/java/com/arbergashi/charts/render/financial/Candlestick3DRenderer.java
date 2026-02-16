package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.Matrix4x4;
import com.arbergashi.charts.engine.spatial.PerspectiveProjector;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicies;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.render.AbstractSpatialLayer;
import com.arbergashi.charts.util.ChartAssets;

import com.arbergashi.charts.core.geometry.ArberRect;

/**
 * 3D candlestick renderer using spatial batch pipeline.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class Candlestick3DRenderer extends AbstractSpatialLayer {
    private final PerspectiveProjector projector = new PerspectiveProjector();
    private final Matrix4x4 transform = new Matrix4x4();
    private final double[] tmpIn = new double[3];
    private final double[] tmpOut = new double[3];
    private final double[] singleIn = new double[3];
    private final double[] singleOut = new double[3];
    private double zOffset = 0.0;
    private double zScale = 0.002;
    private double zDepth = 0.06;
    private double lodDepthThreshold = 1.1;
    private boolean solidEnabled = false;
    private int hitCount = 0;
    private com.arbergashi.charts.engine.spatial.SpatialBuffer hitBuffer = new com.arbergashi.charts.engine.spatial.SpatialBuffer(1024);

    public Candlestick3DRenderer() {
        super("candlestick_3d");
        transform.setIdentity();
        setSpatialProjector(projector);
        setSpatialTransform(transform);
        setSpatialDepthPolicy(SpatialDepthPolicies.getSortedBackToFront());
    }

    public Candlestick3DRenderer setZOffset(double zOffset) {
        this.zOffset = zOffset;
        return this;
    }

    public Candlestick3DRenderer setZScale(double zScale) {
        this.zScale = zScale;
        return this;
    }

    public Candlestick3DRenderer setZDepth(double zDepth) {
        this.zDepth = zDepth;
        return this;
    }

    public Candlestick3DRenderer setLodDepthThreshold(double lodDepthThreshold) {
        this.lodDepthThreshold = lodDepthThreshold;
        return this;
    }

    public Candlestick3DRenderer setSolid(boolean solidEnabled) {
        this.solidEnabled = solidEnabled;
        return this;
    }

    public int getHitCount() {
        return hitCount;
    }

    public com.arbergashi.charts.engine.spatial.SpatialBuffer getHitBuffer() {
        return hitBuffer;
    }

    @Override
    public void renderSpatial(ChartModel model, PlotContext context, com.arbergashi.charts.engine.spatial.SpatialChunkConsumer consumer) {
        if (model == null || context == null) return;

        ArberRect bounds = context.getPlotBounds();
        double scale = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.45;
        projector.setCenterX(bounds.centerX())
                .setCenterY(bounds.centerY())
                .setScale(scale)
                .setZBias(4.0);
        getSpatialPathBatchBuilder().setZMin(-2.0);

        ChartTheme theme = getResolvedTheme(context);
        int bullish = theme.getBullishColor().argb();
        int bearish = theme.getBearishColor().argb();

        int count = model.getPointCount();
        if (count <= 0) return;

        double minX = context.getMinX();
        double maxX = context.getMaxX();
        double minY = context.getMinY();
        double maxY = context.getMaxY();
        double spanX = Math.max(1e-9, maxX - minX);
        double spanY = Math.max(1e-9, maxY - minY);
        double candleWidth = (2.0 / Math.max(1, count)) * 0.7;

        FinancialChartModel fin = (model instanceof FinancialChartModel f) ? f : null;

        hitCount = 0;
        if (count > hitBuffer.getPointCapacity()) {
            hitBuffer = new com.arbergashi.charts.engine.spatial.SpatialBuffer(Math.max(1024, count));
        }
        double[] hitCoords = hitBuffer.getInputCoords();
        double lodThreshold = resolveLodThreshold();

        for (int i = 0; i < count; i++) {
            double xVal = model.getX(i);
            double high = (fin != null) ? fin.getHigh(i) : model.getMax(i);
            double low = (fin != null) ? fin.getLow(i) : model.getMin(i);
            double open = (fin != null) ? fin.getOpen(i) : model.getWeight(i);
            double close = (fin != null) ? fin.getClose(i) : model.getY(i);
            double volume = (fin != null) ? fin.getVolume(i) : model.getWeight(i);

            double xCenter = ((xVal - minX) / spanX) * 2.0 - 1.0;
            double yOpen = ((open - minY) / spanY) * 2.0 - 1.0;
            double yClose = ((close - minY) / spanY) * 2.0 - 1.0;
            double yHigh = ((high - minY) / spanY) * 2.0 - 1.0;
            double yLow = ((low - minY) / spanY) * 2.0 - 1.0;

            double zBase = zOffset + volume * zScale;
            double zFront = zBase;
            double zBack = zBase + zDepth;

            boolean bullishCandle = close >= open;
            long styleKey = SpatialStyleDescriptor.pack(bullishCandle ? bullish : bearish, 1.2f, 0, 0);
            getSpatialPathBatchBuilder().setStyleKey(styleKey);

            double xLeft = xCenter - candleWidth * 0.5;
            double xRight = xCenter + candleWidth * 0.5;
            double yTop = Math.max(yOpen, yClose);
            double yBottom = Math.min(yOpen, yClose);

            if (Math.abs(zBase) > lodThreshold) {
                drawLine(xCenter, yLow, zFront, xCenter, yHigh, zFront);
                storeHitPoint(hitCoords, i, xCenter, yClose, zFront);
                continue;
            }

            // Wick
            drawLine(xCenter, yLow, zFront, xCenter, yHigh, zFront);

            // Optional front face fill
            if (solidEnabled && consumer instanceof com.arbergashi.charts.engine.spatial.SpatialFillConsumer fillConsumer) {
                projectPoint(xLeft, yBottom, zFront, tmpIn);
                projectPoint(xRight, yBottom, zFront, tmpOut);
                double fx1 = tmpIn[0];
                double fy1 = tmpIn[1];
                double fx2 = tmpOut[0];
                double fy2 = tmpOut[1];
                projectPoint(xRight, yTop, zFront, tmpIn);
                projectPoint(xLeft, yTop, zFront, tmpOut);
                fillConsumer.fillQuad(fx1, fy1, fx2, fy2, tmpIn[0], tmpIn[1], tmpOut[0], tmpOut[1]);
            }

            // Box edges (wireframe)
            drawLine(xLeft, yBottom, zFront, xRight, yBottom, zFront);
            drawLine(xRight, yBottom, zFront, xRight, yTop, zFront);
            drawLine(xRight, yTop, zFront, xLeft, yTop, zFront);
            drawLine(xLeft, yTop, zFront, xLeft, yBottom, zFront);

            drawLine(xLeft, yBottom, zBack, xRight, yBottom, zBack);
            drawLine(xRight, yBottom, zBack, xRight, yTop, zBack);
            drawLine(xRight, yTop, zBack, xLeft, yTop, zBack);
            drawLine(xLeft, yTop, zBack, xLeft, yBottom, zBack);

            drawLine(xLeft, yBottom, zFront, xLeft, yBottom, zBack);
            drawLine(xRight, yBottom, zFront, xRight, yBottom, zBack);
            drawLine(xRight, yTop, zFront, xRight, yTop, zBack);
            drawLine(xLeft, yTop, zFront, xLeft, yTop, zBack);
            storeHitPoint(hitCoords, i, xCenter, yClose, zFront);

            if (solidEnabled) {
                drawLine(xLeft, yBottom, zFront, xRight, yTop, zFront);
                drawLine(xRight, yBottom, zFront, xLeft, yTop, zFront);
                drawLine(xLeft, yBottom, zBack, xRight, yTop, zBack);
                drawLine(xRight, yBottom, zBack, xLeft, yTop, zBack);
            }
        }
        hitCount = Math.min(count, hitBuffer.getPointCapacity());
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
        projectPoint(x1, y1, z1, tmpIn);
        projectPoint(x2, y2, z2, tmpOut);
        getSpatialPathBatchBuilder().setLineSegment(
                tmpIn[0], tmpIn[1], tmpIn[2],
                tmpOut[0], tmpOut[1], tmpOut[2]
        );
    }

    private void projectPoint(double x, double y, double z, double[] out) {
        singleIn[0] = x;
        singleIn[1] = y;
        singleIn[2] = z;
        transform.getCalculatedTransformBatch(singleIn, singleOut, 1, hitBuffer.getScratch());
        projector.getCalculatedProjectionBatch(singleOut, singleIn, 1);
        out[0] = singleIn[0];
        out[1] = singleIn[1];
        out[2] = singleIn[2];
    }

    private void storeHitPoint(double[] hitCoords, int index, double x, double y, double z) {
        double[] projected = tmpIn;
        projectPoint(x, y, z, projected);
        int base = index * 3;
        if (base + 2 >= hitCoords.length) {
            return;
        }
        hitCoords[base] = projected[0];
        hitCoords[base + 1] = projected[1];
        hitCoords[base + 2] = projected[2];
    }

    private double resolveLodThreshold() {
        double threshold = lodDepthThreshold;
        int lastFrameMicros = ChartAssets.getInt("Chart.render.lastFrameMicros", 0);
        int target = ChartAssets.getInt("Chart.render.targetFrameMicros", 16_666);
        if (lastFrameMicros > 0 && target > 0) {
            if (lastFrameMicros > target) {
                threshold = ChartAssets.getFloat("Chart.candle3d.lod.thresholdHigh", (float) (lodDepthThreshold * 0.7));
            } else if (lastFrameMicros < (int) (target * 0.7)) {
                threshold = ChartAssets.getFloat("Chart.candle3d.lod.thresholdLow", (float) (lodDepthThreshold * 1.2));
            }
        }
        return threshold;
    }
}
