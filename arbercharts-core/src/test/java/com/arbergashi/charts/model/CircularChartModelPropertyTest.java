package com.arbergashi.charts.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class CircularChartModelPropertyTest {

    @Property
    void snapshotsAreConsistent(@ForAll("series") List<Double> values) {
        CircularChartModel model = new CircularChartModel(2048);
        for (int i = 0; i < values.size(); i++) {
            double y = values.get(i);
            model.setPoint(i, y, y, y, 1.0, null);
        }

        int expectedCount = Math.min(values.size(), 2048);
        int first = values.size() - expectedCount;
        Assertions.assertEquals(expectedCount, model.getPointCount());
        double[] xs = model.getXData();
        double[] ys = model.getYData();
        double[] lows = model.getLowData();
        double[] highs = model.getHighData();
        double[] weights = model.getWeightData();
        for (double[] snapshot : new double[][]{xs, ys, lows, highs, weights}) {
            Assertions.assertTrue(snapshot.length >= expectedCount,
                    "Every snapshot must contain all retained logical points");
        }

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < expectedCount; i++) {
            double expectedY = values.get(first + i);
            Assertions.assertEquals(first + i, xs[i], "Snapshot X must follow retained insertion order");
            Assertions.assertEquals(expectedY, ys[i]);
            Assertions.assertEquals(expectedY, lows[i]);
            Assertions.assertEquals(expectedY, highs[i]);
            Assertions.assertEquals(1.0, weights[i]);
            minY = Math.min(minY, expectedY);
            maxY = Math.max(maxY, expectedY);
        }

        Assertions.assertArrayEquals(new double[]{first, values.size() - 1, minY, maxY}, model.getDataRange());
    }

    @Provide
    Arbitrary<List<Double>> series() {
        return Arbitraries.doubles()
                .between(-500.0, 500.0)
                .list()
                .ofMinSize(32)
                .ofMaxSize(5000);
    }
}
