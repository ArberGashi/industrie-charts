package com.arbergashi.charts.model;
import com.arbergashi.charts.api.types.ArberColor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Default implementation for a flow-based chart model.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class DefaultFlowChartModel implements FlowChartModel {

    private final List<DefaultNode> nodes;
    private final List<DefaultLink> links;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private String name;

    public DefaultFlowChartModel(List<DefaultNode> nodes, List<DefaultLink> links) {
        this.nodes = nodes;
        this.links = links;
    }

    @Override
    public java.util.List<? extends Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public java.util.List<? extends Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

    @Override
    public String getName() {
        return name;
    }

    public DefaultFlowChartModel setName(String name) {
        this.name = name;
        updateStamp.incrementAndGet();
        return this;
    }

    @Override
    public ArberColor getColor() {
        return null;
    } // ArberColor is usually per-node/link

    @Override
    public DefaultFlowChartModel setColor(ArberColor color) {
        return this;
    }

    @Override
    public long getUpdateStamp() {
        return updateStamp.get();
    }

    // --- Legacy/Unsupported methods ---
    public int getPointCount() {
        return nodes != null ? nodes.size() : 0;
    }

    public double getX(int index) {
        return 0;
    }

    public double getY(int index) {
        return 0;
    }

    public void setPoint(ChartPoint point) {
        throw new UnsupportedOperationException();
    }

    public DefaultFlowChartModel setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void setChangeListener(ChartModelListener listener) {
    }

    public void removeChangeListener(ChartModelListener listener) {
    }

    @Override
    public double[] getXData() {
        return new double[0];
    }

    @Override
    public double[] getYData() {
        return new double[0];
    }

    public static class DefaultNode implements Node {
        private final String id;
        private final String label;

        public DefaultNode(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    public static class DefaultLink implements Link {
        private final String source;
        private final String target;
        private final double value;

        public DefaultLink(String source, String target, double value) {
            this.source = source;
            this.target = target;
            this.value = value;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public String getTarget() {
            return target;
        }

        @Override
        public double getValue() {
            return value;
        }
    }
}
