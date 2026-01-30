package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.VentilatorWaveformRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MedicalVentilatorTest {

    @Test
    public void ventilatorRendererSmokeAndNameTranslation() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(1024, 3);
        // populate some points
        for (int i = 0; i < 200; i++) {
            double t = i * 0.01;
            double p = Math.sin(t) * 5.0 + 50.0; // pressure
            double v = Math.cos(t) * 2.0 + 10.0; // volume
            double f = Math.sin(t * 1.5) * 1.0;   // flow
            model.add(t, new double[]{p, v, f});
        }

        VentilatorWaveformRenderer r = new VentilatorWaveformRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("renderer.ventilatorwaveformrenderer", r.getName());

            // test translator
            VentilatorWaveformRenderer.setNameTranslator(k -> "X-" + k);
            assertEquals("X-renderer.ventilatorwaveformrenderer", r.getName());
        } finally {
            g2.dispose();
            // restore default translator
            VentilatorWaveformRenderer.setNameTranslator(null);
        }
    }
}
