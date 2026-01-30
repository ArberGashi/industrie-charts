package com.arbergashi.charts.api;

import com.arbergashi.charts.engine.spatial.SpatialDepthPolicies;
import com.arbergashi.charts.platform.render.RendererRegistry;

import java.util.EnumSet;

/**
 * Standard affinity initialization for spatial-aware renderers.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialRegistryInitializer {
    private SpatialRegistryInitializer() {
    }

    public static void initStandardAffinities() {
        EnumSet<RendererCapability> spatialCaps = EnumSet.of(RendererCapability.SPATIAL_BATCH, RendererCapability.COORDINATE_TRANSFORM_PROVIDER);

        RendererRegistry.setRendererAffinity(
                "smith_chart",
                new RendererAffinity(spatialCaps, SpatialTransformRegistry.getSmithTransform(), SpatialDepthPolicies.getSortedBackToFront())
        );
        RendererRegistry.setRendererAffinity(
                "smith_vswr",
                new RendererAffinity(spatialCaps, SpatialTransformRegistry.getSmithTransform(), SpatialDepthPolicies.getSortedBackToFront())
        );
        RendererRegistry.setRendererAffinity(
                "geo_map",
                new RendererAffinity(spatialCaps, SpatialTransformRegistry.getMercatorTransform(), SpatialDepthPolicies.getLayered())
        );
        RendererRegistry.setRendererAffinity(
                "ternary_system",
                new RendererAffinity(spatialCaps, SpatialTransformRegistry.getLinearTransform(), SpatialDepthPolicies.getSortedBackToFront())
        );
        RendererRegistry.setRendererAffinity(
                "isometric_projection",
                new RendererAffinity(spatialCaps, SpatialTransformRegistry.getLinearTransform(), SpatialDepthPolicies.getSortedBackToFront())
        );
    }
}
