package com.arbergashi.charts.platform.swing.legend;

import com.arbergashi.charts.domain.legend.LegendPosition;
import com.arbergashi.charts.core.geometry.ArberInsets;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.geometry.ArberSize;
import com.arbergashi.charts.render.legend.LegendLayoutTransformer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegendPositionTest {

    @Test
    void parseAcceptsDashedTokens() {
        assertEquals(LegendPosition.TOP_LEFT, LegendPosition.parse("top-left", LegendPosition.BOTTOM_RIGHT));
        assertEquals(LegendPosition.BOTTOM_RIGHT, LegendPosition.parse("bottom-right", LegendPosition.TOP_LEFT));
        assertEquals(LegendPosition.TOP_CENTER, LegendPosition.parse("TOP-CENTER", LegendPosition.TOP_LEFT));
    }

    @Test
    void parseFallsBackOnInvalid() {
        assertEquals(LegendPosition.TOP_LEFT, LegendPosition.parse("nope", LegendPosition.TOP_LEFT));
        assertEquals(LegendPosition.BOTTOM_CENTER, LegendPosition.parse(null, LegendPosition.BOTTOM_CENTER));
        assertEquals(LegendPosition.BOTTOM_CENTER, LegendPosition.parse(" ", LegendPosition.BOTTOM_CENTER));
    }

    @Test
    void placeClampsIntoBounds() {
        ArberRect bounds = new ArberRect(0, 0, 100, 80);
        ArberSize size = new ArberSize(500, 500);

        ArberRect placed = LegendLayoutTransformer.place(LegendPosition.BOTTOM_RIGHT, bounds, size, new ArberInsets(10, 10, 10, 10));
        assertEquals(0, placed.x());
        assertEquals(0, placed.y());
        assertEquals(100, placed.width());
        assertEquals(80, placed.height());
    }

    @Test
    void placeRespectsPaddingForTopLeft() {
        ArberRect bounds = new ArberRect(0, 0, 300, 200);
        ArberSize size = new ArberSize(100, 40);
        ArberInsets pad = new ArberInsets(12, 16, 20, 24);

        ArberRect placed = LegendLayoutTransformer.place(LegendPosition.TOP_LEFT, bounds, size, pad);
        assertEquals(16, placed.x());
        assertEquals(12, placed.y());
        assertEquals(100, placed.width());
        assertEquals(40, placed.height());
    }

    @Test
    void placeRespectsPaddingForBottomRight() {
        ArberRect bounds = new ArberRect(0, 0, 300, 200);
        ArberSize size = new ArberSize(100, 40);
        ArberInsets pad = new ArberInsets(12, 16, 20, 24);

        ArberRect placed = LegendLayoutTransformer.place(LegendPosition.BOTTOM_RIGHT, bounds, size, pad);
        assertEquals(300 - 100 - 24, placed.x());
        assertEquals(200 - 40 - 20, placed.y());
    }
}
