package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ChartModelDataRangeTest {

    @Test
    void dataRangeRespectsPointCount() {
        ChartModel model = new ChartModel() {
            private final double[] xs = {1, 2, 3, 1000};
            private final double[] ys = {-1, 0, 1, 999};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return 3;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        assertArrayEquals(new double[]{1, 3, -1, 1}, model.getDataRange());
    }

    @Test
    void dataRangeIgnoresNonFiniteValues() {
        ChartModel model = new ChartModel() {
            private final double[] xs = {1, Double.NaN, 2, Double.POSITIVE_INFINITY};
            private final double[] ys = {5, 6, Double.NaN, 7};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return 4;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        assertArrayEquals(new double[]{1, 2, 5, 7}, model.getDataRange());
    }

    @Test
    void dataRangeHandlesEmptyAndSinglePoint() {
        ChartModel empty = new ChartModel() {
            @Override
            public String getName() {
                return "empty";
            }

            @Override
            public int getPointCount() {
                return 0;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        assertArrayEquals(new double[]{0, 0, 0, 0}, empty.getDataRange());

        ChartModel single = new ChartModel() {
            private final double[] xs = {3};
            private final double[] ys = {-2};

            @Override
            public String getName() {
                return "single";
            }

            @Override
            public int getPointCount() {
                return 1;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        assertArrayEquals(new double[]{3, 3, -2, -2}, single.getDataRange());
    }
}
