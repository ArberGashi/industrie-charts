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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpecializedFinalBatchTest {

    @Test
    public void chordFlowRendererBasic() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {1, 2, 3};
            final String[] labels = {"A:B", "B:C", "A:C"};

            @Override
            public String getName() {
                return "chord";
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

        ChordFlowRenderer r = new ChordFlowRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("chord_flow", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void alluvialRendererWithFlowModel() {
        FlowChartModel model = new FlowChartModel() {
            @Override
            public String getName() {
                return "alluvial";
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
            public List<? extends Node> getNodes() {
                return Arrays.asList(new Node() {
                                         public String getId() {
                                             return "N1";
                                         }

                                         public String getLabel() {
                                             return "N1";
                                         }
                                     },
                        new Node() {
                            public String getId() {
                                return "N2";
                            }

                            public String getLabel() {
                                return "N2";
                            }
                        });
            }

            @Override
            public List<? extends Link> getLinks() {
                return Arrays.asList(new Link() {
                    public String getSource() {
                        return "N1";
                    }

                    public String getTarget() {
                        return "N2";
                    }

                    public double getValue() {
                        return 1.0;
                    }
                });
            }
        };

        AlluvialRenderer r = new AlluvialRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("alluvial", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void arcDiagramAndWindRoseAndJoyplotSmoke() {
        // ArcDiagram
        ChartModel arcModel = new ChartModel() {
            final double[] xs = {0, 1, 2, 3};
            final double[] ys = {0, 0, 0, 0};

            @Override
            public String getName() {
                return "arc";
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
        ArcDiagramRenderer arc = new ArcDiagramRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, arcModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            arc.render(g2, arcModel, ctx);
            assertEquals("arc_diagram", arc.getName());
        } finally {
            g2.dispose();
        }

        // WindRose
        ChartModel wind = new ChartModel() {
            final double[] deg = {0, 90, 180, 270};
            final double[] sp = {5, 3, 4, 2};

            @Override
            public String getName() {
                return "wind";
            }

            @Override
            public int getPointCount() {
                return deg.length;
            }

            @Override
            public double[] getXData() {
                return deg;
            }

            @Override
            public double[] getYData() {
                return sp;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        WindRoseRenderer wr = new WindRoseRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            wr.render(g2, wind, ctx);
            assertEquals("wind_rose", wr.getName());
        } finally {
            g2.dispose();
        }

        // Joyplot
        ChartModel joy = new ChartModel() {
            final double[] xs = new double[200];
            final double[] ys = new double[200];

            {
                for (int i = 0; i < 200; i++) {
                    xs[i] = i;
                    ys[i] = Math.sin(i * 0.1);
                }
            }

            @Override
            public String getName() {
                return "joy";
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
        JoyplotRenderer jr = new JoyplotRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            jr.render(g2, joy, ctx);
            assertEquals("joyplot", jr.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void dependencyWheelParetoNetworkHorizonTernarySmoke() {
        // Dependency wheel
        ChartModel dw = new ChartModel() {
            final double[] xs = {0, 1};
            final double[] ys = {1, 2};
            final String[] lbl = {"A:B", "B:C"};

            @Override
            public String getName() {
                return "dep";
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
                return lbl[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        DependencyWheelRenderer dwr = new DependencyWheelRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, dw, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            dwr.render(g2, dw, ctx);
            assertEquals("dependency_wheel", dwr.getName());
        } finally {
            g2.dispose();
        }

        // Pareto
        ChartModel pr = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {10, 5, 2};

            @Override
            public String getName() {
                return "pareto";
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
        ParetoRenderer par = new ParetoRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            par.render(g2, pr, ctx);
            assertEquals("pareto", par.getName());
        } finally {
            g2.dispose();
        }

        // Network (FlowChartModel)
        FlowChartModel net = new FlowChartModel() {
            @Override
            public String getName() {
                return "net";
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
            public List<? extends Node> getNodes() {
                return Arrays.asList(new Node() {
                    public String getId() {
                        return "N1";
                    }

                    public String getLabel() {
                        return "N1";
                    }
                }, new Node() {
                    public String getId() {
                        return "N2";
                    }

                    public String getLabel() {
                        return "N2";
                    }
                });
            }

            @Override
            public List<? extends Link> getLinks() {
                return Arrays.asList(new Link() {
                    public String getSource() {
                        return "N1";
                    }

                    public String getTarget() {
                        return "N2";
                    }

                    public double getValue() {
                        return 1.0;
                    }
                });
            }
        };
        NetworkRenderer nr = new NetworkRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            nr.render(g2, net, ctx);
            assertEquals("network", nr.getName());
        } finally {
            g2.dispose();
        }

        // Horizon
        ChartModel horizon = new ChartModel() {
            final double[] xs = new double[200];
            final double[] ys = new double[200];

            {
                for (int i = 0; i < 200; i++) {
                    xs[i] = i;
                    ys[i] = Math.sin(i * 0.05);
                }
            }

            @Override
            public String getName() {
                return "horizon";
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
        HorizonRenderer hr = new HorizonRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            hr.render(g2, horizon, ctx);
            assertEquals("horizon", hr.getName());
        } finally {
            g2.dispose();
        }

        // Ternary Phase
        ChartModel tern = new ChartModel() {
            final double[] xs = {1.0, 2.0};
            final double[] ys = {1.0, 1.0};
            final double[] ws = {1.0, 2.0};

            @Override
            public String getName() {
                return "tern";
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
                return ws[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        TernaryPhasediagramRenderer tpr = new TernaryPhasediagramRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            tpr.render(g2, tern, ctx);
            assertEquals("ternary_phase", tpr.getName());
        } finally {
            g2.dispose();
        }
    }
}
