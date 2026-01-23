package com.arbergashi.charts.ui.legend;

import org.junit.jupiter.api.Test;

import java.awt.*;

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
        Rectangle bounds = new Rectangle(0, 0, 100, 80);
        Dimension size = new Dimension(500, 500);

        Rectangle placed = LegendPosition.BOTTOM_RIGHT.place(bounds, size, new Insets(10, 10, 10, 10));
        assertEquals(0, placed.x);
        assertEquals(0, placed.y);
        assertEquals(100, placed.width);
        assertEquals(80, placed.height);
    }

    @Test
    void placeRespectsPaddingForTopLeft() {
        Rectangle bounds = new Rectangle(0, 0, 300, 200);
        Dimension size = new Dimension(100, 40);
        Insets pad = new Insets(12, 16, 20, 24);

        Rectangle placed = LegendPosition.TOP_LEFT.place(bounds, size, pad);
        assertEquals(16, placed.x);
        assertEquals(12, placed.y);
        assertEquals(100, placed.width);
        assertEquals(40, placed.height);
    }

    @Test
    void placeRespectsPaddingForBottomRight() {
        Rectangle bounds = new Rectangle(0, 0, 300, 200);
        Dimension size = new Dimension(100, 40);
        Insets pad = new Insets(12, 16, 20, 24);

        Rectangle placed = LegendPosition.BOTTOM_RIGHT.place(bounds, size, pad);
        assertEquals(300 - 100 - 24, placed.x);
        assertEquals(200 - 40 - 20, placed.y);
    }
}
