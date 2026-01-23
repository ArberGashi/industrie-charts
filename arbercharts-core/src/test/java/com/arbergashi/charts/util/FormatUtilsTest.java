package com.arbergashi.charts.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatUtilsTest {

    @Test
    void formatsWithLocaleDecimalSeparator() {
        assertEquals("1234.56", FormatUtils.formatAxisLabel(1234.56, Locale.US));
        assertEquals("1234,56", FormatUtils.formatAxisLabel(1234.56, Locale.GERMANY));
    }
}
