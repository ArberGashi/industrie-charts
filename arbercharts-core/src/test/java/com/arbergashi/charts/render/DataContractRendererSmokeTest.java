package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.FastMedicalModel;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Market-readiness contract: every registered renderer must be resilient to:
 * <ul>
 *   <li>Backing arrays larger than {@code getPointCount()} (common for fast streaming models)</li>
 *   <li>Non-finite values (NaN/Inf) in incoming measurement data</li>
 *   <li>Trivial datasets (empty / one point)</li>
 * </ul>
 *
 * <p>This test is intentionally strict: if any renderer throws here, the framework is not shippable.</p>
 */
public class DataContractRendererSmokeTest {

    @Test
    void allRegisteredRenderers_mustNotThrow_onRealWorldDataContracts() {
        // Scenario A: backing arrays (capacity 1024, pointCount small) + injected NaN/Inf.
        runAgainstAllRenderers(buildModelWithNonFiniteValues());

        // Scenario B: empty model.
        runAgainstAllRenderers(new FastMedicalModel("empty", 1024));

        // Scenario C: one-point model.
        FastMedicalModel one = new FastMedicalModel("one", 1024);
        one.addPoint(0, 1);
        runAgainstAllRenderers(one);
    }

    private static FastMedicalModel buildModelWithNonFiniteValues() {
        FastMedicalModel model = new FastMedicalModel("contract", 1024);
        model.addPoint(0, 0);
        model.addPoint(1, 1);
        model.addPoint(2, Double.NaN);
        model.addPoint(3, Double.POSITIVE_INFINITY);
        model.addPoint(4, -1);
        return model;
    }

    private static void runAgainstAllRenderers(FastMedicalModel model) {
        PlotContext ctx = new DefaultPlotContext(new Rectangle2D.Double(0, 0, 600, 300), model,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage img = new BufferedImage(600, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            // Use the public registry facade (what customers use).
            var desc = RendererRegistry.descriptors();
            var ids = new ArrayList<>(desc.keySet());
            Collections.sort(ids);
            for (String id : ids) {
                try {
                    var r = RendererRegistry.require(id);
                    r.render(g2, model, ctx);
                } catch (Throwable t) {
                    fail("Renderer '" + id + "' violated market contract with model '" + model.getName() + "': " + t);
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
