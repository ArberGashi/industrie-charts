package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.AutocorrelationRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

import java.util.Random;

public class AutocorrelationPanelProvider {
    public static ArberChartPanel create() {
        // Production-grade ACF demo:
        // We provide a clean, stationary series so the ACF shows crisp seasonal peaks.
        // Story: monthly demand index with clear 12-month seasonality + mild harmonics.
        // Implementation choices:
        // - no linear trend (ACF stays interpretable)
        // - mean roughly centered
        // - realistic noise (but not too strong)
        // Deterministic for QA/screenshot reproducibility.

        DefaultChartModel model = new DefaultChartModel("Monthly demand (stationary)");

        int n = 120; // 10 years monthly -> ACF peaks at 12, 24, 36, ... are very visible
        double baseline = 0.0;

        Random rnd = new Random(DemoPanelUtils.DEMO_SEED + 1200);

        for (int t = 0; t < n; t++) {
            // 12-month seasonality (dominant)
            double annual = 1.00 * Math.sin((t - 2) * Math.PI / 6.0);
            // weaker semiannual
            double semi = 0.35 * Math.sin((t + 1) * Math.PI / 3.0);
            // tiny quarterly
            double quarterly = 0.15 * Math.sin(t * Math.PI / 2.0);

            // small noise
            double noise = rnd.nextGaussian() * 0.18;

            // stationary series around 0
            double longWave = 0.08 * Math.sin(t * Math.PI / 24.0);
            double y = baseline + annual + semi + quarterly + longWave + noise;

            // Label is used by tooltips; renderer uses Y as ordered series.
            // We make it explicit this is monthly sampling and what peaks to expect.
            String label;
            int monthInYear = (t % 12) + 1;
            if (monthInYear == 1) {
                label = String.format("t=%d (Jan) – expect ACF peaks at lags 12,24,36…", t);
            } else {
                label = String.format("t=%d (Month %d)", t, monthInYear);
            }

            model.addPoint(t, y, 0, label);
        }

        AutocorrelationRenderer renderer = new AutocorrelationRenderer();

        return ArberChartBuilder.create()
                .withTitle("Autocorrelation (ACF) – 12‑Month Seasonality")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
