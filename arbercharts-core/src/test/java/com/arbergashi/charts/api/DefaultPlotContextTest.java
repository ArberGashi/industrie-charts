package com.arbergashi.charts.api;

import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultPlotContextTest {

    @Test
    void mapToPixelAndBackIsStable() {
        ArberRect bounds = new ArberRect(0, 0, 100, 100);
        DefaultPlotContext context = new DefaultPlotContext(
                bounds,
                0, 10,
                0, 20,
                false,
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                ChartThemes.getDarkTheme(),
                null,
                null,
                null
        );

        double[] pixel = new double[2];
        double[] data = new double[2];

        context.mapToPixel(5, 10, pixel);
        assertEquals(50.0, pixel[0], 0.0001);
        assertEquals(50.0, pixel[1], 0.0001);

        context.mapToData(pixel[0], pixel[1], data);
        assertEquals(5.0, data[0], 0.0001);
        assertEquals(10.0, data[1], 0.0001);
    }
}
