package com.arbergashi.charts.api;

import com.arbergashi.charts.model.DefaultChartModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinearLeastSquaresPredictorTest {

    @Test
    void predictsLinearTrend() {
        DefaultChartModel model = new DefaultChartModel("linear");
        for (int i = 0; i < 50; i++) {
            double x = i * 0.04;
            double y = 2.0 * x + 1.0;
            model.setXY(x, y);
        }

        LinearLeastSquaresPredictor predictor = new LinearLeastSquaresPredictor(40, 10, 1e-6, 0.04);
        DefaultPredictionBuffer buffer = new DefaultPredictionBuffer();
        predictor.predict(model, null, buffer);

        assertEquals(10, buffer.count());
        double[] xs = buffer.x();
        double[] ys = buffer.y();
        double[] conf = buffer.confidence();
        assertTrue(xs[0] > model.getX(model.getPointCount() - 1));
        double expectedY0 = 2.0 * xs[0] + 1.0;
        assertEquals(expectedY0, ys[0], 1e-6);
        assertTrue(conf[0] <= 1.0 && conf[0] >= 0.0);
        assertTrue(conf[conf.length - 1] <= conf[0]);
    }
}
