package com.arbergashi.charts.render.testing;

import com.arbergashi.charts.api.types.ArberColor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasterTestCanvasTest {
    @Test
    void polygonOracleFillsTheInteriorWithoutFillingItsBoundingBox() {
        try (RasterTestCanvas canvas = new RasterTestCanvas(32, 32)) {
            canvas.setColor(new ArberColor(0xFFFF0000));
            canvas.fillPolygon(new float[]{2, 28, 2}, new float[]{2, 2, 28}, 3);
            assertEquals(0xFFFF0000, canvas.pixels()[8 * 32 + 8]);
            assertEquals(0, canvas.pixels()[24 * 32 + 24]);
        }
    }

    @Test
    void rectangleOracleUsesHalfOpenExtentsAndIgnoresEmptyFills() {
        try (RasterTestCanvas canvas = new RasterTestCanvas(32, 32)) {
            canvas.setColor(new ArberColor(0xFFFF0000));
            canvas.fillRect(10, 10, 0, 5);
            canvas.fillRect(10, 10, -2, 5);
            assertTrue(Arrays.stream(canvas.pixels()).allMatch(pixel -> pixel == 0));
            canvas.fillRect(10, 10, 4, 3);
            assertEquals(12, Arrays.stream(canvas.pixels()).filter(pixel -> pixel != 0).count());
        }
    }
}
