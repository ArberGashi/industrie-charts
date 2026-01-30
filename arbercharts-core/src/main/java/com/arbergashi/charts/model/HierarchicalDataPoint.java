package com.arbergashi.charts.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a hierarchical data structure, used for charts like Sunburst or Treemaps.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class HierarchicalDataPoint {
    private String name;
    private double value;
    private List<HierarchicalDataPoint> children;

    public HierarchicalDataPoint(String name, double value, List<HierarchicalDataPoint> children) {
        this.name = name;
        this.value = value;
        this.children = children != null ? children : new ArrayList<>();
    }

    public HierarchicalDataPoint(String name, double value) {
        this(name, value, new ArrayList<>());
    }

    public String getName() {
        return name;
    }

    public HierarchicalDataPoint setName(String name) {
        this.name = name;
        return this;
    }

    public double getValue() {
        return value;
    }

    public HierarchicalDataPoint setValue(double value) {
        this.value = value;
        return this;
    }

    public List<HierarchicalDataPoint> getChildren() {
        return children;
    }

    public HierarchicalDataPoint setChildren(List<HierarchicalDataPoint> children) {
        this.children = children != null ? children : new ArrayList<>();
        return this;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
