package com.arbergashi.charts.util;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColorUtilsTest {

    @Test
    public void withAlphaSetsAlpha() {
        Color base = new Color(10, 20, 30);
        Color r = ColorUtils.applyAlpha(base, 0.5f);
        assertEquals(10, r.getRed());
        assertEquals(20, r.getGreen());
        assertEquals(30, r.getBlue());
        assertEquals((int) (0.5f * 255), r.getAlpha());
    }

    @Test
    public void adjustBrightnessClamps() {
        Color base = new Color(200, 200, 200);
        Color brighter = ColorUtils.adjustBrightness(base, 1.5);
        assertTrue(brighter.getRed() <= 255);
        assertTrue(brighter.getGreen() <= 255);
        assertTrue(brighter.getBlue() <= 255);

        Color darker = ColorUtils.adjustBrightness(base, 0.1);
        assertTrue(darker.getRed() >= 0);
    }

    @Test
    public void getContrastColorDecides() {
        assertEquals(Color.WHITE, ColorUtils.getContrastColor(new Color(10, 10, 10)));
        assertEquals(Color.BLACK, ColorUtils.getContrastColor(new Color(250, 250, 250)));
    }

    @Test
    public void interpolateInterpolatesRGBA() {
        Color a = new Color(10, 20, 30, 40);
        Color b = new Color(110, 120, 130, 140);
        Color m = ColorUtils.interpolate(a, b, 0.5f);
        assertEquals((10 + 110) / 2, m.getRed());
        assertEquals((20 + 120) / 2, m.getGreen());
        assertEquals((30 + 130) / 2, m.getBlue());
        assertEquals((40 + 140) / 2, m.getAlpha());
    }
}
