package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.SpectrogramMedicalRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MedicalRenderersTest {

    @Test
    public void spectrogramRendererSmoke() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(128, 2);
        // populate a little data
        for (int i = 0; i < 64; i++) {
            model.add(i, new double[]{i % 256, (i * 2) % 256});
        }

        SpectrogramMedicalRenderer r = new SpectrogramMedicalRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }

        assertEquals("SpectrogramMedical", r.getName());
    }

    @Test
    public void ultrasoundMModeRendererSmoke() {
        com.arbergashi.charts.render.medical.UltrasoundMModeRenderer r = new com.arbergashi.charts.render.medical.UltrasoundMModeRenderer();
        com.arbergashi.charts.model.FastMedicalModel model = new com.arbergashi.charts.model.FastMedicalModel("us", 64);
        for (int i = 0; i < 64; i++) model.setPoint(i, i % 255);

        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 300, 100);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }

        assertEquals("UltrasoundMMode", r.getName());
    }
}
