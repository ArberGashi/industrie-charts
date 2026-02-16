package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelDefaultMethodContractTest {

    @Test
    void chartModelDefaultsRespectLogicalPointCount() {
        ChartModel model = new ChartModel() {
            @Override
            public String getName() {
                return "stub";
            }

            @Override
            public int getPointCount() {
                return 1;
            }

            @Override
            public double[] getXData() {
                return new double[]{11.0, 99.0, 100.0};
            }

            @Override
            public double[] getYData() {
                return new double[]{22.0, 88.0, 77.0};
            }

            @Override
            public double[] getWeightData() {
                return new double[]{33.0, 66.0, 55.0};
            }

            @Override
            public byte[] getProvenanceFlagsData() {
                return new byte[]{7, 8, 9};
            }

            @Override
            public short[] getSourceIdsData() {
                return new short[]{12, 13, 14};
            }

            @Override
            public long[] getTimestampNanosData() {
                return new long[]{101L, 202L, 303L};
            }

            @Override
            public void setChangeListener(ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModelListener listener) {
            }
        };

        assertEquals(11.0, model.getX(0), 0.0);
        assertEquals(22.0, model.getY(0), 0.0);
        assertEquals(33.0, model.getWeight(0), 0.0);
        assertEquals(7, model.getProvenanceFlag(0));
        assertEquals(12, model.getSourceId(0));
        assertEquals(101L, model.getTimestampNanos(0));

        assertEquals(0.0, model.getX(1), 0.0);
        assertEquals(0.0, model.getY(1), 0.0);
        assertEquals(0.0, model.getWeight(1), 0.0);
        assertEquals(ProvenanceFlags.ORIGINAL, model.getProvenanceFlag(1));
        assertEquals(0, model.getSourceId(1));
        assertEquals(0L, model.getTimestampNanos(1));
    }

    @Test
    void specializedModelDefaultsRespectLogicalPointCount() {
        FinancialChartModel financial = new FinancialChartModel() {
            @Override
            public String getName() {
                return "financial";
            }

            @Override
            public int getPointCount() {
                return 1;
            }

            @Override
            public double[] getOpenData() {
                return new double[]{1.0, 9.0};
            }

            @Override
            public double[] getHighData() {
                return new double[]{2.0, 9.0};
            }

            @Override
            public double[] getLowData() {
                return new double[]{0.5, 9.0};
            }

            @Override
            public double[] getCloseData() {
                return new double[]{1.5, 9.0};
            }

            @Override
            public void setChangeListener(ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModelListener listener) {
            }
        };

        SignalChartModel signal = new SignalChartModel() {
            @Override
            public int getChannelCount() {
                return 1;
            }

            @Override
            public double[] getChannelData(int channel) {
                return new double[]{4.0, 99.0};
            }

            @Override
            public String getName() {
                return "signal";
            }

            @Override
            public int getPointCount() {
                return 1;
            }

            @Override
            public void setChangeListener(ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModelListener listener) {
            }
        };

        StatisticalChartModel statistical = new StatisticalChartModel() {
            @Override
            public String getName() {
                return "statistical";
            }

            @Override
            public int getPointCount() {
                return 1;
            }

            @Override
            public double[] getYData() {
                return new double[]{5.0, 99.0};
            }

            @Override
            public double[] getQ1Data() {
                return new double[]{4.0, 99.0};
            }

            @Override
            public double[] getQ3Data() {
                return new double[]{6.0, 99.0};
            }

            @Override
            public double[] getLowData() {
                return new double[]{3.0, 99.0};
            }

            @Override
            public double[] getHighData() {
                return new double[]{7.0, 99.0};
            }

            @Override
            public void setChangeListener(ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModelListener listener) {
            }
        };

        assertEquals(1.0, financial.getOpen(0), 0.0);
        assertEquals(0.0, financial.getOpen(1), 0.0);
        assertEquals(2.0, financial.getHigh(0), 0.0);
        assertEquals(0.0, financial.getHigh(1), 0.0);
        assertEquals(0.5, financial.getLow(0), 0.0);
        assertEquals(0.0, financial.getLow(1), 0.0);
        assertEquals(1.5, financial.getClose(0), 0.0);
        assertEquals(0.0, financial.getClose(1), 0.0);

        assertEquals(4.0, signal.getValue(0, 0), 0.0);
        assertEquals(0.0, signal.getValue(1, 0), 0.0);

        assertEquals(5.0, statistical.getMedian(0), 0.0);
        assertEquals(0.0, statistical.getMedian(1), 0.0);
        assertEquals(4.0, statistical.getQ1(0), 0.0);
        assertEquals(0.0, statistical.getQ1(1), 0.0);
        assertEquals(6.0, statistical.getQ3(0), 0.0);
        assertEquals(0.0, statistical.getQ3(1), 0.0);
        assertEquals(3.0, statistical.getMin(0), 0.0);
        assertEquals(0.0, statistical.getMin(1), 0.0);
        assertEquals(7.0, statistical.getMax(0), 0.0);
        assertEquals(0.0, statistical.getMax(1), 0.0);
    }
}
