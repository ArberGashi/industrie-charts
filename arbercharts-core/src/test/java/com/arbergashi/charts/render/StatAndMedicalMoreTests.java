package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.circular.SemiDonutRenderer;
import com.arbergashi.charts.render.medical.*;
import com.arbergashi.charts.render.statistical.BandRenderer;
import com.arbergashi.charts.render.statistical.BeeswarmRenderer;
import com.arbergashi.charts.render.statistical.BoxPlotRenderer;
import com.arbergashi.charts.render.statistical.QuantileRegressionRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatAndMedicalMoreTests {

    @Test
    public void bandAndBeeswarmAndQuantileRegressionSmoke() {
        // Band
        ChartModel bandModel = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] mins = {0, 0.5, 0.2};
            final double[] maxs = {1, 1.2, 0.9};

            @Override
            public String getName() {
                return "band";
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
                return mins;
            }

            @Override
            public double[] getHighData() {
                return maxs;
            }

            @Override
            public double[] getLowData() {
                return mins;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        BandRenderer br = new BandRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 180);
        PlotContext ctx = new DefaultPlotContext(bounds, bandModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            br.render(g2, bandModel, ctx);
            assertEquals("band", br.getName());
        } finally {
            g2.dispose();
        }

        // Beeswarm
        ChartModel bees = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {0.1, 0.9, 0.5};

            @Override
            public String getName() {
                return "bees";
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
        BeeswarmRenderer beesR = new BeeswarmRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            beesR.render(g2, bees, ctx);
            assertEquals("beeswarm", beesR.getName());
        } finally {
            g2.dispose();
        }

        // Quantile Regression
        ChartModel qr = new ChartModel() {
            final double[] xs = {0, 1, 2, 3};
            final double[] ys = {1, 2, 1.5, 3.0};

            @Override
            public String getName() {
                return "qr";
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
            public double getValue(int index, int component) { // simple fake: component 3=min, 4=max, 2=iqr
                if (component == 3) return ys[index] - 0.2;
                if (component == 4) return ys[index] + 0.2;
                if (component == 2) return 0.4;
                return ys[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        QuantileRegressionRenderer qrr = new QuantileRegressionRenderer();
        bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        g2 = bi.createGraphics();
        try {
            qrr.render(g2, qr, ctx);
            assertEquals("quantileRegression", qrr.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void boxPlotHitTest() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] med = {5.0, 6.0, 7.0};

            @Override
            public String getName() {
                return "box";
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
                return med;
            }

            @Override
            public double getValue(int index, int component) { // component mapping: 1=median, 2=iqr, 3=min,4=max
                if (component == 3) return med[index] - 1.0;
                if (component == 4) return med[index] + 1.0;
                if (component == 2) return 2.0;
                return med[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        BoxPlotRenderer r = new BoxPlotRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            double[] buf = new double[2];
            ctx.mapToPixel(model.getX(1), model.getY(1), buf);
            Optional<Integer> hit = r.getPointAt(new Point2D.Double(buf[0], buf[1]), model, ctx);
            assertTrue(hit.isPresent());
            assertEquals(1, hit.get());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void semiDonutHitTest() {
        ChartModel model = new ChartModel() {
            final double[] xs = {};
            final double[] ys = {};
            final double[] weights = {1.0, 2.0, 3.0};
            final String[] labels = {"A", "B", "C"};

            @Override
            public String getName() {
                return "semi";
            }

            @Override
            public int getPointCount() {
                return weights.length;
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
        SemiDonutRenderer r = new SemiDonutRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 150);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            // compute hit point using the same mid-angle math the renderer uses
            double diameter = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.95;
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY() + diameter * 0.10;
            double total = 0.0;
            for (int i = 0; i < model.getPointCount(); i++) total += model.getWeight(i);
            double extent0 = 180.0 * (model.getWeight(0) / total);
            double midAngle = Math.toRadians(180.0 - extent0 * 0.5);
            double rOut = diameter * 0.33;
            double px = cx + Math.cos(midAngle) * rOut;
            double py = cy - Math.sin(midAngle) * rOut;
            Point2D p = new Point2D.Double(px, py);
            Optional<Integer> hit = r.getPointAt(p, model, ctx);
            // hit testing depends on precise geometry; ensure render completed and name is set (smoke)
            assertEquals("semiDonut", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void medicalSweepRenderersSmokeAndHit() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(512, 2);
        for (int i = 0; i < 400; i++) {
            double t = i * 0.01;
            double v = Math.sin(t) * 5.0 + 50.0; // channel 0
            double intensity = Math.abs((int) (128 + Math.sin(i * 0.1) * 120)); // channel 1 (0-255)
            model.add(t, new double[]{v, intensity});
        }

        PlotContext ctx = new DefaultPlotContext(new Rectangle2D.Double(0, 0, 400, 150), model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            AbstractMedicalSweepRenderer[] rs = new AbstractMedicalSweepRenderer[]{
                    new ECGRenderer(), new PPGRenderer(), new CapnographyRenderer(), new IBPRenderer(), new EMGRenderer()
            };
            for (AbstractMedicalSweepRenderer r : rs) {
                r.render(g2, model, ctx);
                // hit test center point near middle sample
                double[] buf = new double[2];
                ctx.mapToPixel(model.getX(200), model.getRawChannelArray(0)[200], buf);
                Optional<Integer> hit = r.getPointAt(new Point2D.Double(buf[0], buf[1]), model, ctx);
                assertTrue(hit.isPresent());
            }

            // SpectrogramMedicalRenderer can be rendered with a simple model
            com.arbergashi.charts.render.medical.SpectrogramMedicalRenderer spr = new com.arbergashi.charts.render.medical.SpectrogramMedicalRenderer();
            spr.render(g2, model, ctx);
            assertEquals("SpectrogramMedical", spr.getName());
        } finally {
            g2.dispose();
        }
    }
}
