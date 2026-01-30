package com.arbergashi.charts.domain.render;

import java.util.Objects;

/**
 * Stable public renderer metadata.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class RendererMetadata {
    private String id;
    private String nameKey;
    private String iconPath;

    public RendererMetadata(String id, String nameKey, String iconPath) {
        this.id = Objects.requireNonNull(id, "id");
        this.nameKey = Objects.requireNonNull(nameKey, "nameKey");
        this.iconPath = Objects.requireNonNull(iconPath, "iconPath");
    }

    public String getId() {
        return id;
    }

    public RendererMetadata setId(String id) {
        this.id = Objects.requireNonNull(id, "id");
        return this;
    }

    public String getNameKey() {
        return nameKey;
    }

    public RendererMetadata setNameKey(String nameKey) {
        this.nameKey = Objects.requireNonNull(nameKey, "nameKey");
        return this;
    }

    public String getIconPath() {
        return iconPath;
    }

    public RendererMetadata setIconPath(String iconPath) {
        this.iconPath = Objects.requireNonNull(iconPath, "iconPath");
        return this;
    }
}
