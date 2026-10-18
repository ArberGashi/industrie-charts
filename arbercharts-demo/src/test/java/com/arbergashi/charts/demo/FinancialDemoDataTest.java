package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialDemoDataTest {
    @Test
    void simplePointAndFigureUsesDiscreteAlternatingPriceColumns() {
        assertPointAndFigureColumns(RendererDemoDataFactory.build("financial",
                "com.arbergashi.charts.render.financial.PointAndFigureRenderer"));
    }

    static void assertPointAndFigureColumns(ChartModel model) {
        int count = model.getPointCount();
        assertTrue(count > 100, "The showcase must contain substantial rising and falling box columns");
        int columns = 0, rising = 0, falling = 0;
        int start = 0;
        double previousPrice = Double.NaN;
        double previousDirection = 0;
        while (start < count) {
            double column = model.getX(start);
            assertEquals(columns, column, 1e-9, "X must identify consecutive columns, not elapsed time");
            double direction = Math.signum(model.getWeight(start));
            assertTrue(direction == 1 || direction == -1, "Each box must explicitly identify X or O direction");
            if (columns > 0) {
                assertEquals(-previousDirection, direction, "Reversals must alternate between X and O columns");
                assertEquals(previousPrice + direction * 2, model.getY(start), 1e-9,
                        "The next column starts one box beyond the preceding turning price");
            }
            int end = start;
            while (end < count && model.getX(end) == column) {
                assertEquals(direction, Math.signum(model.getWeight(end)), "A column cannot mix rising and falling boxes");
                assertEquals(Math.rint(model.getY(end) / 2), model.getY(end) / 2, 1e-9,
                        "Every displayed price must lie on the two-point box grid");
                if (end > start) assertEquals(direction * 2, model.getY(end) - model.getY(end - 1), 1e-9);
                if (direction > 0) rising++;
                else falling++;
                end++;
            }
            assertTrue(end - start >= 3, "Each reversal must form a genuine column of at least three boxes");
            previousPrice = model.getY(end - 1);
            previousDirection = direction;
            columns++;
            start = end;
        }
        assertTrue(columns >= 20 && rising > 50 && falling > 50,
                "The demo must show repeated X/O reversals across the price domain");
    }
}
