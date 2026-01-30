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

        int n = model.getPointCount();
        double[] xs = model.getXData();
        double[] ys = model.getYData();
        int count = Math.min(n, Math.min(xs.length, ys.length));
        Assertions.assertTrue(count >= 0);

        for (int i = 1; i < count; i++) {
            Assertions.assertTrue(xs[i] >= xs[i - 1], "X values must be monotonic for sequential inputs.");
        }

        double[] range = model.getDataRange();
        if (count > 0) {
            Assertions.assertTrue(range[0] <= range[1], "minX <= maxX");
            Assertions.assertTrue(range[2] <= range[3], "minY <= maxY");
        }
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
