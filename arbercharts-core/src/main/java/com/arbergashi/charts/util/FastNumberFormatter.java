package com.arbergashi.charts.util;
/**
 * Lightweight formatter for fixed-point numbers without allocations.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class FastNumberFormatter {
    private static final long[] POW10 = {
            1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L,
            10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L
    };

    private FastNumberFormatter() {
    }

    public static void appendFixed(StringBuilder sb, double value, int decimals) {
        if (!Double.isFinite(value)) {
            sb.append("NaN");
            return;
        }
        int dec = Math.max(0, Math.min(decimals, 9));
        long scale = POW10[dec];
        long scaled = Math.round(value * scale);

        if (scaled < 0) {
            sb.append('-');
            scaled = -scaled;
        }

        long intPart = scaled / scale;
        long fracPart = scaled % scale;

        appendUnsignedLong(sb, intPart);
        if (dec > 0) {
            sb.append('.');
            appendFraction(sb, fracPart, dec);
        }
    }

    private static void appendFraction(StringBuilder sb, long value, int width) {
        long divisor = POW10[width - 1];
        for (int i = 0; i < width; i++) {
            long digit = value / divisor;
            sb.append((char) ('0' + digit));
            value %= divisor;
            divisor /= 10;
        }
    }

    private static void appendUnsignedLong(StringBuilder sb, long value) {
        if (value == 0) {
            sb.append('0');
            return;
        }
        char[] buf = new char[20];
        int pos = buf.length;
        long v = value;
        while (v > 0) {
            long q = v / 10;
            int digit = (int) (v - q * 10);
            buf[--pos] = (char) ('0' + digit);
            v = q;
        }
        sb.append(buf, pos, buf.length - pos);
    }
}
