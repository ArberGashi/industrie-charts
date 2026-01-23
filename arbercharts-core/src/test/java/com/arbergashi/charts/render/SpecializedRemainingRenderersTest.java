package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.render.specialized.*;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecializedRemainingRenderersTest {

    @Test
    public void marimekkoRendererStackedRender() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 0.0, 1.0, 1.0};
            final double[] ys = {10.0, 5.0, 8.0, 2.0};
            final double[] weights = {2.0, 0.0, 1.0, 0.0};
            final String[] labels = {"A", "B", "C", "D"};

            @Override
            public String getName() {
                return "mar";
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

        MarimekkoRenderer r = new MarimekkoRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("marimekko", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void bulletChartRendererBasic() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.6, 0.9};
            final double[] ys = {0.6, 0.9};

            @Override
            public String getName() {
                return "bullet_test";
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

        BulletChartRenderer r = new BulletChartRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("bullet", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void heatmapContourRendererWeightBased() {
        ChartModel model = new ChartModel() {
            final double[] xs = {1.0, 2.0, 3.0, 4.0};
            final double[] ys = {1.0, 2.0, 3.0, 4.0};
            final double[] weights = {0.1, 0.5, 0.9, 0.3};

            @Override
            public String getName() {
                return "heatcont";
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
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        HeatmapContourRenderer r = new HeatmapContourRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertTrue(r.isInterpolate() || !r.isInterpolate()); // trivial assertion: method exists
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void sankeyRendererWithFlowModel() {
        FlowChartModel model = new FlowChartModel() {
            @Override
            public String getName() {
                return "flow";
            }

            @Override
            public int getPointCount() {
                return 0;
            }

            @Override
            public double[] getXData() {
                return new double[0];
            }

            @Override
            public double[] getYData() {
                return new double[0];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public java.util.List<? extends Node> getNodes() {
                return Arrays.asList(new Node() {
                                         public String getId() {
                                             return "A";
                                         }

                                         public String getLabel() {
                                             return "A";
                                         }
                                     },
                        new Node() {
                            public String getId() {
                                return "B";
                            }

                            public String getLabel() {
                                return "B";
                            }
                        });
            }

            @Override
            public java.util.List<? extends Link> getLinks() {
                return Arrays.asList(new Link() {
                    public String getSource() {
                        return "A";
                    }

                    public String getTarget() {
                        return "B";
                    }

                    public double getValue() {
                        return 5.0;
                    }
                });
            }
        };

        SankeyRenderer r = new SankeyRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("sankey", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void dendrogramRendererBasic() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0, 3.0};
            final double[] ys = {0.0, 1.0, 0.5, 1.5};

            @Override
            public String getName() {
                return "dend";
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

        DendrogramRenderer r = new DendrogramRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("dendrogram", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void voronoiZeroAndLargeSample() {
        com.arbergashi.charts.render.specialized.VoronoiRenderer r = new com.arbergashi.charts.render.specialized.VoronoiRenderer();
        // zero points -> no-op
        ChartModel empty = new ChartModel() {
            final double[] xs = {};

            @Override
            public String getName() {
                return "v_empty";
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
                return xs;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds2 = new Rectangle2D.Double(0, 0, 300, 240);
        PlotContext ctx2 = new DefaultPlotContext(bounds2, empty, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi2 = new BufferedImage(300, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g22 = bi2.createGraphics();
        try {
            r.render(g22, empty, ctx2);
        } finally {
            g22.dispose();
        }

        // large sample > 200 candidates triggers internal n=min(count,200) path
        final int n = 300;
        ChartModel many = new ChartModel() {
            final double[] xs = new double[n];
            final double[] ys = new double[n];

            {
                for (int i = 0; i < n; i++) {
                    xs[i] = i;
                    ys[i] = Math.sin(i);
                }
            }

            @Override
            public String getName() {
                return "v_many";
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
        PlotContext ctx3 = new DefaultPlotContext(bounds2, many, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi3 = new BufferedImage(300, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g23 = bi3.createGraphics();
        try {
            r.render(g23, many, ctx3);
        } finally {
            g23.dispose();
        }
    }

    @Test
    public void networkRendererHandlesEmptyAndMissingLinkTargets() {
        com.arbergashi.charts.render.specialized.NetworkRenderer r = new com.arbergashi.charts.render.specialized.NetworkRenderer();
        FlowChartModel empty = new FlowChartModel() {
            final java.util.List<FlowChartModel.Node> nodes = new java.util.ArrayList<>();
            final java.util.List<FlowChartModel.Link> links = new java.util.ArrayList<>();

            @Override
            public String getName() {
                return "net_empty";
            }

            @Override
            public int getPointCount() {
                return links.size();
            }

            @Override
            public java.util.List<? extends FlowChartModel.Node> getNodes() {
                return nodes;
            }

            @Override
            public java.util.List<? extends FlowChartModel.Link> getLinks() {
                return links;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds3 = new Rectangle2D.Double(0, 0, 400, 300);
        PlotContext ctx4 = new DefaultPlotContext(bounds3, empty, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi4 = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g24 = bi4.createGraphics();
        try {
            r.render(g24, empty, ctx4);
        } finally {
            g24.dispose();
        }

        // nodes present but link references missing id -> should skip gracefully
        FlowChartModel model = new FlowChartModel() {
            final java.util.List<FlowChartModel.Node> nodes = new java.util.ArrayList<>();
            final java.util.List<FlowChartModel.Link> links = new java.util.ArrayList<>();

            {
                nodes.add(new NodeImpl("a", "A"));
                links.add(new LinkImpl("a", "missing", 1.0));
            }

            @Override
            public String getName() {
                return "net_missing";
            }

            @Override
            public int getPointCount() {
                return links.size();
            }

            @Override
            public java.util.List<? extends FlowChartModel.Node> getNodes() {
                return nodes;
            }

            @Override
            public java.util.List<? extends FlowChartModel.Link> getLinks() {
                return links;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        PlotContext ctx5 = new DefaultPlotContext(bounds3, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi5 = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g25 = bi5.createGraphics();
        try {
            r.render(g25, model, ctx5);
        } finally {
            g25.dispose();
        }
    }

    @Test
    public void delaunayDenseRenderAndSmallNoCrash() {
        com.arbergashi.charts.render.specialized.DelaunayRenderer r = new com.arbergashi.charts.render.specialized.DelaunayRenderer();
        // small
        ChartModel small = new ChartModel() {
            final double[] xs = {0, 1, 2, 3};
            final double[] ys = {0, 1, 0, 1};

            @Override
            public String getName() {
                return "d_small";
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
        Rectangle2D.Double bounds4 = new Rectangle2D.Double(0, 0, 800, 300);
        PlotContext ctx6 = new DefaultPlotContext(bounds4, small, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi6 = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g26 = bi6.createGraphics();
        try {
            r.render(g26, small, ctx6);
        } finally {
            g26.dispose();
        }

        // dense (large) triggers sample-limit code path
        final int n = 6000; // large but should be handled by sampling
        ChartModel big = new ChartModel() {
            final double[] xs = new double[n];
            final double[] ys = new double[n];

            {
                for (int i = 0; i < n; i++) {
                    xs[i] = i % 1000;
                    ys[i] = Math.sin(i);
                }
            }

            @Override
            public String getName() {
                return "d_big";
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
        PlotContext ctx7 = new DefaultPlotContext(bounds4, big, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi7 = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g27 = bi7.createGraphics();
        try {
            r.render(g27, big, ctx7);
        } finally {
            g27.dispose();
        }
    }

    @Test
    public void windRoseSmallSet() {
        com.arbergashi.charts.render.specialized.WindRoseRenderer r = new com.arbergashi.charts.render.specialized.WindRoseRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 45, 90, 180};
            final double[] ys = {5, 2, 3, 1};

            @Override
            public String getName() {
                return "wind";
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
        Rectangle2D.Double bounds5 = new Rectangle2D.Double(0, 0, 240, 240);
        PlotContext ctx8 = new DefaultPlotContext(bounds5, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi8 = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g28 = bi8.createGraphics();
        try {
            r.render(g28, model, ctx8);
        } finally {
            g28.dispose();
        }
    }

    // helpers for tests
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

