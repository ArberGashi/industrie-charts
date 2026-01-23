package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MedicalEdgeCasesTest {

    @Test
    public void ventilatorTinySampleNoCrash() {
        com.arbergashi.charts.render.medical.VentilatorWaveformRenderer r = new com.arbergashi.charts.render.medical.VentilatorWaveformRenderer();
        com.arbergashi.charts.model.CircularFastMedicalModel model = new com.arbergashi.charts.model.CircularFastMedicalModel(8, 1);
        model.add(0.0, new double[]{5.0});
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(640, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void spectrogramMissingChannelGraceful() {
        com.arbergashi.charts.render.medical.SpectrogramMedicalRenderer r = new com.arbergashi.charts.render.medical.SpectrogramMedicalRenderer();
        com.arbergashi.charts.model.CircularFastMedicalModel model = new com.arbergashi.charts.model.CircularFastMedicalModel(8, 2);
        model.add(0.0, new double[]{1.0, 0.1});
        model.add(1.0, new double[]{0.5, 0.2});
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void ecgMultiChannelHitTest() {
        com.arbergashi.charts.render.medical.ECGRenderer r = new com.arbergashi.charts.render.medical.ECGRenderer();
        com.arbergashi.charts.model.CircularFastMedicalModel model = new com.arbergashi.charts.model.CircularFastMedicalModel(8, 2);
        model.add(0.0, new double[]{0.0, 0.5});
        model.add(1.0, new double[]{1.0, 0.6});
        model.add(2.0, new double[]{2.0, 0.7});
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx); // attempt a hit test at center
            Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
            var opt = r.getPointAt(center, model, ctx);
            // Ensure the method returns non-null Optional
            assertNotNull(opt);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void medicalSweepSparseNoCrash() {
        com.arbergashi.charts.render.medical.MedicalSweepRenderer r = new com.arbergashi.charts.render.medical.MedicalSweepRenderer();
        com.arbergashi.charts.model.CircularFastMedicalModel model = new com.arbergashi.charts.model.CircularFastMedicalModel(16, 1);
        // intentionally do not add points (size == 0)
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(600, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

}
