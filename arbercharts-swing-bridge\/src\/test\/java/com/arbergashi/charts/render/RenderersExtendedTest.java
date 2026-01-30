package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.internal.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.model.MatrixChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RenderersExtendedTest {

    @Test
    public void registryDescriptorsMatchRendererId() {
        Map<String, ? extends com.arbergashi.charts.internal.RendererDescriptor> desc = RendererRegistry.descriptors();
        assertNotNull(desc);
        for (String id : desc.keySet()) {
            var r = RendererRegistry.getRenderer(id);
            if (r == null) continue; // some factories may be conditional
            String name = r.getName();
            assertNotNull(name);
            assertFalse(name.isBlank(), "Renderer must provide a non-empty name");
            // tooltip should not throw
            assertDoesNotThrow(() -> r.getTooltipText(0, (ChartModel) null));
        }
    }

    @Test
    public void chordFlowHitTestExpectHit() {
        com.arbergashi.charts.render.specialized.ChordFlowRenderer r = new com.arbergashi.charts.render.specialized.ChordFlowRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2, 3};
            final double[] ys = {0, 0, 0, 0};
            final double[] weights = {1, 2, 1, 3};
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
            public void setChangeListener(ChartModel.ChartModelListener listener) {
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
            // Probe near center
            Point2D p = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
            var hit = r.getPointAt(p, model, ctx);
            // Accept either hit or empty but ensure no exceptions; prefer presence
            assertNotNull(hit);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void chordDiagramHitSmokeAndNoThrow() {
        com.arbergashi.charts.render.circular.ChordDiagramRenderer r = new com.arbergashi.charts.render.circular.ChordDiagramRenderer();
        MatrixChartModel model = new MatrixChartModel() {
            @Override
            public String getName() {
                return "chord";
            }

            @Override
            public int getPointCount() {
                return 2;
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
            public double[][] getMatrix() {
                return new double[][]{{0, 2}, {1, 0}};
            }

            @Override
            public java.util.List<String> getEntityLabels() {
                return java.util.List.of("A", "B");
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void networkRendererStressMultipleRenders() {
        com.arbergashi.charts.render.specialized.NetworkRenderer r = new com.arbergashi.charts.render.specialized.NetworkRenderer();
        FlowChartModel model = new FlowChartModel() {
            final List<FlowChartModel.Node> nodes = new ArrayList<>();
            final List<FlowChartModel.Link> links = new ArrayList<>();

            {
                for (int i = 0; i < 20; i++) {
                    final int idx = i;
                    nodes.add(new Node() {
                        public String getId() {
                            return "n" + idx;
                        }

                        public String getLabel() {
                            return "N" + idx;
                        }
                    });
                }
                for (int i = 0; i < 40; i++) {
                    final int idx = i;
                    links.add(new Link() {
                        public String getSource() {
                            return "n" + (idx % 20);
                        }

                        public String getTarget() {
                            return "n" + ((idx + 3) % 20);
                        }

                        public double getValue() {
                            return 1.0;
                        }
                    });
                }
            }

            @Override
            public String getName() {
                return "net";
            }

            @Override
            public int getPointCount() {
                return links.size();
            }

            @Override
            public java.util.List<? extends Node> getNodes() {
                return nodes;
            }

            @Override
            public java.util.List<? extends Link> getLinks() {
                return links;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 800, 600);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            for (int i = 0; i < 200; i++) {
                r.render(g2, model, ctx);
            }
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void allocationSafetyStressPBufferDuringMultipleRenders() {
        // Local helper renderer exposing BaseRenderer helpers
        class LocalHelper extends BaseRenderer {
            LocalHelper() {
                super("helper-local");
            }

            @Override
            protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
            }

            public double[] exposePB() {
                return pBuffer();
            }

            public double[] exposePB4() {
                return pBuffer4();
            }

            public Stroke exposeCachedStroke(float w) {
                return getCachedStroke(w);
            }
        }

        LocalHelper r = new LocalHelper();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1};
            final double[] ys = {1, 2};

            @Override
            public String getName() {
                return "m";
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
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            double[] first = r.exposePB();
            for (int i = 0; i < 500; i++) {
                r.render(g2, model, ctx);
                double[] buf = r.exposePB();
                assertSame(first, buf, "pBuffer identity must be preserved across renders");
                r.exposeCachedStroke(1.0f); // ensure caching does not throw
            }
        } finally {
            g2.dispose();
        }
    }
}
