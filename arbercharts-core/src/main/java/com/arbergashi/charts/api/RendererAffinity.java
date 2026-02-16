package com.arbergashi.charts.api;

import com.arbergashi.charts.engine.spatial.SpatialDepthPolicy;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Renderer affinity information for capability and default policies.
 *
 * @since 2.0.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class RendererAffinity {
    private EnumSet<RendererCapability> capabilities;
    private CoordinateTransformer preferredTransform;
    private SpatialDepthPolicy preferredDepthPolicy;

    public RendererAffinity(EnumSet<RendererCapability> capabilities,
                            CoordinateTransformer preferredTransform,
                            SpatialDepthPolicy preferredDepthPolicy) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.preferredTransform = preferredTransform;
        this.preferredDepthPolicy = preferredDepthPolicy;
    }

    public EnumSet<RendererCapability> getCapabilities() {
        return capabilities;
    }

    public RendererAffinity setCapabilities(EnumSet<RendererCapability> capabilities) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        return this;
    }

    public CoordinateTransformer getPreferredTransform() {
        return preferredTransform;
    }

    public RendererAffinity setPreferredTransform(CoordinateTransformer preferredTransform) {
        this.preferredTransform = preferredTransform;
        return this;
    }

    public SpatialDepthPolicy getPreferredDepthPolicy() {
        return preferredDepthPolicy;
    }

    public RendererAffinity setPreferredDepthPolicy(SpatialDepthPolicy preferredDepthPolicy) {
        this.preferredDepthPolicy = preferredDepthPolicy;
        return this;
    }
}
