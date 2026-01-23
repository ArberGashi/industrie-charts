package com.arbergashi.charts.render;

import com.arbergashi.charts.util.ChartAssets;

import javax.swing.*;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Descriptor for registered renderers.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-17
 */
public final class RendererDescriptor {
    private final String key;
    private final RendererCategory category;
    private final Supplier<? extends ChartRenderer> supplier;
    private final String iconKey;
    private final String i18nKey;

    public RendererDescriptor(String key, RendererCategory category, Supplier<? extends ChartRenderer> supplier, String iconKey, String i18nKey) {
        this.key = key;
        this.category = category;
        this.supplier = supplier;
        this.iconKey = iconKey;
        this.i18nKey = i18nKey;
    }

    public String getKey() {
        return key;
    }

    public RendererCategory getCategory() {
        return category;
    }

    public ChartRenderer create() {
        return supplier.get();
    }

    public Icon getIcon() {
        return ChartAssets.getScaledIcon(iconKey, 16f);
    }

    public String getLabel(Locale locale) {
        return ChartAssets.getTranslation(i18nKey, locale);
    }
}
