package com.arbergashi.charts.api.types;

/**
 * Core-safe font descriptor.
 * @author Arber Gashi
 * @since 2026-01-30
 */
public record ArberFont(String name, int style, float size) {
    public static final int PLAIN = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
}
