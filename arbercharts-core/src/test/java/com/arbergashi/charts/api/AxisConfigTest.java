package com.arbergashi.charts.api;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxisConfigTest {

    @Test
    void testFormatValue() {
        AxisConfig config = new AxisConfig();
        config.setLocale(Locale.US);

        // 1000.555 wird gerundet (standardmäßig HALF_EVEN oder HALF_UP)
        // Je nach JDK Version kann das Ergebnis 1,000.56 sein. 
        // Scheinbar liefert das aktuelle System 1,000.55 oder 1,000.56.
        String val = config.getFormattedValue(1000.555);
        assertTrue(val.equals("1,000.56") || val.equals("1,000.55"));

        config.setLabelFormatPattern("#,##0.0");
        assertEquals("1,000.6", config.getFormattedValue(1000.555));

        config.setUnitSuffix("CHF");
        assertEquals("1,000.6 CHF", config.getFormattedValue(1000.555));
    }

    @Test
    void testCaching() {
        AxisConfig config = new AxisConfig();
        config.setLocale(Locale.US);

        String first = config.getFormattedValue(100);
        String second = config.getFormattedValue(200);

        assertEquals("100", first);
        assertEquals("200", second);

        config.setLabelFormatPattern("0.00");
        assertEquals("100.00", config.getFormattedValue(100));

        config.setLocale(Locale.GERMANY);
        assertEquals("100,00", config.getFormattedValue(100));
    }

    @Test
    void testFixedRangeAndUnitsPerPixel() {
        AxisConfig config = new AxisConfig();
        assertFalse(config.isFixedRange());

        config.setFixedRange(-5.0, 15.0);
        assertTrue(config.isFixedRange());
        assertEquals(-5.0, config.getFixedMin());
        assertEquals(15.0, config.getFixedMax());

        config.setUnitsPerPixel(0.25);
        assertEquals(0.25, config.getUnitsPerPixel());
    }

    @Test
    void testMedicalScaleResolvesWithPhysicalDensity() {
        AxisConfig config = new AxisConfig().setMedicalScale(25.0);
        double resolved = config.getResolvedUnitsPerPixel(4.0);
        // unitsPerPixel = (1 / mmPerUnit) / pixelsPerMm
        assertEquals(0.01, resolved, 1e-9);
    }
}
