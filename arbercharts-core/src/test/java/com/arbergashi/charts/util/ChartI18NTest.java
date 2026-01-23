package com.arbergashi.charts.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartI18NTest {

    @Test
    void loadsBundleFromResources() {
        String title = ChartI18N.getString("chart.title", Locale.US);
        assertEquals("Chart", title);
    }

    @Test
    void missingKeyUsesFallbackMarker() {
        String value = ChartI18N.getString("chart.__missing__", Locale.US);
        assertEquals("!chart.__missing__!", value);
    }

    @Test
    void defaultLocaleOverrideIsUsed() {
        Locale original = ChartI18N.getDefaultLocale();
        try {
            ChartI18N.setDefaultLocale(Locale.US);
            String direct = ChartI18N.getString("chart.title", Locale.US);
            String viaDefault = ChartI18N.getString("chart.title");
            assertEquals(direct, viaDefault);
        } finally {
            ChartI18N.setDefaultLocale(original);
        }
    }
}
