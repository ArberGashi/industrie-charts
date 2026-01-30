package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.FastMedicalModel;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Contract tests to ensure renderers respect {@code ChartModel#getPointCount()} and
 * do not assume that {@code getXData().length == getPointCount()}.
 *
 * <p>This is critical for models like {@link FastMedicalModel}, which return backing arrays for speed.</p>
 */
public class LineRendererModelSizeContractTest {

    @Test
    void lineRenderer_doesNotReadBeyondPointCount_whenModelReturnsBackingArrays() {
        FastMedicalModel model = new FastMedicalModel("ecg", 1024);
        model.setPoint(0, 0);
        model.setPoint(1, 1);
        model.setPoint(2, 0);

        PlotContext ctx = new DefaultPlotContext(new Rectangle2D.Double(0, 0, 400, 200), model,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            LineRenderer r = new LineRenderer();
            assertDoesNotThrow(() -> r.render(g2, model, ctx));
        } finally {
            g2.dispose();
        }
    }
}
