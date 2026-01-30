package com.arbergashi.charts.api.types;

/**
 * Core-safe color value (ARGB).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public record ArberColor(int argb) {
    public static final ArberColor TRANSPARENT = new ArberColor(0x00000000);

    public int alpha() {
        return (argb >>> 24) & 0xFF;
    }

    public int red() {
        return (argb >>> 16) & 0xFF;
    }

    public int green() {
        return (argb >>> 8) & 0xFF;
    }

    public int blue() {
        return argb & 0xFF;
    }

    public static ArberColor of(int r, int g, int b, int a) {
        int v = ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (b & 0xFF);
        return new ArberColor(v);
    }
}
