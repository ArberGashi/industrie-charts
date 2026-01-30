package com.arbergashi.charts.domain.render;

import java.util.Objects;

/**
 * Stable public renderer descriptor.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class RendererDescriptor {
    private String id;
    private String nameKey;
    private String iconPath;
    private RendererCategory category;

    public RendererDescriptor(String id, String nameKey, String iconPath, RendererCategory category) {
        this.id = Objects.requireNonNull(id, "id");
        this.nameKey = Objects.requireNonNull(nameKey, "nameKey");
        this.iconPath = Objects.requireNonNull(iconPath, "iconPath");
        this.category = Objects.requireNonNull(category, "category");
    }

    public String getId() {
        return id;
    }

    public RendererDescriptor setId(String id) {
        this.id = Objects.requireNonNull(id, "id");
        return this;
    }

    public String getNameKey() {
        return nameKey;
    }

    public RendererDescriptor setNameKey(String nameKey) {
        this.nameKey = Objects.requireNonNull(nameKey, "nameKey");
        return this;
    }

    public String getIconPath() {
        return iconPath;
    }

    public RendererDescriptor setIconPath(String iconPath) {
        this.iconPath = Objects.requireNonNull(iconPath, "iconPath");
        return this;
    }

    public RendererCategory getCategory() {
        return category;
    }

    public RendererDescriptor setCategory(RendererCategory category) {
        this.category = Objects.requireNonNull(category, "category");
        return this;
    }
}
