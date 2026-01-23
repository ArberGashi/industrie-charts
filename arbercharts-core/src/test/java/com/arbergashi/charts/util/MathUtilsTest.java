package com.arbergashi.charts.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathUtilsTest {

    @Test
    public void clampWorks() {
        assertEquals(5.0, MathUtils.clamp(5.0, 0.0, 10.0));
        assertEquals(0.0, MathUtils.clamp(-1.0, 0.0, 10.0));
        assertEquals(10.0, MathUtils.clamp(100.0, 0.0, 10.0));
    }

    @Test
    public void clamp01DoubleAndFloat() {
        assertEquals(0.0, MathUtils.clamp01(-0.5));
        assertEquals(1.0, MathUtils.clamp01(2.0));
        assertEquals(0.5, MathUtils.clamp01(0.5));
        assertEquals(0.0f, MathUtils.clamp01(-0.5f));
        assertEquals(1.0f, MathUtils.clamp01(5.0f));
    }
}
