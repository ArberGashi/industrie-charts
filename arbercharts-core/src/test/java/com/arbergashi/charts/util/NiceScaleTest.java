package com.arbergashi.charts.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NiceScaleTest {
    @Test
    void ordinaryRangesRetainHumanReadableSpacing() {
        NiceScale scale = new NiceScale(0, 10).setMaxTicks(3);
        assertArrayEquals(new double[]{0, 5, 10}, values(scale));
        scale.setRange(-2, 2);
        assertArrayEquals(new double[]{-2, 0, 2}, values(scale));
    }

    @Test
    void narrowRangesAtLargeOffsetsHaveDistinctRepresentableTicks() {
        for (double min : new double[]{1e16, -1e16}) {
            NiceScale scale = new NiceScale(min, min + 4);
            assertArrayEquals(new double[]{min, min + 2, min + 4}, values(scale));
            List<Double> visited = new ArrayList<>();
            scale.forEachTick(visited::add);
            assertEquals(List.of(min, min + 2, min + 4), visited);
        }
    }

    @Test
    void extremeAndSubnormalDomainsProduceFiniteIncreasingBoundedSequences() {
        for (double[] domain : new double[][]{
                {-Double.MAX_VALUE, Double.MAX_VALUE}, {0, Double.MAX_VALUE}, {-Double.MAX_VALUE, 0},
                {Math.nextDown(Double.MAX_VALUE), Double.MAX_VALUE},
                {0, Double.MIN_VALUE}, {-Double.MIN_VALUE, Double.MIN_VALUE},
                {1.7e9, Math.nextUp(Math.nextUp(1.7e9))}, {-1e-300, 1e-300}, {-1e300, 1e-300}}) {
            NiceScale scale = new NiceScale(domain[0], domain[1]);
            double[] ticks = values(scale);
            assertTrue(ticks.length > 0 && ticks.length < 100, Arrays.toString(domain));
            assertTrue(Double.isFinite(scale.getTickSpacing()) && scale.getTickSpacing() > 0);
            for (int i = 0; i < ticks.length; i++) {
                assertTrue(Double.isFinite(ticks[i]));
                assertTrue(ticks[i] >= scale.getNiceMin() && ticks[i] <= scale.getNiceMax());
                if (i > 0) assertTrue(ticks[i] > ticks[i - 1]);
            }
            if (domain[0] <= 0 && domain[1] >= 0) {
                assertTrue(Arrays.stream(ticks).anyMatch(tick -> tick == 0), "The zero tick must not be lost to cancellation");
            }
        }
    }

    @Test
    void constantAndInvalidDomainsDoNotReuseStaleTicks() {
        NiceScale scale = new NiceScale(0, 10);
        assertTrue(values(scale).length > 1);
        for (double value : new double[]{0, -10, Double.MIN_VALUE, Double.MAX_VALUE}) {
            scale.setRange(value, value);
            assertArrayEquals(new double[]{value}, values(scale));
        }
        for (double[] domain : new double[][]{{2, 1}, {Double.NaN, 1}, {0, Double.POSITIVE_INFINITY}}) {
            scale.setRange(domain[0], domain[1]);
            assertEquals(0, scale.getTickCount());
            assertEquals(0, values(scale).length);
            scale.forEachTick(value -> { throw new AssertionError("Invalid range emitted a tick"); });
        }
    }

    @Test
    void invalidTickTargetsCannotCauseUnboundedAllocation() {
        for (double target : new double[]{0, -1, 1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            NiceScale scale = new NiceScale(0, 1).setMaxTicks(target);
            double[] ticks = values(scale);
            assertTrue(ticks.length >= 2 && ticks.length <= 20002);
            assertEquals(0, ticks[0]);
            assertEquals(1, ticks[ticks.length - 1], 1e-12);
        }
    }

    private static double[] values(NiceScale scale) {
        double[] ticks = scale.getTicks();
        return Arrays.copyOf(ticks, scale.getTickCount());
    }
}
