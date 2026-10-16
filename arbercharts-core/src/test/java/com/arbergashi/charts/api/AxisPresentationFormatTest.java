package com.arbergashi.charts.api;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxisPresentationFormatTest {
    @Test
    void tickSpacingPreservesSmallDistinctValuesAndUnits() {
        AxisConfig axis = new AxisConfig().setUnitSuffix("mV");
        assertEquals("0.001 mV", axis.getFormattedValue(0.001, Locale.US, 0.001));
        assertEquals("0.002 mV", axis.getFormattedValue(0.002, Locale.US, 0.001));
        assertEquals("0.000000001 mV", axis.getFormattedValue(1e-9, Locale.US, 1e-9));
        assertEquals("0 mV", axis.getFormattedValue(-1e-16, Locale.US, 0.001));
    }

    @Test
    void explicitAxisLocaleAndPatternOverrideHostDefaults() {
        AxisConfig axis = new AxisConfig();
        assertFalse(axis.isLocaleExplicit());
        assertEquals("1,5", axis.getFormattedValue(1.5, Locale.GERMANY, 0.1));
        axis.setLocale(Locale.US).setLabelFormatPattern("0.000");
        assertTrue(axis.isLocaleExplicit());
        assertEquals("1.500", axis.getFormattedValue(1.5, Locale.GERMANY, 0.1));
    }

    @Test
    void switchingPatternAndLocaleKeepsBothFormatterCachesConsistent() {
        AxisConfig axis = new AxisConfig();
        assertEquals("1.5", axis.getFormattedValue(1.5, Locale.US, 0.1));
        axis.setLabelFormatPattern("0.00");
        assertEquals("1,50", axis.getFormattedValue(1.5, Locale.GERMANY, 0.1));
        axis.setLabelFormatPattern(null);
        assertEquals("1,5", axis.getFormattedValue(1.5, Locale.GERMANY, 0.1));
        assertEquals("1.5", axis.getFormattedValue(1.5, Locale.US, 0.1));
    }
}
