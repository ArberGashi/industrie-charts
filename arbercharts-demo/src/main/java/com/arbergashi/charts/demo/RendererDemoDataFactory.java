package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.model.DefaultFlowChartModel;
import com.arbergashi.charts.model.DefaultHierarchicalChartModel;
import com.arbergashi.charts.model.DefaultMatrixChartModel;
import com.arbergashi.charts.model.DefaultMultiDimensionalChartModel;
import com.arbergashi.charts.model.DefaultStatisticalChartModel;
import com.arbergashi.charts.model.DefaultTernaryChartModel;
import com.arbergashi.charts.model.TernaryChartModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Factory for generating demo data for each renderer category.
 *
 * <p>Creates realistic sample data sets appropriate for each renderer type,
 * including financial OHLC data, medical waveforms, statistical distributions,
 * and specialized chart data.
 *
 * <h2>Supported Categories</h2>
 * <ul>
 *   <li><strong>standard</strong> - Line, bar, scatter plots with sine waves</li>
 *   <li><strong>financial</strong> - OHLC candlestick data with realistic price movement</li>
 *   <li><strong>medical</strong> - Multi-channel ECG/EEG waveforms</li>
 *   <li><strong>statistical</strong> - Box plots, distributions</li>
 *   <li><strong>circular</strong> - Radar, pie, polar data</li>
 *   <li><strong>flow</strong> - Sankey, network graph connections</li>
 *   <li><strong>ternary</strong> - Three-component composition data</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ChartModel model = RendererDemoDataFactory.build("financial",
 *     "com.arbergashi.charts.render.financial.CandlestickRenderer");
 * }</pre>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
public final class RendererDemoDataFactory {
    private static final ThreadLocal<Integer> RENDERER_SEED = ThreadLocal.withInitial(() -> 0);
    private static final Set<String> FLOW_RENDERERS = Set.of(
            "AlluvialRenderer",
            "NetworkRenderer",
            "SankeyRenderer"
    );
    private static final Set<String> MATRIX_RENDERERS = Set.of(
            "ChordDiagramRenderer"
    );
    private static final Set<String> TERNARY_RENDERERS = Set.of(
            "TernaryPlotRenderer",
            "TernaryContourRenderer",
            "TernaryPhasediagramRenderer"
    );
    private static final Set<String> MULTI_RENDERERS = Set.of(
            "ParallelCoordinatesRenderer",
            "StreamgraphRenderer"
    );
    private static final Set<String> PATH_SUNBURST_RENDERERS = Set.of(
            "com.arbergashi.charts.render.specialized.SunburstRenderer",
            "com.arbergashi.charts.render.circular.SunburstRenderer"
    );
    private static final Set<String> LINK_LABEL_RENDERERS = Set.of(
            "ChordFlowRenderer",
            "DependencyWheelRenderer",
            "SankeyProRenderer"
    );
    private static final Set<String> AUDIT_RENDERERS = Set.of(
            "AuditTrailRenderer"
    );
    private static final Set<String> ANOMALY_RENDERERS = Set.of(
            "ChangePointRenderer",
            "OutlierDetectionRenderer"
    );
    private static final Set<String> SLOPE_RENDERERS = Set.of(
            "SlopeRenderer"
    );
    private static final Set<String> DISTRIBUTION_RENDERERS = Set.of(
            "ECDFRenderer",
            "KDERenderer",
            "RugPlotRenderer"
    );
    private static final Set<String> SMITH_RENDERERS = Set.of(
            "SmithChartRenderer",
            "VSWRCircleRenderer"
    );
    private static final Set<String> CORRELATION_RENDERERS = Set.of(
            "MovingCorrelationRenderer"
    );

    private RendererDemoDataFactory() {
    }

    static ChartModel build(String category, String className) {
        RENDERER_SEED.set(stableSeed(className));
        String simple = simpleName(className);
        if (FLOW_RENDERERS.contains(simple)) {
            return flowModel();
        }
        if (MATRIX_RENDERERS.contains(simple)) {
            return matrixModel();
        }
        if (TERNARY_RENDERERS.contains(simple)) {
            return ternaryModel();
        }
        if (MULTI_RENDERERS.contains(simple)) {
            return multiDimensionalModel();
        }
        if (PATH_SUNBURST_RENDERERS.contains(className)) {
            return sunburstPathModel();
        }
        if (LINK_LABEL_RENDERERS.contains(simple)) {
            return linkLabelModel();
        }
        if (AUDIT_RENDERERS.contains(simple)) {
            return auditTrailModel();
        }
        if (ANOMALY_RENDERERS.contains(simple)) {
            return anomalyModel();
        }
        if (SLOPE_RENDERERS.contains(simple)) {
            return slopeModel();
        }
        if (DISTRIBUTION_RENDERERS.contains(simple)) {
            return distributionModel();
        }
        if (SMITH_RENDERERS.contains(simple)) {
            return smithModel();
        }
        if (CORRELATION_RENDERERS.contains(simple)) {
            return correlationModel();
        }
        return switch (category) {
            case "financial" -> financialModel();
            case "statistical" -> statisticalModel();
            case "medical" -> medicalModel();
            case "circular" -> circularModel();
            case "predictive" -> anomalyModel();
            default -> standardModel();
        };
    }

    static List<ChartModel> standardSeries(int count) {
        int seriesCount = Math.max(1, count);
        List<ChartModel> models = new ArrayList<>(seriesCount);
        for (int i = 0; i < seriesCount; i++) {
            models.add(standardModel("Standard " + (char) ('A' + i), i, seriesCount));
        }
        return models;
    }

    private static DefaultChartModel standardModel() {
        return standardModel("Standard", 0, 1);
    }

    private static DefaultChartModel standardModel(String name, int index, int total) {
        DefaultChartModel model = new DefaultChartModel(name);
        int seed = RENDERER_SEED.get();
        int points = 280;
        double offset = (index - (total - 1) * 0.5) * 15.0;
        double phase = index * Math.PI / 3 + seededRange(seed, 0.0, Math.PI / 8, 1);
        double amplitude = 30 + seededRange(seed, 0.0, 10.0, 2) - index * 5.5;

        for (int i = 0; i < points; i++) {
            double x = i;
            double trendStrength = 0.08 + seededRange(seed, 0.0, 0.03, 3);
            double trend = i < 110 ? i * trendStrength
                    : (i < 210 ? 110 * trendStrength - (i - 110) * (0.05 + seededRange(seed, 0.0, 0.02, 4))
                    : 110 * trendStrength - 100 * (0.05 + seededRange(seed, 0.0, 0.02, 4)) + (i - 210) * (0.06 + seededRange(seed, 0.0, 0.03, 5)));
            double y = Math.sin(i * 0.05 + phase) * amplitude
                    + Math.sin(i * 0.018 + phase) * (amplitude * 0.42)
                    + trend
                    + offset;
            int eventA = 72 + seededInt(seed, 0, 24, 6);
            int eventB = 176 + seededInt(seed, 0, 28, 7);
            int eventC = 136 + seededInt(seed, 0, 22, 8);
            if (i == eventA || i == eventB) {
                y += 14.0;
            }
            if (i == eventC) {
                y -= 11.0;
            }

            double bandWidth = 4.0 + Math.abs(Math.sin(i * 0.08 + phase)) * 3.2;
            double min = y - bandWidth;
            double max = y + bandWidth;
            double weight = Math.abs(y - offset) * 0.6 + 8.0;

            String label = (i == eventA) ? "Event A" : (i == eventC) ? "Correction" : (i == eventB) ? "Event B" : null;
            model.setPoint(x, y, min, max, weight, label);
        }
        return model;
    }

    private static DefaultChartModel circularModel() {
        DefaultChartModel model = new DefaultChartModel("Asset Allocation");
        int seed = RENDERER_SEED.get();
        String[] labels = {"US Equities", "Int'l Equities", "Fixed Income", "Real Estate", "Commodities", "Cash"};
        double[] weights = {
                30 + seededRange(seed, 0, 8, 11),
                17 + seededRange(seed, 0, 6, 12),
                20 + seededRange(seed, 0, 8, 13),
                8 + seededRange(seed, 0, 5, 14),
                4 + seededRange(seed, 0, 4, 15),
                3 + seededRange(seed, 0, 3, 16)
        };
        for (int i = 0; i < labels.length; i++) {
            double w = weights[i];
            model.setPoint(i, w, 0.0, w, w, labels[i]);
        }
        return model;
    }

    private static DefaultChartModel linkLabelModel() {
        DefaultChartModel model = new DefaultChartModel("Dependency Links");
        int seed = RENDERER_SEED.get();
        String[] pairs = {"Ingest:Validate", "Validate:Risk", "Risk:Route", "Route:Execute", "Execute:Settle", "Settle:Archive"};
        double[] weights = {24, 21, 17, 16, 13, 9};
        for (int i = 0; i < pairs.length; i++) {
            double w = weights[i % weights.length] + seededRange(seed, -3.0, 3.0, 20 + i);
            model.setPoint(i, w, 0.0, w, w, pairs[i]);
        }
        return model;
    }

    private static DefaultChartModel auditTrailModel() {
        DefaultChartModel model = new DefaultChartModel("Audit");
        int points = 180;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.08) * 20 + Math.cos(i * 0.05) * 12;
            double min = y - 6;
            double max = y + 6;
            double weight = Math.abs(y) + 6;
            byte flag = (i % 27 == 0) ? com.arbergashi.charts.model.ProvenanceFlags.MANUAL
                    : (i % 19 == 0) ? com.arbergashi.charts.model.ProvenanceFlags.SYNTHETIC
                    : (i % 13 == 0) ? com.arbergashi.charts.model.ProvenanceFlags.SMOOTHED
                    : com.arbergashi.charts.model.ProvenanceFlags.ORIGINAL;
            model.setPoint(x, y, min, max, weight, "A" + i, flag, (short) (i % 5), System.nanoTime());
        }
        return model;
    }

    private static DefaultChartModel anomalyModel() {
        DefaultChartModel model = new DefaultChartModel("Anomaly");
        int points = 200;
        for (int i = 0; i < points; i++) {
            double base = (i < 100) ? Math.sin(i * 0.06) * 18 : Math.sin(i * 0.06) * 18 + 30;
            if (i == 60 || i == 140) {
                base += 45;
            }
            double x = i;
            double y = base + Math.cos(i * 0.12) * 6;
            double min = y - 6;
            double max = y + 6;
            double weight = Math.abs(y) + 8;
            model.setPoint(x, y, min, max, weight, null);
        }
        return model;
    }

    private static DefaultChartModel slopeModel() {
        DefaultChartModel model = new DefaultChartModel("Slope");
        model.setPoint(0, 12, 10, 14, 1.0, "A");
        model.setPoint(1, 26, 24, 28, 1.0, "B");
        return model;
    }

    private static DefaultChartModel distributionModel() {
        DefaultChartModel model = new DefaultChartModel("Distribution");
        int seed = RENDERER_SEED.get();
        int points = 220;
        for (int i = 0; i < points; i++) {
            double clusterA = Math.sin(i * (0.10 + seededRange(seed, 0.0, 0.03, 30))) * 8.0 + 16.0;
            double clusterB = Math.cos(i * (0.12 + seededRange(seed, 0.0, 0.03, 31))) * 7.0 - 12.0;
            double mixing = (i % 5 < 3) ? clusterA : clusterB;
            double sample = mixing + Math.sin(i * 0.37) * 2.4 + Math.cos(i * 0.09) * 1.8;
            double x = sample;
            double y = sample;
            double min = sample - 2.2;
            double max = sample + 2.2;
            double weight = Math.abs(sample) * 0.45 + 4.0;
            model.setPoint(x, y, min, max, weight, null);
        }
        return model;
    }

    private static DefaultChartModel smithModel() {
        DefaultChartModel model = new DefaultChartModel("Smith");
        int seed = RENDERER_SEED.get();
        int points = 96;
        for (int i = 0; i < points; i++) {
            double t = i * (Math.PI * 2.0 / points);
            double radius = 0.65 + 0.25 * Math.sin(i * (0.15 + seededRange(seed, 0.0, 0.04, 40)));
            double x = Math.cos(t) * radius;
            double y = Math.sin(t) * radius;
            model.setPoint(x, y, y - 0.02, y + 0.02, radius, null);
        }
        return model;
    }

    private static DefaultChartModel correlationModel() {
        DefaultChartModel model = new DefaultChartModel("Moving Correlation");
        int seed = RENDERER_SEED.get();
        int points = 240;
        for (int i = 0; i < points; i++) {
            double x = i;
            int cutA = 96 + seededInt(seed, 0, 28, 50);
            int cutB = 160 + seededInt(seed, 0, 28, 51);
            double windowShift = (i < cutA) ? 14.0 : (i < cutB ? -8.0 : 9.0);
            double y = Math.sin(i * (0.045 + seededRange(seed, 0.0, 0.015, 52))) * 21
                    + Math.cos(i * (0.016 + seededRange(seed, 0.0, 0.01, 53))) * 11 + windowShift;
            double weight = y * 0.48 + Math.cos(i * 0.11) * 5;
            double min = y - 6;
            double max = y + 6;
            model.setPoint(x, y, min, max, weight, null);
        }
        return model;
    }

    private static DefaultFinancialChartModel financialModel() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("AAPL - Daily");
        int seed = RENDERER_SEED.get();
        double price = 185.50;
        int points = 160;

        for (int i = 0; i < points; i++) {
            int cutA = 44 + seededInt(seed, 0, 16, 60);
            int cutB = 94 + seededInt(seed, 0, 18, 61);
            int evtA = 24 + seededInt(seed, 0, 18, 62);
            int evtB = 72 + seededInt(seed, 0, 22, 63);
            int evtC = 118 + seededInt(seed, 0, 24, 64);
            double regime = i < cutA ? 0.08 : (i < cutB ? -0.05 : 0.11);
            double cyclical = Math.sin(i * (0.14 + seededRange(seed, 0.0, 0.03, 65))) * 1.45
                    + Math.cos(i * (0.06 + seededRange(seed, 0.0, 0.02, 66))) * 0.95;
            double event = (i == evtA) ? 5.8 : (i == evtB ? -7.2 : (i == evtC ? 6.3 : 0.0));
            double change = regime + cyclical + event;

            double open = price;
            double close = price + change;
            double bodySize = Math.abs(close - open);
            double wickUp = bodySize * (0.45 + Math.abs(Math.sin(i * 0.21)) * 0.55);
            double wickDown = bodySize * (0.4 + Math.abs(Math.cos(i * 0.19)) * 0.6);

            double high = Math.max(open, close) + wickUp;
            double low = Math.min(open, close) - wickDown;
            double baseVolume = 45_000_000;
            double volumeMultiplier = 1.0 + Math.abs(change / Math.max(1.0, price)) * 9;
            double cycleVolume = 0.82 + Math.abs(Math.sin(i * 0.11)) * 0.52;
            double eventVolume = Math.abs(event) * 2_100_000;
            double volume = baseVolume * volumeMultiplier * cycleVolume + eventVolume;

            String label = (i == evtA) ? "Earnings +" : (i == evtB) ? "Guidance -" : (i == evtC) ? "Upgrade" : null;
            model.setOHLC(i, open, high, low, close, volume, label);
            price = close;
        }
        return model;
    }

    private static DefaultStatisticalChartModel statisticalModel() {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel("Quarterly Returns (%)");
        int seed = RENDERER_SEED.get();
        String[] quarters = {"Q1'24", "Q2'24", "Q3'24", "Q4'24", "Q1'25", "Q2'25", "Q3'25", "Q4'25"};

        // Realistic quarterly return distributions
        double[][] data = {
                {4.2, 2.1, 6.8, -1.5, 9.2},   // Q1'24
                {-2.1, -4.5, 0.8, -6.2, 3.1}, // Q2'24
                {5.5, 3.2, 7.9, 1.8, 10.4},   // Q3'24
                {2.8, 0.5, 4.6, -2.1, 6.8},   // Q4'24
                {6.1, 4.0, 8.5, 2.2, 11.0},   // Q1'25
                {1.5, -1.2, 3.8, -3.5, 5.2},  // Q2'25
                {7.2, 5.1, 9.8, 3.0, 12.5},   // Q3'25
                {3.5, 1.2, 5.9, -0.8, 7.8}    // Q4'25
        };

        for (int i = 0; i < quarters.length; i++) {
            double[] d = data[i];
            double shift = seededRange(seed, -0.8, 0.8, 80 + i);
            model.setBoxPlot(i, d[0] + shift, d[1] + shift, d[2] + shift, d[3] + shift, d[4] + shift, quarters[i]);
        }
        return model;
    }

    private static CircularFastMedicalModel medicalModel() {
        int seed = RENDERER_SEED.get();
        int sampleRate = 250; // 250 Hz - standard ECG
        double duration = 6.0; // 6 seconds
        int points = (int) (sampleRate * duration);
        CircularFastMedicalModel model = new CircularFastMedicalModel(points, 3);

        double heartRate = 72.0; // BPM
        double rrInterval = 60.0 / heartRate; // seconds between beats

        for (int i = 0; i < points; i++) {
            double t = i / (double) sampleRate;

            // Lead I - realistic ECG morphology
            double lead1 = generateECGBeat(t, rrInterval, 0.95 + seededRange(seed, 0.0, 0.15, 90));

            // Lead II - similar but slightly different amplitude
            double lead2 = generateECGBeat(t + (0.015 + seededRange(seed, 0.0, 0.01, 91)), rrInterval, 1.1 + seededRange(seed, 0.0, 0.2, 92));

            // Lead III - computed as Lead II - Lead I (Einthoven)
            double lead3 = lead2 - lead1;

            // Deterministic baseline/noise for reproducible demos.
            lead1 += Math.sin(i * 0.31) * 0.008 + Math.cos(i * 0.07) * 0.004;
            lead2 += Math.sin(i * 0.27 + 0.4) * 0.007 + Math.cos(i * 0.09) * 0.004;
            lead3 += Math.sin(i * 0.29 + 0.2) * 0.006 + Math.cos(i * 0.08) * 0.003;

            model.add(t, new double[]{lead1, lead2, lead3});
        }
        return model;
    }

    /**
     * Generates a realistic ECG waveform component at time t.
     */
    private static double generateECGBeat(double t, double rrInterval, double amplitude) {
        double phase = (t % rrInterval) / rrInterval;
        double y = 0.0;

        // P wave (atrial depolarization)
        if (phase > 0.0 && phase < 0.1) {
            double pPhase = (phase - 0.05) / 0.05;
            y += 0.15 * amplitude * Math.exp(-pPhase * pPhase * 8);
        }

        // QRS complex (ventricular depolarization)
        if (phase > 0.12 && phase < 0.22) {
            double qrsPhase = (phase - 0.17) / 0.05;
            // Q wave (small negative)
            y -= 0.1 * amplitude * Math.exp(-Math.pow(qrsPhase + 0.8, 2) * 30);
            // R wave (large positive)
            y += 1.0 * amplitude * Math.exp(-qrsPhase * qrsPhase * 25);
            // S wave (negative)
            y -= 0.2 * amplitude * Math.exp(-Math.pow(qrsPhase - 0.6, 2) * 35);
        }

        // T wave (ventricular repolarization)
        if (phase > 0.25 && phase < 0.45) {
            double tPhase = (phase - 0.35) / 0.1;
            y += 0.3 * amplitude * Math.exp(-tPhase * tPhase * 4);
        }

        return y;
    }

    private static DefaultFlowChartModel flowModel() {
        int seed = RENDERER_SEED.get();
        List<DefaultFlowChartModel.DefaultNode> nodes = List.of(
                new DefaultFlowChartModel.DefaultNode("orders", "Orders"),
                new DefaultFlowChartModel.DefaultNode("validation", "Validation"),
                new DefaultFlowChartModel.DefaultNode("risk", "Risk Check"),
                new DefaultFlowChartModel.DefaultNode("routing", "Smart Routing"),
                new DefaultFlowChartModel.DefaultNode("exchange", "Exchange"),
                new DefaultFlowChartModel.DefaultNode("filled", "Filled"),
                new DefaultFlowChartModel.DefaultNode("rejected", "Rejected")
        );
        List<DefaultFlowChartModel.DefaultLink> links = List.of(
                new DefaultFlowChartModel.DefaultLink("orders", "validation", 94 + seededInt(seed, 0, 12, 100)),
                new DefaultFlowChartModel.DefaultLink("validation", "risk", 88 + seededInt(seed, 0, 10, 101)),
                new DefaultFlowChartModel.DefaultLink("validation", "rejected", 3 + seededInt(seed, 0, 4, 102)),
                new DefaultFlowChartModel.DefaultLink("risk", "routing", 80 + seededInt(seed, 0, 14, 103)),
                new DefaultFlowChartModel.DefaultLink("risk", "rejected", 4 + seededInt(seed, 0, 5, 104)),
                new DefaultFlowChartModel.DefaultLink("routing", "exchange", 79 + seededInt(seed, 0, 14, 105)),
                new DefaultFlowChartModel.DefaultLink("exchange", "filled", 74 + seededInt(seed, 0, 12, 106)),
                new DefaultFlowChartModel.DefaultLink("exchange", "rejected", 4 + seededInt(seed, 0, 5, 107))
        );
        return new DefaultFlowChartModel(nodes, links).setName("Order Flow");
    }

    private static DefaultMatrixChartModel matrixModel() {
        int seed = RENDERER_SEED.get();
        // Correlation matrix between asset classes
        double[][] matrix = {
                {1.00, 0.78 + seededRange(seed, -0.06, 0.06, 110), 0.66 + seededRange(seed, -0.06, 0.06, 111), 0.42 + seededRange(seed, -0.06, 0.06, 112), 0.28 + seededRange(seed, -0.06, 0.06, 113)},
                {0.78 + seededRange(seed, -0.06, 0.06, 114), 1.00, 0.62 + seededRange(seed, -0.06, 0.06, 115), 0.46 + seededRange(seed, -0.06, 0.06, 116), 0.31 + seededRange(seed, -0.06, 0.06, 117)},
                {0.66 + seededRange(seed, -0.06, 0.06, 118), 0.62 + seededRange(seed, -0.06, 0.06, 119), 1.00, 0.36 + seededRange(seed, -0.06, 0.06, 120), 0.21 + seededRange(seed, -0.06, 0.06, 121)},
                {0.42 + seededRange(seed, -0.06, 0.06, 122), 0.46 + seededRange(seed, -0.06, 0.06, 123), 0.36 + seededRange(seed, -0.06, 0.06, 124), 1.00, 0.55 + seededRange(seed, -0.06, 0.06, 125)},
                {0.28 + seededRange(seed, -0.06, 0.06, 126), 0.31 + seededRange(seed, -0.06, 0.06, 127), 0.21 + seededRange(seed, -0.06, 0.06, 128), 0.55 + seededRange(seed, -0.06, 0.06, 129), 1.00}
        };
        List<String> labels = List.of("US Eq", "Int'l Eq", "EM", "Cmdty", "Bonds");
        return new DefaultMatrixChartModel(matrix, labels).setName("Correlation Matrix");
    }

    private static DefaultTernaryChartModel ternaryModel() {
        int seed = RENDERER_SEED.get();
        List<TernaryChartModel.TernaryPoint> points = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double a = 18 + (i % 6) * (2.6 + seededRange(seed, 0.0, 0.6, 140));
            double b = 50 + Math.sin(i * (0.36 + seededRange(seed, 0.0, 0.08, 141))) * 10;
            double c = 100 - a - b;
            if (c < 5) c = 5;
            double scale = 100.0 / (a + b + c);
            points.add(new DefaultTernaryChartModel.DefaultTernaryPoint(a * scale, b * scale, c * scale));
        }
        return new DefaultTernaryChartModel(points, List.of("A", "B", "C")).setName("Ternary");
    }

    private static DefaultMultiDimensionalChartModel multiDimensionalModel() {
        int seed = RENDERER_SEED.get();
        List<double[]> data = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            double base = 48 + Math.sin(i * (0.17 + seededRange(seed, 0.0, 0.05, 150))) * 10;
            data.add(new double[]{
                    base + Math.cos(i * 0.1) * 12,
                    base + Math.sin(i * 0.12) * 9,
                    base + Math.cos(i * 0.18) * 6,
                    base + Math.sin(i * 0.22) * 14,
                    base + Math.cos(i * 0.3) * 8
            });
        }
        return new DefaultMultiDimensionalChartModel(data, List.of("A", "B", "C", "D", "E")).setName("MultiDim");
    }

    private static DefaultHierarchicalChartModel hierarchicalModel() {
        int seed = RENDERER_SEED.get();
        DefaultHierarchicalChartModel.DefaultNode root = new DefaultHierarchicalChartModel.DefaultNode("Global Portfolio", 0);

        // Equities
        DefaultHierarchicalChartModel.DefaultNode equities = new DefaultHierarchicalChartModel.DefaultNode("Equities", 0);
        DefaultHierarchicalChartModel.DefaultNode usEq = new DefaultHierarchicalChartModel.DefaultNode("US", 0);
        usEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Large Cap", 22 + seededInt(seed, 0, 6, 160)));
        usEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Mid Cap", 6 + seededInt(seed, 0, 4, 161)));
        usEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Small Cap", 4 + seededInt(seed, 0, 3, 162)));
        equities.setChild(usEq);
        DefaultHierarchicalChartModel.DefaultNode intlEq = new DefaultHierarchicalChartModel.DefaultNode("International", 0);
        intlEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Europe", 8 + seededInt(seed, 0, 4, 163)));
        intlEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Asia Pacific", 7 + seededInt(seed, 0, 4, 164)));
        intlEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Emerging", 5 + seededInt(seed, 0, 4, 165)));
        equities.setChild(intlEq);

        // Fixed Income
        DefaultHierarchicalChartModel.DefaultNode bonds = new DefaultHierarchicalChartModel.DefaultNode("Fixed Income", 0);
        bonds.setChild(new DefaultHierarchicalChartModel.DefaultNode("Government", 10 + seededInt(seed, 0, 4, 166)));
        bonds.setChild(new DefaultHierarchicalChartModel.DefaultNode("Corporate", 9 + seededInt(seed, 0, 4, 167)));
        bonds.setChild(new DefaultHierarchicalChartModel.DefaultNode("High Yield", 3 + seededInt(seed, 0, 3, 168)));

        // Alternatives
        DefaultHierarchicalChartModel.DefaultNode alts = new DefaultHierarchicalChartModel.DefaultNode("Alternatives", 0);
        alts.setChild(new DefaultHierarchicalChartModel.DefaultNode("Real Estate", 4 + seededInt(seed, 0, 3, 169)));
        alts.setChild(new DefaultHierarchicalChartModel.DefaultNode("Commodities", 3 + seededInt(seed, 0, 2, 170)));
        alts.setChild(new DefaultHierarchicalChartModel.DefaultNode("Cash", 1 + seededInt(seed, 0, 2, 171)));

        root.setChild(equities);
        root.setChild(bonds);
        root.setChild(alts);

        return new DefaultHierarchicalChartModel(root).setName("Portfolio Allocation");
    }

    private static DefaultChartModel sunburstPathModel() {
        DefaultChartModel model = new DefaultChartModel("Sunburst Paths");
        int seed = RENDERER_SEED.get();
        model.setPoint(0, 26 + seededInt(seed, 0, 8, 172), 0, 0, 1, "Portfolio/Equities/US/Large Cap");
        model.setPoint(1, 12 + seededInt(seed, 0, 6, 173), 0, 0, 1, "Portfolio/Equities/US/Mid Cap");
        model.setPoint(2, 8 + seededInt(seed, 0, 5, 174), 0, 0, 1, "Portfolio/Equities/Europe/Core");
        model.setPoint(3, 9 + seededInt(seed, 0, 5, 175), 0, 0, 1, "Portfolio/Equities/Asia/Japan");
        model.setPoint(4, 11 + seededInt(seed, 0, 6, 176), 0, 0, 1, "Portfolio/FixedIncome/Government/US Treasuries");
        model.setPoint(5, 7 + seededInt(seed, 0, 4, 177), 0, 0, 1, "Portfolio/FixedIncome/Corporate/Investment Grade");
        model.setPoint(6, 5 + seededInt(seed, 0, 4, 178), 0, 0, 1, "Portfolio/Alternatives/RealEstate/REITs");
        model.setPoint(7, 4 + seededInt(seed, 0, 3, 179), 0, 0, 1, "Portfolio/Alternatives/Commodities/Metals");
        return model;
    }


    private static String simpleName(String className) {
        int idx = className.lastIndexOf('.');
        return idx >= 0 ? className.substring(idx + 1) : className;
    }

    private static int stableSeed(String className) {
        if (className == null || className.isBlank()) {
            return 1;
        }
        return Math.abs(className.hashCode()) + 1;
    }

    private static int seededInt(int seed, int min, int max, int salt) {
        int span = Math.max(1, (max - min) + 1);
        int value = Math.abs((seed * 31) ^ (salt * 131)) % span;
        return min + value;
    }

    private static double seededRange(int seed, double min, double max, int salt) {
        double n = (Math.abs((seed * 31) ^ (salt * 131)) % 10000) / 10000.0;
        return min + (max - min) * n;
    }
}
