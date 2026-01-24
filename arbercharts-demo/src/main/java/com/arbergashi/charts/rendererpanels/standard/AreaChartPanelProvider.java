package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.StackedAreaRenderer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import com.arbergashi.charts.ui.ArberChartPanel;

import java.util.Random;

public class AreaChartPanelProvider {
    public static ArberChartPanel create() {
        // Professional stacked area demo:
        // Electricity generation mix (GW) with realistic constraints:
        // - total demand fluctuates mildly
        // - renewables share rises, fossil share declines, nuclear stays relatively stable
        // - deterministic and smooth for consistent screenshots

        DefaultChartModel renewable = new DefaultChartModel("Renewables (GW)");
        DefaultChartModel fossil = new DefaultChartModel("Fossil (GW)");
        DefaultChartModel nuclear = new DefaultChartModel("Nuclear (GW)");

        Random r = new Random(DemoPanelUtils.DEMO_SEED + 3);

        int months = 120; // 2015..2024 (monthly)
        int startYear = 2015;

        for (int i = 0; i < months; i++) {
            // Total demand: seasonal + small random component
            double seasonal = 12.0 * Math.sin(i * 2.0 * Math.PI / 12.0);
            double noise = r.nextGaussian() * 2.0;
            double total = 140.0 + seasonal + noise;
            total = clamp(total, 120.0, 165.0);

            // Share trajectories (0..1)
            double t = i / (double) (months - 1);

            // Renewables rise 28% -> 52% with a mid-period acceleration
            double policyBoost = (t > 0.55) ? 0.03 * (t - 0.55) : 0.0;
            double renewShare = lerp(0.28, 0.52, t) + policyBoost + r.nextGaussian() * 0.01;
            // Fossil falls 55% -> 30%
            double fossilShare = lerp(0.55, 0.30, t) + r.nextGaussian() * 0.01;
            // Nuclear roughly stable 17% -> 18%
            double nuclearShare = 1.0 - renewShare - fossilShare;

            // Keep shares sane; nuclear absorbs residual to keep totals stable.
            renewShare = clamp(renewShare, 0.18, 0.65);
            fossilShare = clamp(fossilShare, 0.15, 0.70);
            nuclearShare = clamp(nuclearShare, 0.08, 0.30);

            // Normalize to exactly 1.0 (avoid drift)
            double sum = renewShare + fossilShare + nuclearShare;
            renewShare /= sum;
            fossilShare /= sum;
            nuclearShare /= sum;

            double renewGW = total * renewShare;
            double fossilGW = total * fossilShare;
            double nuclearGW = total * nuclearShare;

            // Only label full years to keep tooltips readable.
            String label = (i % 12 == 0) ? String.format("%d", startYear + (i / 12)) : String.format("%d-%02d", startYear + (i / 12), (i % 12) + 1);

            renewable.addPoint(i, renewGW, 0, label);
            fossil.addPoint(i, fossilGW, 0, label);
            nuclear.addPoint(i, nuclearGW, 0, label);
        }

        return ArberChartBuilder.create()
                .withTitle("Area Chart - Energy Mix")
                .addLayer(renewable, new StackedAreaRenderer())
                .addLayer(fossil, new StackedAreaRenderer())
                .addLayer(nuclear, new StackedAreaRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
