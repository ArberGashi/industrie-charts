package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer;
import com.arbergashi.charts.render.analysis.VectorFieldRenderer;
import com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer;
import com.arbergashi.charts.render.financial.PredictiveCandleRenderer;
import com.arbergashi.charts.render.forensic.PlaybackStatusRenderer;
import com.arbergashi.charts.render.predictive.AnomalyGapRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.util.LatencyTracker;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
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
     * Renderers without a no-arg constructor, wired the same way the demo
     * wires them so their render path is exercised too.
     */
    private static final Map<String, Supplier<ChartRenderer>> SPECIAL_FACTORIES = Map.of(
            "com.arbergashi.charts.render.financial.PredictiveCandleRenderer",
            () -> new PredictiveCandleRenderer(new PredictiveShadowRenderer()),
            "com.arbergashi.charts.render.predictive.AnomalyGapRenderer",
            () -> new AnomalyGapRenderer(new PredictiveShadowRenderer()),
            "com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer",
            () -> new AdaptiveFunctionRenderer(Math::sin),
            "com.arbergashi.charts.render.analysis.VectorFieldRenderer",
            () -> new VectorFieldRenderer((x, y, out) -> {
                out[0] = Math.sin(y * 0.2);
                out[1] = Math.cos(x * 0.2);
                return true;
            }),
            "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer",
            () -> new CircularLatencyOverlayRenderer(sampleTracker()),
            "com.arbergashi.charts.render.forensic.PlaybackStatusRenderer",
            () -> new PlaybackStatusRenderer(new StubPlaybackController()));

    /**
     * Catalog entries that are not ChartRenderer implementations at all;
     * the demo wraps them via adapters (naming quirk in the catalog).
     */
    private static final Set<String> NOT_A_CHART_RENDERER = Set.of(
            "com.arbergashi.charts.render.common.PerformanceAuditRenderer");

    private static LatencyTracker sampleTracker() {
        LatencyTracker tracker = new LatencyTracker(256);
        for (int i = 0; i < 200; i++) {
            tracker.record(1_000_000L + (i % 17) * 250_000L);
        }
        return tracker;
    }

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
     * Known blank in the plain single-frame headless path. All of these are
     * exercised (construction + render run exception-free); they draw nothing
     * without additional runtime state:
     * - Prediction state required: PredictivePath, PredictiveShadow,
     *   PredictiveCandle.
     * - Live runtime state required: CircularLatencyOverlay (latency window),
     *   PlaybackStatus (active playback).
     * - Async first frame: AdaptiveFunction computes in the background and
     *   draws from the second frame on (sync fallback is tracked backlog).
     * - Config-gated overlays, disabled by default: Calibration,
     *   PhysicalScale.
     */
    private static final Set<String> KNOWN_BLANK_HEADLESS = Set.of(
            "com.arbergashi.charts.render.standard.PredictivePathRenderer",
            "com.arbergashi.charts.render.predictive.PredictiveShadowRenderer",
            "com.arbergashi.charts.render.financial.PredictiveCandleRenderer",
            "com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer",
            "com.arbergashi.charts.render.forensic.PlaybackStatusRenderer",
            "com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer",
            "com.arbergashi.charts.render.medical.CalibrationRenderer",
            "com.arbergashi.charts.render.common.PhysicalScaleRenderer");

    @TestFactory
    Stream<DynamicTest> everyCatalogRendererRendersHeadless() {
        List<RendererCatalogEntry> entries = RendererCatalog.load().entries();
        assertTrue(entries.size() >= 150, "catalog unexpectedly small: " + entries.size());
        ServerRenderService service = new ServerRenderService(1, false, null);

        return entries.stream()
                .filter(entry -> !NOT_A_CHART_RENDERER.contains(entry.className()))
                .map(entry -> DynamicTest.dynamicTest(
                        entry.category() + "/" + entry.simpleName(),
                        () -> renderOnce(service, entry)));
    }

    private static void renderOnce(ServerRenderService service, RendererCatalogEntry entry) throws Exception {
        Supplier<ChartRenderer> factory = SPECIAL_FACTORIES.get(entry.className());
        ChartRenderer renderer = (factory != null)
                ? factory.get()
                : (ChartRenderer) Class.forName(entry.className()).getDeclaredConstructor().newInstance();
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

    /** Minimal inactive playback wiring for the status renderer. */
    private static final class StubPlaybackController implements PlaybackController {
        private boolean deterministic;
        private double speed = 1.0;

        @Override
        public PlaybackController setDeterministic(boolean enabled) {
            this.deterministic = enabled;
            return this;
        }

        @Override
        public boolean isDeterministic() {
            return deterministic;
        }

        @Override
        public PlaybackController setSpeed(double speed) {
            this.speed = speed;
            return this;
        }

        @Override
        public double getSpeed() {
            return speed;
        }

        @Override
        public void reset(long firstTimestampNanos) {
        }

        @Override
        public void stepTo(long tickTimestampNanos) {
        }

        @Override
        public void advanceByNanos(long deltaNanos) {
        }

        @Override
        public boolean isPlaybackActive() {
            return false;
        }

        @Override
        public long getLastTickNanos() {
            return 0L;
        }

        @Override
        public long getResolvedNowNanos(long systemNowNanos) {
            return systemNowNanos;
        }

        @Override
        public long getPlaybackElapsedNanos() {
            return 0L;
        }

        @Override
        public void appendStatus(StringBuilder target) {
            target.append("stub");
        }
    }
}
