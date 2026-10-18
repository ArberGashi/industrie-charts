package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSignalChartModelTest {

    @Test
    void timestampsFollowLogicalSamplesAcrossWrap() {
        DefaultSignalChartModel model = new DefaultSignalChartModel(2, 3, true);
        for (int sample = 1; sample <= 7; sample++) {
            model.setSample(sample, sample * 100L, new double[]{sample * 10.0, sample * 1000.0}, "s" + sample);
            int first = Math.max(1, sample - 2);
            for (int index = 0; index < model.getPointCount(); index++) {
                int expectedSample = first + index;
                assertEquals(expectedSample, model.getX(index));
                assertEquals(expectedSample * 100L, model.getTimestampNanos(index));
                assertEquals("s" + expectedSample, model.getLabel(index));
            }
        }
    }

    @Test
    void channelAccessorsFollowLogicalSamplesAcrossWrap() {
        DefaultSignalChartModel model = new DefaultSignalChartModel(2, 3, true);
        for (int sample = 1; sample <= 7; sample++) {
            model.setSample(sample, sample * 100L, new double[]{sample * 10.0, sample * 1000.0}, null);
            int first = Math.max(1, sample - 2);
            for (int index = 0; index < model.getPointCount(); index++) {
                int expectedSample = first + index;
                assertEquals(expectedSample * 10.0, model.getY(index));
                assertEquals(expectedSample * 10.0, model.getY(index, 0));
                assertEquals(expectedSample * 1000.0, model.getY(index, 1));
            }
        }
    }

    @Test
    void timestampAndChannelAccessorsFollowSamplesAcrossArrayGrowth() {
        DefaultSignalChartModel model = new DefaultSignalChartModel(2, 1);
        for (int sample = 1; sample <= 5; sample++) {
            model.setSample(sample, sample * 100L, new double[]{sample * 10.0, sample * 1000.0}, null);
        }

        for (int index = 0; index < model.getPointCount(); index++) {
            assertEquals((index + 1) * 100L, model.getTimestampNanos(index));
            assertEquals((index + 1) * 1000.0, model.getY(index, 1));
        }
    }

    @Test
    void timestampAndChannelAccessorsReturnSafeDefaultsForInvalidIndices() {
        for (boolean circular : new boolean[]{false, true}) {
            DefaultSignalChartModel model = new DefaultSignalChartModel(2, 3, circular);
            assertEquals(0L, model.getTimestampNanos(0));
            assertEquals(0.0, model.getY(0, 1));
            model.setSample(1, 100L, new double[]{10.0, 1000.0}, null);

            for (int index : new int[]{-1, 1, 3, Integer.MAX_VALUE}) {
                assertEquals(0L, model.getTimestampNanos(index));
                assertEquals(0.0, model.getY(index, 0));
                assertEquals(0.0, model.getY(index, 1));
            }
            assertEquals(0.0, model.getY(0, -1));
            assertEquals(0.0, model.getY(0, 2));
        }
    }
}
