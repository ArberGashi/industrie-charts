package com.arbergashi.charts.domain.legend;

import java.util.Map;

/**
 * A single row in the interactive legend.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class LegendSeriesRow {
    private String id;
    private String name;
    private int argb;
    private boolean visible;
    private boolean hasSettings;
    private Map<String, Object> values;

    public LegendSeriesRow(String id, String name, int argb, boolean visible, boolean hasSettings, Map<String, Object> values) {
        this.id = id;
        this.name = name;
        this.argb = argb;
        this.visible = visible;
        this.hasSettings = hasSettings;
        this.values = values;
    }

    public String getId() {
        return id;
    }

    public LegendSeriesRow setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public LegendSeriesRow setName(String name) {
        this.name = name;
        return this;
    }

    public int getArgb() {
        return argb;
    }

    public LegendSeriesRow setArgb(int argb) {
        this.argb = argb;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public LegendSeriesRow setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isHasSettings() {
        return hasSettings;
    }

    public LegendSeriesRow setHasSettings(boolean hasSettings) {
        this.hasSettings = hasSettings;
        return this;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public LegendSeriesRow setValues(Map<String, Object> values) {
        this.values = values;
        return this;
    }
}
