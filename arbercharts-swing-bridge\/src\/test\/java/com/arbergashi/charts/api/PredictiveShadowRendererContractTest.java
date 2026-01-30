package com.arbergashi.charts.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import java.awt.geom.Rectangle2D;
import org.junit.jupiter.api.Test;

class PredictiveShadowRendererContractTest {

    @Test
    void predictionPublishesResidualStatsAndAlignedAuditSamples() {
        DefaultChartModel model = new DefaultChartModel("predictive-contract");
        for (int i = 0; i < 64; i++) {
            model.setXY(i, Math.sin(i * 0.1));
        }

        PredictiveShadowRenderer shadow = new PredictiveShadowRenderer(new HarmonicOscillatorPredictor());
        CartesianPlotContext ctx = new CartesianPlotContext(new Rectangle2D.Double(0, 0, 800, 400),
                0.0, 80.0, -2.0, 2.0);

        shadow.ensurePredicted(model, ctx);

        double dx = model.getX(model.getPointCount() - 1) - model.getX(model.getPointCount() - 2);
        double nextX = model.getX(model.getPointCount() - 1) + dx;

        assertTrue(shadow.residualStd() >= 0.0);
        assertTrue(shadow.residualScale() > 0.0);
        assertTrue(Double.isFinite(shadow.predictedForX(nextX)));
    }
}

