package com.arbergashi.charts.render;

import java.util.Objects;

/**
 * Stable public renderer metadata.
 *
 * <p>This lightweight value object is intended for UI discovery, navigation trees,
 * and general introspection in consuming applications. It intentionally contains only
 * stable identifiers and resource keys, not factories or Swing types.</p>
 *
 * <p><b>Framework contract:</b> This type is part of the public API and will remain
 * backwards compatible across 1.x releases.</p>
 *
 * @param id unique renderer id used for lookup/registration
 * @param nameKey i18n key for the renderer display name
 * @param iconPath classpath resource path to an SVG icon (e.g. {@code "/icons/line.svg"})
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-17
 */
public record RendererMetadata(String id, String nameKey, String iconPath) {

    public RendererMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(iconPath, "iconPath");
    }
}
