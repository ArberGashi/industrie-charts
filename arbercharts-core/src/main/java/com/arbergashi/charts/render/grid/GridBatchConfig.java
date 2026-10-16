package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialDepthPolicy;

/**
 * Configuration for grid batch rendering in spatial pipelines.
 * Grid coordinates match the Canvas path; {@code gridZ} supplies their depth
 * for clipping. The default depth of 1 is visible above the default clip plane 0.
 * All segments share this depth, so depth sorting does not change their order.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
 * @author Arber Gashi
 */
public final class GridBatchConfig {
    private double zMin;
    private double gridZ = 1.0;
    private SpatialPathBatchBuilder.ClippingMode clippingMode = SpatialPathBatchBuilder.ClippingMode.DISCARD;
    private SpatialDepthPolicy depthPolicy = () -> SpatialDepthPolicy.Mode.LAYERED;

    public double getZMin() {
        return zMin;
    }

    public GridBatchConfig setZMin(double zMin) {
        this.zMin = zMin;
        return this;
    }

    public double getGridZ() {
        return gridZ;
    }

    public GridBatchConfig setGridZ(double gridZ) {
        this.gridZ = gridZ;
        return this;
    }

    public SpatialPathBatchBuilder.ClippingMode getClippingMode() {
        return clippingMode;
    }

    public GridBatchConfig setClippingMode(SpatialPathBatchBuilder.ClippingMode clippingMode) {
        if (clippingMode != null) {
            this.clippingMode = clippingMode;
        }
        return this;
    }

    public SpatialDepthPolicy getDepthPolicy() {
        return depthPolicy;
    }

    public GridBatchConfig setDepthPolicy(SpatialDepthPolicy depthPolicy) {
        if (depthPolicy != null) {
            this.depthPolicy = depthPolicy;
        }
        return this;
    }
}
