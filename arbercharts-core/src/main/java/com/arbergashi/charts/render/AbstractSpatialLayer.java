package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.SpatialTransformRegistry;
import com.arbergashi.charts.engine.spatial.Matrix4x4;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialDataMapper;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicy;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicies;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialProjector;
import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.VectorizedSpatialOptimizer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartAssets;

/**
 * Base class for spatial-aware layers using batch mapping and projection.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
 */
public abstract class AbstractSpatialLayer extends BaseRenderer implements SpatialChunkRenderer, com.arbergashi.charts.engine.spatial.SpatialPeakProvider {
    private SpatialPathBatchBuilder spatialPathBatchBuilder = new SpatialPathBatchBuilder();
    private SpatialDepthPolicy spatialDepthPolicy = SpatialDepthPolicies.getLayered();
    private SpatialProjector spatialProjector;
    private Matrix4x4 spatialTransform;
    private CoordinateTransformer coordinateTransformer;
    private final SpatialDataMapper spatialDataMapper = new SpatialDataMapper();
    private final SpatialBuffer spatialBuffer = new SpatialBuffer(1024);
    private VectorizedSpatialOptimizer autoOptimizer;

    protected AbstractSpatialLayer(String id) {
        super(id);
    }

    public SpatialPathBatchBuilder getSpatialPathBatchBuilder() {
        return spatialPathBatchBuilder;
    }

    public AbstractSpatialLayer setSpatialPathBatchBuilder(SpatialPathBatchBuilder spatialPathBatchBuilder) {
        if (spatialPathBatchBuilder != null) {
            this.spatialPathBatchBuilder = spatialPathBatchBuilder;
        }
        return this;
    }

    public SpatialDepthPolicy getSpatialDepthPolicy() {
        return spatialDepthPolicy;
    }

    public AbstractSpatialLayer setSpatialDepthPolicy(SpatialDepthPolicy spatialDepthPolicy) {
        if (spatialDepthPolicy != null) {
            this.spatialDepthPolicy = spatialDepthPolicy;
        }
        return this;
    }

    public SpatialProjector getSpatialProjector() {
        return spatialProjector;
    }

    public AbstractSpatialLayer setSpatialProjector(SpatialProjector spatialProjector) {
        this.spatialProjector = spatialProjector;
        return this;
    }

    public CoordinateTransformer getCoordinateTransformer() {
        return coordinateTransformer;
    }

    public AbstractSpatialLayer setCoordinateTransformer(CoordinateTransformer coordinateTransformer) {
        this.coordinateTransformer = coordinateTransformer;
        return this;
    }

    public Matrix4x4 getSpatialTransform() {
        return spatialTransform;
    }

    public AbstractSpatialLayer setSpatialTransform(Matrix4x4 spatialTransform) {
        this.spatialTransform = spatialTransform;
        return this;
    }

    public SpatialDataMapper getSpatialDataMapper() {
        return spatialDataMapper;
    }

    public AbstractSpatialLayer setSpatialOptimizer(VectorizedSpatialOptimizer optimizer) {
        spatialDataMapper.setOptimizer(optimizer);
        return this;
    }

    public SpatialBuffer getSpatialBuffer() {
        return spatialBuffer;
    }

    public com.arbergashi.charts.engine.spatial.VectorizedHitDetector getSpatialHitDetector() {
        return com.arbergashi.charts.engine.spatial.VectorizedHitDetector.getShared();
    }

    @Override
    public com.arbergashi.charts.engine.spatial.SpatialPeakMetadata getSpatialPeakMetadata() {
        return spatialBuffer.getPeakMetadata();
    }

    /**
     * Streams the model through the spatial pipeline into the provided consumer.
     *
     * @param model model to map
     * @param context plot context (reserved for future spatial scaling)
     * @param consumer chunk consumer
     */
    protected void renderSpatialInternal(ChartModel model, PlotContext context, SpatialChunkConsumer consumer) {
        if (consumer == null || model == null) {
            return;
        }
        if (spatialDataMapper.getOptimizer() == null
                && ChartAssets.getBoolean("Chart.ai.optimizer.enabled", false)) {
            if (autoOptimizer == null) {
                autoOptimizer = new VectorizedSpatialOptimizer();
            }
            double minPixelDistanceX = ChartAssets.getFloat("Chart.ai.optimizer.minPixelDistanceX",
                    ChartAssets.getFloat("Chart.ai.optimizer.minPixelDistance", 0.0f));
            double minPixelDistanceY = ChartAssets.getFloat("Chart.ai.optimizer.minPixelDistanceY",
                    ChartAssets.getFloat("Chart.ai.optimizer.minPixelDistance", 0.0f));
            double depthNear = ChartAssets.getFloat("Chart.ai.optimizer.depthNear", 0.0f);
            double depthFar = ChartAssets.getFloat("Chart.ai.optimizer.depthFar", 0.0f);
            double depthScale = ChartAssets.getFloat("Chart.ai.optimizer.depthScale", 0.0f);
            double zDampingFactor = ChartAssets.getFloat("Chart.ai.optimizer.zDampingFactor", 0.5f);
            String zDampingMode = ChartAssets.getString("Chart.ai.optimizer.zDampingMode", "LINEAR");
            boolean preservePeaks = ChartAssets.getBoolean("Chart.ai.optimizer.preservePeaks", true);
            boolean exportPeaks = ChartAssets.getBoolean("Chart.ai.optimizer.exportPeaks", false);
            int lastFrameMicros = ChartAssets.getInt("Chart.render.lastFrameMicros", 0);
            int targetFrameMicros = ChartAssets.getInt("Chart.render.targetFrameMicros", 16_666);
            if (lastFrameMicros > 0 && targetFrameMicros > 0) {
                double factor = 1.0;
                if (lastFrameMicros > targetFrameMicros) {
                    factor = ChartAssets.getFloat("Chart.ai.optimizer.scaleUpFactor", 1.15f);
                } else if (lastFrameMicros < (int) (targetFrameMicros * 0.7)) {
                    factor = ChartAssets.getFloat("Chart.ai.optimizer.scaleDownFactor", 0.9f);
                }
                minPixelDistanceX *= factor;
                minPixelDistanceY *= factor;
            }
            if (context != null) {
                double plotWidth = context.getPlotBounds().width();
                double plotHeight = context.getPlotBounds().height();
                if (plotWidth > 0.0 && plotHeight > 0.0) {
                    double unitsPerPixelX = context.rangeX() / plotWidth;
                    double unitsPerPixelY = context.rangeY() / plotHeight;
                    double refUnitsPerPixelX = ChartAssets.getFloat("Chart.ai.optimizer.referenceUnitsPerPixelX",
                            ChartAssets.getFloat("Chart.ai.optimizer.referenceUnitsPerPixel",
                                    (float) unitsPerPixelX));
                    double refUnitsPerPixelY = ChartAssets.getFloat("Chart.ai.optimizer.referenceUnitsPerPixelY",
                            ChartAssets.getFloat("Chart.ai.optimizer.referenceUnitsPerPixel",
                                    (float) unitsPerPixelY));
                    if (refUnitsPerPixelX > 0.0) {
                        minPixelDistanceX *= (unitsPerPixelX / refUnitsPerPixelX);
                    }
                    if (refUnitsPerPixelY > 0.0) {
                        minPixelDistanceY *= (unitsPerPixelY / refUnitsPerPixelY);
                    }
                }
            }
            double minClamp = ChartAssets.getFloat("Chart.ai.optimizer.minPixelDistanceMin", 0.1f);
            double maxClamp = ChartAssets.getFloat("Chart.ai.optimizer.minPixelDistanceMax", 2.0f);
            minPixelDistanceX = Math.min(Math.max(minPixelDistanceX, minClamp), maxClamp);
            minPixelDistanceY = Math.min(Math.max(minPixelDistanceY, minClamp), maxClamp);
            autoOptimizer.setMinPixelDistance(minPixelDistanceX, minPixelDistanceY);
            autoOptimizer.setDepthAttenuation(depthNear, depthFar, depthScale);
            autoOptimizer.setDepthDamping(zDampingFactor,
                    "EXPONENTIAL".equalsIgnoreCase(zDampingMode)
                            ? VectorizedSpatialOptimizer.DepthDampingMode.EXPONENTIAL
                            : VectorizedSpatialOptimizer.DepthDampingMode.LINEAR);
            autoOptimizer.setPreservePeaks(preservePeaks);
            autoOptimizer.setExportPeaks(exportPeaks);
            spatialDataMapper.setOptimizer(autoOptimizer);
        }
        CoordinateTransformer resolved = SpatialTransformRegistry.getResolvedTransform(this, coordinateTransformer);
        if (context == null) {
            spatialDataMapper.mapAllProjected(model, spatialBuffer, spatialTransform, spatialProjector, consumer);
            return;
        }
        spatialDataMapper.mapAllProjected(model, spatialBuffer, spatialTransform, spatialProjector, resolved, context, consumer);
    }

    @Override
    public void renderSpatial(ChartModel model, PlotContext context, SpatialChunkConsumer consumer) {
        renderSpatialInternal(model, context, consumer);
    }

}
