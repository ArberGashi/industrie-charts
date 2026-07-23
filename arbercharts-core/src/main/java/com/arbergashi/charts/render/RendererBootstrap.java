package com.arbergashi.charts.render;

/**
 * Deterministic registration of all self-registering renderers.
 *
 * <p>Renderers register themselves in the {@code RendererRegistry} from their
 * static initializers, which only run once the class is loaded. Without an
 * explicit bootstrap, registry content depends on which renderer classes
 * happen to have been touched first. Calling {@link #ensureRegistered()} loads
 * every self-registering renderer in a fixed order so registry lookups behave
 * identically in desktop, server, and test environments.</p>
 *
 * <p>Idempotent and cheap after the first call.</p>
 *
 * @since 2.1.0
 * @author Arber Gashi
 */
public final class RendererBootstrap {
    private static final String[] SELF_REGISTERING = {
            "com.arbergashi.charts.render.financial.CandlestickHollowRenderer",
            "com.arbergashi.charts.render.financial.LiquidityHeatmapRenderer",
            "com.arbergashi.charts.render.financial.PointAndFigureAdvancedRenderer",
            "com.arbergashi.charts.render.financial.RenkoRenderer",
            "com.arbergashi.charts.render.specialized.ArcDiagramRenderer",
            "com.arbergashi.charts.render.specialized.BulletChartRenderer",
            "com.arbergashi.charts.render.specialized.ChernoffFacesRenderer",
            "com.arbergashi.charts.render.specialized.ChordFlowRenderer",
            "com.arbergashi.charts.render.specialized.ControlChartRenderer",
            "com.arbergashi.charts.render.specialized.DelaunayRenderer",
            "com.arbergashi.charts.render.specialized.DendrogramRenderer",
            "com.arbergashi.charts.render.specialized.GanttResourceViewRenderer",
            "com.arbergashi.charts.render.specialized.HeatmapContourRenderer",
            "com.arbergashi.charts.render.specialized.HeatmapRenderer",
            "com.arbergashi.charts.render.specialized.HexbinRenderer",
            "com.arbergashi.charts.render.specialized.HorizonRenderer",
            "com.arbergashi.charts.render.specialized.JoyplotRenderer",
            "com.arbergashi.charts.render.specialized.ParetoRenderer",
            "com.arbergashi.charts.render.specialized.SankeyProRenderer",
            "com.arbergashi.charts.render.specialized.StreamgraphRenderer",
            "com.arbergashi.charts.render.specialized.SunburstRenderer",
            "com.arbergashi.charts.render.specialized.TernaryContourRenderer",
            "com.arbergashi.charts.render.specialized.VoronoiRenderer",
            "com.arbergashi.charts.render.specialized.WindRoseRenderer",
    };

    private static volatile boolean bootstrapped;

    private RendererBootstrap() {
    }

    /**
     * Loads all self-registering renderer classes so the registry is complete.
     *
     * @throws IllegalStateException if a renderer class cannot be loaded
     */
    public static void ensureRegistered() {
        if (bootstrapped) {
            return;
        }
        synchronized (RendererBootstrap.class) {
            if (bootstrapped) {
                return;
            }
            for (String className : SELF_REGISTERING) {
                try {
                    Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Renderer class missing during bootstrap: " + className, e);
                }
            }
            bootstrapped = true;
        }
    }
}
