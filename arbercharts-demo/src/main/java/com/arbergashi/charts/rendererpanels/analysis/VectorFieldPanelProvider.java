package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.VectorFieldRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class VectorFieldPanelProvider {
    public static ArberChartPanel create() {
        // Professional vector field demo:
        // Incompressible vortex pair with gentle shear:
        // - visually stable
        // - educational (flow pattern is clear)
        // - domain is explicitly anchored so autoscaling is deterministic

        DefaultChartModel domain = new DefaultChartModel("Domain");
        domain.addPoint(-2.0, -1.5, 0, "Domain min");
        domain.addPoint(2.0, 1.5, 0, "Domain max");
        domain.addPoint(0.0, 0.0, 0, "Center");

        // Vortex pair around (-0.8, 0) and (0.8, 0).
        VectorFieldRenderer renderer = new VectorFieldRenderer((x, y, out) -> {
            double v1x = x + 0.8;
            double v1y = y;
            double v2x = x - 0.8;
            double v2y = y;

            double r1 = v1x * v1x + v1y * v1y + 0.08;
            double r2 = v2x * v2x + v2y * v2y + 0.08;

            // Perpendicular swirl contribution
            double s1x = -v1y / r1;
            double s1y = v1x / r1;

            double s2x = v2y / r2;   // opposite swirl
            double s2y = -v2x / r2;

            // Gentle rightward shear so flow isn't perfectly symmetric.
            double shear = 0.12 * y;

            out[0] = s1x + s2x + shear;
            out[1] = s1y + s2y;
            return true;
        });

        // 18–22 tends to be the sweet spot for readability.
        renderer.setGridResolution(22);

        return ArberChartBuilder.create()
                .withTitle("Vector Field – Vortex Pair Flow")
                // Grid is enforced centrally by DemoPanelFactory (AnalysisGridLayer).
                .addLayer(domain, renderer)
                .withTooltips(true)
                .withLegend(false)
                .build().withAnimations(true);
    }
}
