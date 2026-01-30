package com.arbergashi.charts.visualverifier;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.DefaultFlowChartModel;
import com.arbergashi.charts.model.DefaultMatrixChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.financial.Candlestick3DRenderer;
import com.arbergashi.charts.render.financial.VolumeRenderer;
import com.arbergashi.charts.render.standard.AreaRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.render.medical.ECGRenderer;
import com.arbergashi.charts.render.medical.MedicalSweepRenderer;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.render.analysis.VectorFieldRenderer;
import com.arbergashi.charts.render.security.VoxelCloudRenderer;
import com.arbergashi.charts.render.circular.PieRenderer;
import com.arbergashi.charts.render.circular.DonutRenderer;
import com.arbergashi.charts.render.circular.SemiDonutRenderer;
import com.arbergashi.charts.render.circular.NightingaleRoseRenderer;
import com.arbergashi.charts.render.circular.RadarRenderer;
import com.arbergashi.charts.render.circular.PolarRenderer;
import com.arbergashi.charts.render.circular.PolarLineRenderer;
import com.arbergashi.charts.render.circular.GaugeRenderer;
import com.arbergashi.charts.render.circular.GaugeBandsRenderer;
import com.arbergashi.charts.render.circular.ChordDiagramRenderer;
import com.arbergashi.charts.render.specialized.SankeyRenderer;
import com.arbergashi.charts.render.AbstractSpatialLayer;
import com.arbergashi.charts.engine.spatial.Matrix4x4;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class VerificationController {
    private final ServerRenderService renderService;
    private static final boolean VECTOR_AVAILABLE = isVectorAvailable();

    public VerificationController(ServerRenderService renderService) {
        this.renderService = renderService;
    }

    @GetMapping("/")
    public String index(Model model) {
        populateModel(model);
        return "index";
    }

    @GetMapping("/core")
    public String core(Model model) {
        populateModel(model);
        return "core";
    }

    @GetMapping("/domains")
    public String domains(Model model) {
        populateModel(model);
        model.addAttribute("dashboardReadout", financialDashboardReadout(8000));
        return "domains";
    }

    @GetMapping("/families")
    public String families(Model model) {
        populateModel(model);
        model.addAttribute("families", familyGroups());
        return "families";
    }

    @GetMapping("/renderers")
    public String renderers(Model model) {
        populateModel(model);
        return "renderers";
    }

    @GetMapping("/performance")
    public String performance(Model model) {
        populateModel(model);
        return "performance";
    }

    @GetMapping(value = "/api/chart/{type}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> chart(@PathVariable String type,
                                        @RequestParam(defaultValue = "10000") int points,
                                        @RequestParam(defaultValue = "800") int width,
                                        @RequestParam(defaultValue = "500") int height,
                                        @RequestParam(defaultValue = "dark") String theme) {
        return chart(type, points, width, height, theme, null, 25, 35, 0);
    }

    @GetMapping(value = "/api/chart/{type}", produces = MediaType.IMAGE_PNG_VALUE, params = "domain")
    @ResponseBody
    public ResponseEntity<byte[]> chart(@PathVariable String type,
                                        @RequestParam(defaultValue = "10000") int points,
                                        @RequestParam(defaultValue = "800") int width,
                                        @RequestParam(defaultValue = "500") int height,
                                        @RequestParam(defaultValue = "dark") String theme,
                                        @RequestParam(required = false) String domain,
                                        @RequestParam(defaultValue = "25") double rx,
                                        @RequestParam(defaultValue = "35") double ry,
                                        @RequestParam(defaultValue = "0") double rz) {
        if (isSpatialType(type) && !VECTOR_AVAILABLE) {
            return ResponseEntity.badRequest().build();
        }
        ChartTheme chartTheme = "light".equalsIgnoreCase(theme) ? ChartThemes.getLightTheme() : ChartThemes.getDarkTheme();
        ChartModel model = buildModel(type, points);
        ChartRenderer renderer = selectRenderer(type);
        if ("medical".equalsIgnoreCase(domain)) {
            renderer = new VerifierRendererWrapper(renderer, true);
        }
        if ("spatial".equalsIgnoreCase(domain)) {
            applySpatialTransform(renderer, rx, ry, rz);
        }
        byte[] png = renderService.renderToPng(model, new Dimension(width, height), chartTheme, renderer);
        return ResponseEntity.ok(png);
    }

    @GetMapping("/api/metrics")
    @ResponseBody
    public Map<String, Object> metrics() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("poolActive", renderService.getPoolActive());
        payload.put("poolIdle", renderService.getPoolIdle());
        payload.put("poolContention", renderService.getPoolContentionCount());
        return payload;
    }

    @GetMapping("/api/stress")
    @ResponseBody
    public Map<String, Object> stress(@RequestParam(defaultValue = "50") int count,
                                      @RequestParam(defaultValue = "10000") int points,
                                      @RequestParam(defaultValue = "800") int width,
                                      @RequestParam(defaultValue = "500") int height,
                                      @RequestParam(defaultValue = "dark") String theme) {
        int iterations = Math.max(1, count);
        ChartTheme chartTheme = "light".equalsIgnoreCase(theme) ? ChartThemes.getLightTheme() : ChartThemes.getDarkTheme();

        long contentionBefore = renderService.getPoolContentionCount();
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String type = (i % 3 == 0) ? "candlestick" : (i % 3 == 1) ? "line" : "area";
            ChartModel model = buildModel(type, points);
            ChartRenderer renderer = selectRenderer(type);
            renderService.renderToImage(model, new Dimension(width, height), chartTheme, renderer);
        }
        long elapsed = System.nanoTime() - start;
        long contentionAfter = renderService.getPoolContentionCount();

        double totalMs = elapsed / 1_000_000.0;
        double avgMs = totalMs / iterations;
        double perSecond = iterations / (totalMs / 1000.0);

        Map<String, Object> payload = new HashMap<>();
        payload.put("count", iterations);
        payload.put("points", points);
        payload.put("totalMs", totalMs);
        payload.put("avgMs", avgMs);
        payload.put("chartsPerSecond", perSecond);
        payload.put("poolActive", renderService.getPoolActive());
        payload.put("poolIdle", renderService.getPoolIdle());
        payload.put("poolContentionDelta", Math.max(0, contentionAfter - contentionBefore));
        return payload;
    }

    @GetMapping("/api/stress-view")
    public String stressView(@RequestParam(defaultValue = "50") int count,
                             @RequestParam(defaultValue = "10000") int points,
                             @RequestParam(defaultValue = "800") int width,
                             @RequestParam(defaultValue = "500") int height,
                             @RequestParam(defaultValue = "dark") String theme,
                             Model model) {
        Map<String, Object> metrics = stress(count, points, width, height, theme);
        model.addAttribute("metrics", metrics);
        return "fragments/stress :: panel";
    }

    private static List<Map<String, Object>> chartGroups() {
        List<Map<String, Object>> groups = new ArrayList<>();

        Map<String, Object> financial = new LinkedHashMap<>();
        financial.put("name", "Financial Suite");
        financial.put("charts", List.of(
                chartEntry("candlestick", "Candlestick (10k)", "financial"),
                chartEntry("volume", "Volume (10k)", "financial"),
                chartEntry("line", "Line (50k)", "financial"),
                chartEntry("area", "Area (50k)", "financial")
        ));
        groups.add(financial);

        Map<String, Object> medical = new LinkedHashMap<>();
        medical.put("name", "Medical & Bio-Tech");
        medical.put("charts", List.of(
                chartEntry("ecg", "ECG Sweep (20k)", "medical"),
                chartEntry("medical", "Medical Sweep (20k)", "medical")
        ));
        groups.add(medical);

        Map<String, Object> engineering = new LinkedHashMap<>();
        engineering.put("name", "Engineering & Analysis");
        engineering.put("charts", List.of(
                chartEntry("heatmap", "Heatmap (Matrix)", "analysis"),
                chartEntry("vector", "Vector Field", "analysis"),
                chartEntry("radar", "Radar Mesh", "analysis")
        ));
        groups.add(engineering);

        Map<String, Object> circular = new LinkedHashMap<>();
        circular.put("name", "Circular Analytics");
        circular.put("charts", List.of(
                chartEntry("pie", "Pie", "circular"),
                chartEntry("donut", "Donut", "circular"),
                chartEntry("nightingale", "Nightingale Rose", "circular"),
                chartEntry("polar", "Polar Area", "circular"),
                chartEntry("gauge", "Gauge", "circular")
        ));
        groups.add(circular);

        Map<String, Object> network = new LinkedHashMap<>();
        network.put("name", "Network & Flow");
        network.put("charts", List.of(
                chartEntry("chord", "Chord Diagram", "network"),
                chartEntry("sankey", "Sankey Flow", "network")
        ));
        groups.add(network);

        if (VECTOR_AVAILABLE) {
            Map<String, Object> spatial = new LinkedHashMap<>();
            spatial.put("name", "3D & Spatial Analysis");
            spatial.put("charts", List.of(
                    chartEntry("candlestick3d", "Candlestick 3D", "spatial"),
                    chartEntry("voxel", "Voxel Cloud", "spatial")
            ));
            groups.add(spatial);
        }

        return groups;
    }

    private static void populateModel(Model model) {
        model.addAttribute("groups", chartGroups());
        model.addAttribute("vectorAvailable", VECTOR_AVAILABLE);
    }

    private static Map<String, String> chartEntry(String type, String label, String domain) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("type", type);
        entry.put("label", label);
        entry.put("domain", domain);
        return entry;
    }

    private static ChartModel buildModel(String type, int points) {
        if ("candlestick".equalsIgnoreCase(type)) {
            return generateFinancialCandles(points);
        }
        if ("volume".equalsIgnoreCase(type)) {
            return generateFinancialDashboard(points);
        }
        if ("financial-dashboard".equalsIgnoreCase(type)) {
            return generateFinancialDashboard(points);
        }
        if ("pie".equalsIgnoreCase(type) || "donut".equalsIgnoreCase(type) || "semi-donut".equalsIgnoreCase(type)) {
            return generatePieModel();
        }
        if ("nightingale".equalsIgnoreCase(type)) {
            return generateNightingaleModel();
        }
        if ("radar".equalsIgnoreCase(type)) {
            return generateRadarModel();
        }
        if ("polar".equalsIgnoreCase(type) || "polar-line".equalsIgnoreCase(type)) {
            return generatePolarModel();
        }
        if ("gauge".equalsIgnoreCase(type) || "gauge-bands".equalsIgnoreCase(type)) {
            return generateGaugeModel();
        }
        if ("chord".equalsIgnoreCase(type)) {
            return generateChordModel();
        }
        if ("sankey".equalsIgnoreCase(type)) {
            return generateSankeyModel();
        }
        if ("ecg".equalsIgnoreCase(type) || "medical".equalsIgnoreCase(type)) {
            return generateMedical(points);
        }
        if ("heatmap".equalsIgnoreCase(type)) {
            return generateHeatmap(points);
        }
        if ("vector".equalsIgnoreCase(type)) {
            return generateVectorField(points);
        }
        if ("candlestick3d".equalsIgnoreCase(type)) {
            return generateFinancialCandles(points);
        }
        if ("voxel".equalsIgnoreCase(type)) {
            return generateVoxel(points);
        }
        return generateLineSeries(points);
    }

    private static ChartRenderer selectRenderer(String type) {
        if ("candlestick".equalsIgnoreCase(type)) {
            return new CandlestickRenderer();
        }
        if ("volume".equalsIgnoreCase(type)) {
            return new VolumeRenderer();
        }
        if ("financial-dashboard".equalsIgnoreCase(type)) {
            return new FinancialDashboardRenderer();
        }
        if ("pie".equalsIgnoreCase(type)) {
            return new PieRenderer();
        }
        if ("donut".equalsIgnoreCase(type)) {
            return new DonutRenderer().setCenterText("Portfolio").setCenterSubText("v1.7.0 LTS");
        }
        if ("semi-donut".equalsIgnoreCase(type)) {
            return new SemiDonutRenderer().setValue(0.72);
        }
        if ("nightingale".equalsIgnoreCase(type)) {
            return new NightingaleRoseRenderer();
        }
        if ("radar".equalsIgnoreCase(type)) {
            return new RadarRenderer();
        }
        if ("polar".equalsIgnoreCase(type)) {
            return new PolarRenderer();
        }
        if ("polar-line".equalsIgnoreCase(type)) {
            return new PolarLineRenderer();
        }
        if ("gauge".equalsIgnoreCase(type)) {
            return new GaugeRenderer().setValue(72);
        }
        if ("gauge-bands".equalsIgnoreCase(type)) {
            return new GaugeBandsRenderer();
        }
        if ("chord".equalsIgnoreCase(type)) {
            return new ChordDiagramRenderer();
        }
        if ("sankey".equalsIgnoreCase(type)) {
            return new SankeyRenderer();
        }
        if ("area".equalsIgnoreCase(type)) {
            return new AreaRenderer();
        }
        if ("ecg".equalsIgnoreCase(type)) {
            return new ECGRenderer();
        }
        if ("medical".equalsIgnoreCase(type)) {
            return new MedicalSweepRenderer();
        }
        if ("heatmap".equalsIgnoreCase(type)) {
            return new HeatmapRenderer();
        }
        if ("vector".equalsIgnoreCase(type)) {
            return new VectorFieldRenderer((x, y, out) -> {
                out[0] = Math.cos(x * 0.04) * 0.8;
                out[1] = Math.sin(y * 0.04) * 0.8;
                return true;
            });
        }
        if ("candlestick3d".equalsIgnoreCase(type)) {
            if (!VECTOR_AVAILABLE) {
                return new LineRenderer();
            }
            return new Candlestick3DRenderer().setSolid(true);
        }
        if ("voxel".equalsIgnoreCase(type)) {
            if (!VECTOR_AVAILABLE) {
                return new LineRenderer();
            }
            return new VoxelCloudRenderer();
        }
        return new LineRenderer();
    }

    private static boolean isSpatialType(String type) {
        return "candlestick3d".equalsIgnoreCase(type) || "voxel".equalsIgnoreCase(type);
    }

    private static boolean isVectorAvailable() {
        try {
            Class.forName("jdk.incubator.vector.DoubleVector");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static DefaultFinancialChartModel generateFinancialCandles(int points) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Verifier");
        double price = 100.0;
        for (int i = 0; i < points; i++) {
            double t = i;
            double wave = Math.sin(i * 0.08) * 2.5 + Math.cos(i * 0.03) * 4.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            model.setOHLC(t, open, high, low, close);
            price = close;
        }
        return model;
    }

    private static DefaultFinancialChartModel generateFinancialDashboard(int points) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Dashboard");
        double price = 120.0;
        for (int i = 0; i < points; i++) {
            double t = i;
            double wave = Math.sin(i * 0.07) * 2.8 + Math.cos(i * 0.02) * 5.5;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.5;
            double low = Math.min(open, close) - 1.5;
            double volume = 800 + 400 * Math.abs(Math.sin(i * 0.06)) + (i % 7) * 45;
            model.setOHLC(t, open, high, low, close, volume, null);
            price = close;
        }
        return model;
    }

    private static DefaultChartModel generatePieModel() {
        DefaultChartModel model = new DefaultChartModel("Pie");
        String[] labels = {"Equities", "Bonds", "FX", "Commodities", "Alt"};
        double[] weights = {42, 18, 15, 12, 13};
        for (int i = 0; i < labels.length; i++) {
            model.setPoint(i, weights[i], 0.0, 0.0, weights[i], labels[i]);
        }
        return model;
    }

    private static DefaultChartModel generateNightingaleModel() {
        DefaultChartModel model = new DefaultChartModel("Nightingale");
        for (int i = 0; i < 8; i++) {
            double value = 20 + (Math.sin(i * 0.8) + 1.5) * 35;
            model.setPoint(i, value, 0.0, 0.0, value, "S" + (i + 1));
        }
        return model;
    }

    private static DefaultChartModel generateRadarModel() {
        DefaultChartModel model = new DefaultChartModel("Radar");
        double[] values = {76, 58, 92, 64, 81, 70};
        for (int i = 0; i < values.length; i++) {
            model.setPoint(i, values[i], 0.0, 0.0, values[i], "R" + (i + 1));
        }
        return model;
    }

    private static DefaultChartModel generatePolarModel() {
        DefaultChartModel model = new DefaultChartModel("Polar");
        int points = 12;
        for (int i = 0; i < points; i++) {
            double angle = i * (360.0 / points);
            double radius = 35 + 25 * Math.abs(Math.sin(i * 0.6));
            double weight = 10 + i;
            model.setPoint(angle, radius, 0.0, 0.0, weight, "P" + (i + 1));
        }
        return model;
    }

    private static DefaultChartModel generateGaugeModel() {
        DefaultChartModel model = new DefaultChartModel("Gauge");
        model.setPoint(0, 72, 0.0, 0.0, 72, "Health");
        return model;
    }

    private static DefaultMatrixChartModel generateChordModel() {
        double[][] matrix = {
                {0, 6, 2, 4},
                {3, 0, 5, 1},
                {4, 2, 0, 7},
                {1, 5, 3, 0}
        };
        List<String> labels = List.of("Risk", "FX", "Rates", "Equity");
        return new DefaultMatrixChartModel(matrix, labels).setName("Chord");
    }

    private static DefaultFlowChartModel generateSankeyModel() {
        List<DefaultFlowChartModel.DefaultNode> nodes = List.of(
                new DefaultFlowChartModel.DefaultNode("a", "Ingress"),
                new DefaultFlowChartModel.DefaultNode("b", "Pricing"),
                new DefaultFlowChartModel.DefaultNode("c", "Risk"),
                new DefaultFlowChartModel.DefaultNode("d", "Execution")
        );
        List<DefaultFlowChartModel.DefaultLink> links = List.of(
                new DefaultFlowChartModel.DefaultLink("a", "b", 24),
                new DefaultFlowChartModel.DefaultLink("a", "c", 16),
                new DefaultFlowChartModel.DefaultLink("b", "d", 20),
                new DefaultFlowChartModel.DefaultLink("c", "d", 12)
        );
        return new DefaultFlowChartModel(nodes, links).setName("Sankey");
    }

    private static List<Map<String, Object>> familyGroups() {
        List<Map<String, Object>> families = new ArrayList<>();

        families.add(familyGroup(
                "financial",
                "Financial systems",
                "Candles, volume, and forensic price structure for trading desks and monitoring.",
                "5 renderers",
                List.of(
                        legendItem("tone-1", "OHLC + volume with forensic detail"),
                        legendItem("tone-2", "Dashboard-friendly composition"),
                        legendItem("tone-3", "High-precision axis rendering")
                ),
                List.of(
                        kpiItem("Latency", "sub‑ms"),
                        kpiItem("Precision", "tick‑accurate"),
                        kpiItem("Depth", "10k+ candles")
                ),
                List.of(
                        featuredEntry("financial-dashboard", "Financial dashboard", "financial", 1040, 620,
                                List.of("Candles + volume split panes", "High-precision crosshair overlay", "Grid and axis tuned for trading desks")),
                        featuredEntry("candlestick", "Candlestick focus", "financial", 1040, 620,
                                List.of("Adaptive candle width", "Bull/bear color governance", "Snapshot-ready rendering"))
                ),
                List.of(
                        chartEntry("volume", "Volume", "financial"),
                        chartEntry("line", "Trend line", "financial"),
                        chartEntry("area", "Price area", "financial")
                )
        ));

        families.add(familyGroup(
                "circular",
                "Circular analytics",
                "Portfolio composition, radial KPIs, and directional insight.",
                "9 renderers",
                List.of(
                        legendItem("tone-1", "Pie/Donut composition views"),
                        legendItem("tone-2", "Polar & Nightingale analysis"),
                        legendItem("tone-3", "KPI gauges with bands")
                ),
                List.of(
                        kpiItem("Layout", "radial"),
                        kpiItem("KPI", "gauge‑ready"),
                        kpiItem("Composition", "portfolio")
                ),
                List.of(
                        featuredEntry("donut", "Donut composition", "circular", 980, 580,
                                List.of("Center labels for KPIs", "Hover‑ready segments", "Clean external labeling")),
                        featuredEntry("nightingale", "Nightingale rose", "circular", 980, 580,
                                List.of("Radial magnitude comparison", "Polar grid reference", "Directional analytics"))
                ),
                List.of(
                        chartEntry("pie", "Pie", "circular"),
                        chartEntry("semi-donut", "Semi donut", "circular"),
                        chartEntry("polar", "Polar area", "circular"),
                        chartEntry("polar-line", "Polar line", "circular"),
                        chartEntry("gauge", "Gauge", "circular"),
                        chartEntry("gauge-bands", "Gauge bands", "circular")
                )
        ));

        families.add(familyGroup(
                "medical",
                "Medical diagnostics",
                "High-precision waveforms that remain stable under continuous streaming.",
                "6 renderers",
                List.of(
                        legendItem("tone-1", "Sweep-erase ECG stability"),
                        legendItem("tone-4", "Zero-GC for continuous monitoring"),
                        legendItem("tone-2", "Calibration-safe plotting")
                ),
                List.of(
                        kpiItem("FPS", "60+"),
                        kpiItem("Latency", "real‑time"),
                        kpiItem("Scaling", "medical‑grade")
                ),
                List.of(
                        featuredEntry("ecg", "ECG sweep", "medical", 980, 580,
                                List.of("Sweep‑erase persistence", "Lead clarity at scale", "Stable signal grid")),
                        featuredEntry("medical", "Medical sweep", "medical", 980, 580,
                                List.of("Circular buffer performance", "HiDPI crisp rendering", "Zero‑GC redraws"))
                ),
                List.of(
                        chartEntry("line", "Telemetry line", "medical"),
                        chartEntry("area", "Vitals area", "medical")
                )
        ));

        families.add(familyGroup(
                "engineering",
                "Engineering & analytics",
                "Field analysis, vector overlays, and data density visualization.",
                "8 renderers",
                List.of(
                        legendItem("tone-3", "Density & field visualization"),
                        legendItem("tone-1", "Vector overlays"),
                        legendItem("tone-2", "Analytical radar mesh")
                ),
                List.of(
                        kpiItem("Field", "vector‑ready"),
                        kpiItem("Density", "matrix"),
                        kpiItem("Analysis", "radar")
                ),
                List.of(
                        featuredEntry("heatmap", "Heatmap matrix", "analysis", 980, 580,
                                List.of("Dense data visualization", "Stable color encoding", "Matrix clarity")),
                        featuredEntry("vector", "Vector field", "analysis", 980, 580,
                                List.of("Directional overlays", "Sparse vector grids", "Readable flow"))
                ),
                List.of(
                        chartEntry("radar", "Radar mesh", "analysis")
                )
        ));

        families.add(familyGroup(
                "network",
                "Network & flow",
                "Dependency visualization and capital flow storytelling.",
                "4 renderers",
                List.of(
                        legendItem("tone-5", "Chord relationships"),
                        legendItem("tone-1", "Sankey flow ribbons"),
                        legendItem("tone-2", "Network topology clarity")
                ),
                List.of(
                        kpiItem("Topology", "network"),
                        kpiItem("Flow", "capital"),
                        kpiItem("Clarity", "high")
                ),
                List.of(
                        featuredEntry("chord", "Chord diagram", "network", 980, 580,
                                List.of("Relationship density", "Ring‑based grouping", "Highlight‑ready")),
                        featuredEntry("sankey", "Sankey flow", "network", 980, 580,
                                List.of("Flow storytelling", "Ribbons with weight", "End‑to‑end clarity"))
                ),
                List.of()
        ));

        if (VECTOR_AVAILABLE) {
            families.add(familyGroup(
                    "spatial",
                    "Spatial 3D",
                    "Vector-accelerated spatial pipelines for 3D market and sensor data.",
                    "6 renderers",
                    List.of(
                            legendItem("tone-1", "3D market spatialization"),
                            legendItem("tone-4", "Voxel density clouds"),
                            legendItem("tone-3", "Vectorized math pipeline")
                    ),
                    List.of(
                            kpiItem("Depth", "3D"),
                            kpiItem("Vector", "JDK"),
                            kpiItem("Model", "spatial")
                    ),
                    List.of(
                            featuredEntry("candlestick3d", "Candlestick 3D", "spatial", 980, 580,
                                    List.of("Depth‑aware candles", "Spatial transforms", "Vector‑accelerated math")),
                            featuredEntry("voxel", "Voxel cloud", "spatial", 980, 580,
                                    List.of("Density clouds", "3D projection", "High‑throughput"))
                    ),
                    List.of()
            ));
        }

        return families;
    }

    private static Map<String, Object> familyGroup(String key,
                                                   String name,
                                                   String description,
                                                   String kpi,
                                                   List<Map<String, String>> legend,
                                                   List<Map<String, String>> kpis,
                                                   List<Map<String, Object>> featured,
                                                   List<Map<String, String>> charts) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("key", key);
        group.put("name", name);
        group.put("description", description);
        group.put("kpi", kpi);
        group.put("legend", legend);
        group.put("kpis", kpis);
        group.put("featured", featured);
        group.put("charts", charts);
        return group;
    }

    private static Map<String, String> legendItem(String tone, String label) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("tone", tone);
        item.put("label", label);
        return item;
    }

    private static Map<String, String> kpiItem(String label, String value) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        return item;
    }

    private static Map<String, Object> featuredEntry(String type, String label, String domain, int width, int height, List<String> callouts) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", type);
        entry.put("label", label);
        entry.put("domain", domain);
        entry.put("width", String.valueOf(width));
        entry.put("height", String.valueOf(height));
        entry.put("callouts", callouts);
        return entry;
    }

    private static Map<String, String> financialDashboardReadout(int points) {
        int target = Math.max(0, Math.min(points - 1, (int) (points * 0.65)));
        double price = 120.0;
        double open = price;
        double close = price;
        double high = price;
        double low = price;
        double volume = 0.0;
        double time = 0.0;

        for (int i = 0; i <= target; i++) {
            time = i;
            double wave = Math.sin(i * 0.07) * 2.8 + Math.cos(i * 0.02) * 5.5;
            open = price;
            close = price + wave;
            high = Math.max(open, close) + 1.5;
            low = Math.min(open, close) - 1.5;
            volume = 800 + 400 * Math.abs(Math.sin(i * 0.06)) + (i % 7) * 45;
            price = close;
        }

        Map<String, String> readout = new LinkedHashMap<>();
        readout.put("time", String.format(java.util.Locale.US, "%.0f", time));
        readout.put("open", String.format(java.util.Locale.US, "%.2f", open));
        readout.put("high", String.format(java.util.Locale.US, "%.2f", high));
        readout.put("low", String.format(java.util.Locale.US, "%.2f", low));
        readout.put("close", String.format(java.util.Locale.US, "%.2f", close));
        readout.put("volume", String.format(java.util.Locale.US, "%.0f", volume));
        return readout;
    }

    private static DefaultChartModel generateLineSeries(int points) {
        DefaultChartModel model = new DefaultChartModel("Series");
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.05) * 50.0 + Math.cos(i * 0.03) * 20.0;
            model.setXY(x, y);
        }
        return model;
    }

    private static CircularFastMedicalModel generateMedical(int points) {
        int cap = Math.max(1024, points);
        CircularFastMedicalModel model = new CircularFastMedicalModel(cap, 2);
        double t = 0.0;
        double dt = 1.0;
        for (int i = 0; i < points; i++) {
            double y = Math.sin(t * 0.05) * 0.8 + Math.sin(t * 0.12) * 0.2;
            model.add(t, new double[]{t, y});
            t += dt;
        }
        return model;
    }

    private static DefaultChartModel generateHeatmap(int points) {
        int side = Math.max(32, (int) Math.sqrt(points));
        DefaultChartModel model = new DefaultChartModel("Heatmap");
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                double x = c;
                double y = r;
                model.setXY(x, y);
            }
        }
        return model;
    }

    private static DefaultChartModel generateVectorField(int points) {
        DefaultChartModel model = new DefaultChartModel("Vector");
        int side = Math.max(12, (int) Math.sqrt(points));
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                model.setXY(c, r);
            }
        }
        return model;
    }

    private static DefaultChartModel generateVoxel(int points) {
        DefaultChartModel model = new DefaultChartModel("VoxelCloud");
        int count = Math.max(1000, points);
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            double x = Math.cos(t * Math.PI * 4) * 0.6 + noise(i, 0.12);
            double y = Math.sin(t * Math.PI * 3) * 0.6 + noise(i + 31, 0.12);
            double z = Math.sin(t * Math.PI * 2) * 0.6 + noise(i + 63, 0.12);
            double intensity = 0.5 + 0.5 * Math.sin(t * Math.PI * 6);
            model.setPoint(x, y, 0.0, intensity, z, null);
        }
        return model;
    }

    private static double noise(int seed, double scale) {
        double v = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return (v - Math.floor(v) - 0.5) * scale;
    }

    private static void applySpatialTransform(ChartRenderer renderer, double rx, double ry, double rz) {
        if (!(renderer instanceof AbstractSpatialLayer spatial)) {
            return;
        }
        Matrix4x4 rotX = new Matrix4x4().setIdentity().setRotationX(Math.toRadians(rx));
        Matrix4x4 rotY = new Matrix4x4().setIdentity().setRotationY(Math.toRadians(ry));
        Matrix4x4 rotZ = new Matrix4x4().setIdentity().setRotationZ(Math.toRadians(rz));
        Matrix4x4 combined = new Matrix4x4().setIdentity().setProduct(rotZ).setProduct(rotY).setProduct(rotX);
        spatial.setSpatialTransform(combined);
    }
}
