package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless smoke test over the full renderer catalog: every renderer is
 * instantiated and rendered once into an image with its demo model. Catches
 * registry id collisions, broken static initializers, render-path
 * exceptions, and blank output.
 */
class RendererSmokeTest {
    private static final Dimension SIZE = new Dimension(640, 400);

    /**
     * Renderers without a no-arg constructor; they require wiring (delegate
     * renderers, functions) that the demo provides via special-case factories.
     */
    private static final Set<String> REQUIRES_CONSTRUCTOR_ARGS = Set.of(
            "com.arbergashi.charts.render.financial.PredictiveCandleRenderer",
            "com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer",
            "com.arbergashi.charts.render.analysis.VectorFieldRenderer",
            "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer",
            "com.arbergashi.charts.render.predictive.AnomalyGapRenderer",
            "com.arbergashi.charts.render.forensic.PlaybackStatusRenderer",
            "com.arbergashi.charts.render.common.PerformanceAuditRenderer");

    /**
     * Oscillator-style indicators draw in their own value domain, not the
     * price range; they are rendered with an explicit Y view window.
     */
    private static final Map<String, double[]> INDICATOR_Y_RANGES = Map.of(
            "com.arbergashi.charts.render.financial.ADXRenderer", new double[]{0, 100},
            "com.arbergashi.charts.render.financial.ATRRenderer", new double[]{0, 20},
            "com.arbergashi.charts.render.financial.MACDRenderer", new double[]{-10, 10},
            "com.arbergashi.charts.render.financial.StochasticRenderer", new double[]{0, 100});

    /**
     * Known blank in the plain headless path. Tracked 2.1 backlog:
     * - Need attached prediction/animation state:
     *   PredictivePath, PredictiveShadow.
     * - Config-gated overlays, disabled by default (documented behavior):
     *   Calibration, PhysicalScale.
     */
    private static final Set<String> KNOWN_BLANK_HEADLESS = Set.of(
            "com.arbergashi.charts.render.standard.PredictivePathRenderer",
            "com.arbergashi.charts.render.predictive.PredictiveShadowRenderer",
            "com.arbergashi.charts.render.medical.CalibrationRenderer",
            "com.arbergashi.charts.render.common.PhysicalScaleRenderer");

    @TestFactory
    Stream<DynamicTest> everyCatalogRendererRendersHeadless() {
        List<RendererCatalogEntry> entries = RendererCatalog.load().entries();
        assertTrue(entries.size() >= 150, "catalog unexpectedly small: " + entries.size());
        ServerRenderService service = new ServerRenderService(1, false, null);

        return entries.stream()
                .filter(entry -> !REQUIRES_CONSTRUCTOR_ARGS.contains(entry.className()))
                .map(entry -> DynamicTest.dynamicTest(
                        entry.category() + "/" + entry.simpleName(),
                        () -> renderOnce(service, entry)));
    }

    private static void renderOnce(ServerRenderService service, RendererCatalogEntry entry) throws Exception {
        Class<?> type = Class.forName(entry.className());
        ChartRenderer renderer = (ChartRenderer) type.getDeclaredConstructor().newInstance();
        ChartModel model = RendererDemoDataFactory.build(entry.category(), entry.className());
        assertNotNull(model, "no demo model for " + entry.className());

        double[] range = INDICATOR_Y_RANGES.get(entry.className());
        BufferedImage image = (range == null)
                ? service.renderToImage(model, SIZE, ChartThemes.getDarkTheme(), renderer)
                : service.renderToImage(model, SIZE, ChartThemes.getDarkTheme(), renderer, range[0], range[1]);
        assertNotNull(image, "renderer produced no image: " + entry.className());
        if (!KNOWN_BLANK_HEADLESS.contains(entry.className())) {
            assertTrue(hasContent(image), "renderer produced a blank image: " + entry.className());
        }
    }

    private static boolean hasContent(BufferedImage image) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int pixel : pixels) {
            if (pixel != 0) {
                return true;
            }
        }
        return false;
    }
}
