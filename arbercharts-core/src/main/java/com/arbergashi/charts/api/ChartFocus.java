package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.api.types.ArberPoint;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the current focus state of a chart interaction.
  * @author Arber Gashi
  * @version 2.0.0
  * @since 2026-01-30
 */
public final class ChartFocus {
    public static final ChartFocus EMPTY = new ChartFocus(null, Double.NaN, Double.NaN, -1, null, null, Map.of());

    private ArberPoint pixel;
    private double x;
    private double y;
    private int index;
    private ChartModel model;
    private Instant timestamp;
    private Map<String, Object> values;

    public ChartFocus(ArberPoint pixel, double x, double y, int index, ChartModel model, Instant timestamp, Map<String, Object> values) {
        this.pixel = pixel;
        this.x = x;
        this.y = y;
        this.index = index;
        this.model = model;
        this.timestamp = timestamp;
        setValues(values);
    }

    public ArberPoint getPixel() {
        return pixel;
    }

    public ChartFocus setPixel(ArberPoint pixel) {
        this.pixel = pixel;
        return this;
    }

    public double getX() {
        return x;
    }

    public ChartFocus setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public ChartFocus setY(double y) {
        this.y = y;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public ChartFocus setIndex(int index) {
        this.index = index;
        return this;
    }

    public ChartModel getModel() {
        return model;
    }

    public ChartFocus setModel(ChartModel model) {
        this.model = model;
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ChartFocus setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public ChartFocus setValues(Map<String, Object> values) {
        if (values == null) {
            this.values = Map.of();
        } else {
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }
        return this;
    }

    public boolean isActive() {
        return pixel != null && !Double.isNaN(x) && !Double.isNaN(y);
    }

    public Object getValue(String key) {
        if (key == null) {
            return null;
        }
        return values.get(key);
    }

    public ChartFocus setValue(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Map<String, Object> map = new LinkedHashMap<>(values);
        map.put(key, value);
        setValues(map);
        return this;
    }
}
