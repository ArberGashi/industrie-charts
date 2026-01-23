package com.arbergashi.charts.internal;

import java.util.Objects;

/**
 * Metadata descriptor for renderers to be used by RendererRegistry and the UI.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public record RendererDescriptor(String id, String nameKey, String iconPath) {
    public RendererDescriptor(String id, String nameKey, String iconPath) {
        this.id = Objects.requireNonNull(id);
        this.nameKey = Objects.requireNonNull(nameKey);
        this.iconPath = Objects.requireNonNull(iconPath);
    }
}

