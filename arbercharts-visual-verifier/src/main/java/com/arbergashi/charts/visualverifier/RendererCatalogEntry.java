package com.arbergashi.charts.visualverifier;

public record RendererCatalogEntry(String category, String className, String simpleName) {
    static RendererCatalogEntry of(String category, String className) {
        int idx = className.lastIndexOf('.');
        String simple = idx >= 0 ? className.substring(idx + 1) : className;
        return new RendererCatalogEntry(category, className, simple);
    }
}
