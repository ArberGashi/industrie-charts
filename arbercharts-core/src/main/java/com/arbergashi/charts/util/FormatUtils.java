package com.arbergashi.charts.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utilities for formatting numbers efficiently.
 * Uses ThreadLocal to avoid allocation in render loops.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class FormatUtils {

    private static final ThreadLocal<FormatState> AXIS_FORMAT = ThreadLocal.withInitial(FormatState::new);

    private static final class FormatState {
        private DecimalFormat format;
        private Locale locale;
    }

    private FormatUtils() {
    }

    public static String formatAxisLabel(double value) {
        return formatAxisLabel(value, Locale.getDefault());
    }

    public static String formatAxisLabel(double value, Locale locale) {
        // Avoid -0.0
        if (Math.abs(value) < 1e-10) value = 0.0;
        Locale loc = (locale != null) ? locale : Locale.getDefault();
        FormatState state = AXIS_FORMAT.get();
        if (state.format == null || state.locale == null || !state.locale.equals(loc)) {
            DecimalFormat df = new DecimalFormat("0.##");
            df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(loc));
            state.format = df;
            state.locale = loc;
        }
        return state.format.format(value);
    }

    public static void clearCache() {
        AXIS_FORMAT.remove();
    }
}
