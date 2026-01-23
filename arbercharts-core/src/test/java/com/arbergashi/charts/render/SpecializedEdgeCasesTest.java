package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class SpecializedEdgeCasesTest {

    @Test
    public void chordFlowSmallSetNoCrash() {
        com.arbergashi.charts.render.specialized.ChordFlowRenderer r = new com.arbergashi.charts.render.specialized.ChordFlowRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2, 3};
            final double[] ys = {0, 0, 0, 0};
            final double[] weights = {1, 2, 0, 3};
            final String[] labels = {"A:B", "B:C", "C:A", "A:C"};

            @Override
            public String getName() {
                return "chord_small";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public double getWeight(int index) {
                return weights[index];
            }

            @Override
            public String getLabel(int index) {
                return labels[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 500, 400);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(600, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void sankeyMinimalFlow() {
        com.arbergashi.charts.render.specialized.SankeyRenderer r = new com.arbergashi.charts.render.specialized.SankeyRenderer();
        FlowChartModel model = new FlowChartModel() {
            final List<FlowChartModel.Node> nodes = new ArrayList<>();
            final List<FlowChartModel.Link> links = new ArrayList<>();

            {
                nodes.add(new NodeImpl("n1", "Node 1"));
                nodes.add(new NodeImpl("n2", "Node 2"));
                links.add(new LinkImpl("n1", "n2", 5.0));
            }

            @Override
            public String getName() {
                return "sankey_min";
            }

            @Override
            public int getPointCount() {
                return links.size();
            }

            @Override
            public List<? extends FlowChartModel.Node> getNodes() {
                return nodes;
            }

            @Override
            public List<? extends FlowChartModel.Link> getLinks() {
                return links;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 420, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void dependencyWheelOverlapEdgesNoCrash() {
        com.arbergashi.charts.render.specialized.DependencyWheelRenderer r = new com.arbergashi.charts.render.specialized.DependencyWheelRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {0, 0, 0};
            final String[] labels = {"A:B", "A:C", "B:C"};

            @Override
            public String getName() {
                return "dep_wheel";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public String getLabel(int index) {
                return labels[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 360, 360);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(480, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void arcDiagramLargeDenseDecimation() {
        com.arbergashi.charts.render.specialized.ArcDiagramRenderer r = new com.arbergashi.charts.render.specialized.ArcDiagramRenderer();
        final int n = 5000;
        ChartModel model = new ChartModel() {
            final double[] xs = new double[n];
            final double[] ys = new double[n];

            {
                for (int i = 0; i < n; i++) {
                    xs[i] = i;
                    ys[i] = 0;
                }
            }

            @Override
            public String getName() {
                return "arc_dense";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return ys;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 1600, 400);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(1600, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void alluvialMinimalFlowNoCrash() {
        com.arbergashi.charts.render.specialized.AlluvialRenderer r = new com.arbergashi.charts.render.specialized.AlluvialRenderer();
        FlowChartModel model = new FlowChartModel() {
            final List<FlowChartModel.Node> nodes = new ArrayList<>();
            final List<FlowChartModel.Link> links = new ArrayList<>();

            {
                nodes.add(new NodeImpl("s", "S"));
                nodes.add(new NodeImpl("t", "T"));
                links.add(new LinkImpl("s", "t", 1.0));
            }

            @Override
            public String getName() {
                return "alluvial_min";
            }

            @Override
            public int getPointCount() {
                return links.size();
            }

            @Override
            public List<? extends FlowChartModel.Node> getNodes() {
                return nodes;
            }

            @Override
            public List<? extends FlowChartModel.Link> getLinks() {
                return links;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 600, 320);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    // simple test utilities
    static final class NodeImpl implements FlowChartModel.Node {
        private final String id, label;

        NodeImpl(String id, String label) {
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

    static final class LinkImpl implements FlowChartModel.Link {
        private final String s, t;
        private final double v;

        LinkImpl(String s, String t, double v) {
            this.s = s;
            this.t = t;
            this.v = v;
        }

        @Override
        public String getSource() {
            return s;
        }

        @Override
        public String getTarget() {
            return t;
        }

        @Override
        public double getValue() {
            return v;
        }
    }
}
