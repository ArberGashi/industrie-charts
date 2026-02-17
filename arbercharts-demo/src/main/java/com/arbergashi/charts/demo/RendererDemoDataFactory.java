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
    private static final Set<String> HIER_RENDERERS = Set.of(
            "SunburstRenderer"
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
        if (HIER_RENDERERS.contains(simple)) {
            return hierarchicalModel();
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
        int points = 280;
        double offset = (index - (total - 1) * 0.5) * 15.0;
        double phase = index * Math.PI / 3;
        double amplitude = 34 - index * 6;

        for (int i = 0; i < points; i++) {
            double x = i;
            double trend = i < 110 ? i * 0.09 : (i < 210 ? 9.9 - (i - 110) * 0.06 : 3.9 + (i - 210) * 0.08);
            double y = Math.sin(i * 0.05 + phase) * amplitude
                    + Math.sin(i * 0.018 + phase) * (amplitude * 0.42)
                    + trend
                    + offset;
            if (i == 84 || i == 192) {
                y += 14.0;
            }
            if (i == 148) {
                y -= 11.0;
            }

            double bandWidth = 4.0 + Math.abs(Math.sin(i * 0.08 + phase)) * 3.2;
            double min = y - bandWidth;
            double max = y + bandWidth;
            double weight = Math.abs(y - offset) * 0.6 + 8.0;

            String label = (i == 84) ? "Event A" : (i == 148) ? "Correction" : (i == 192) ? "Event B" : null;
            model.setPoint(x, y, min, max, weight, label);
        }
        return model;
    }

    private static DefaultChartModel circularModel() {
        DefaultChartModel model = new DefaultChartModel("Asset Allocation");
        String[] labels = {"US Equities", "Int'l Equities", "Fixed Income", "Real Estate", "Commodities", "Cash"};
        double[] weights = {35, 20, 25, 10, 6, 4};
        for (int i = 0; i < labels.length; i++) {
            double w = weights[i];
            model.setPoint(i, w, 0.0, w, w, labels[i]);
        }
        return model;
    }

    private static DefaultChartModel linkLabelModel() {
        DefaultChartModel model = new DefaultChartModel("Dependency Links");
        String[] pairs = {"Ingest:Validate", "Validate:Risk", "Risk:Route", "Route:Execute", "Execute:Settle", "Settle:Archive"};
        double[] weights = {24, 21, 17, 16, 13, 9};
        for (int i = 0; i < pairs.length; i++) {
            double w = weights[i % weights.length];
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
        int points = 220;
        for (int i = 0; i < points; i++) {
            double clusterA = Math.sin(i * 0.11) * 8.0 + 16.0;
            double clusterB = Math.cos(i * 0.13) * 7.0 - 12.0;
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
        int points = 96;
        for (int i = 0; i < points; i++) {
            double t = i * (Math.PI * 2.0 / points);
            double radius = 0.65 + 0.25 * Math.sin(i * 0.17);
            double x = Math.cos(t) * radius;
            double y = Math.sin(t) * radius;
            model.setPoint(x, y, y - 0.02, y + 0.02, radius, null);
        }
        return model;
    }

    private static DefaultChartModel correlationModel() {
        DefaultChartModel model = new DefaultChartModel("Moving Correlation");
        int points = 240;
        for (int i = 0; i < points; i++) {
            double x = i;
            double windowShift = (i < 110) ? 14.0 : (i < 175 ? -8.0 : 9.0);
            double y = Math.sin(i * 0.05) * 21 + Math.cos(i * 0.018) * 11 + windowShift;
            double weight = y * 0.48 + Math.cos(i * 0.11) * 5;
            double min = y - 6;
            double max = y + 6;
            model.setPoint(x, y, min, max, weight, null);
        }
        return model;
    }

    private static DefaultFinancialChartModel financialModel() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("AAPL - Daily");
        double price = 185.50;
        int points = 160;

        for (int i = 0; i < points; i++) {
            double regime = i < 52 ? 0.08 : (i < 104 ? -0.05 : 0.11);
            double cyclical = Math.sin(i * 0.16) * 1.45 + Math.cos(i * 0.07) * 0.95;
            double event = (i == 33) ? 5.8 : (i == 87 ? -7.2 : (i == 129 ? 6.3 : 0.0));
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

            String label = (i == 33) ? "Earnings +" : (i == 87) ? "Guidance -" : (i == 129) ? "Upgrade" : null;
            model.setOHLC(i, open, high, low, close, volume, label);
            price = close;
        }
        return model;
    }

    private static DefaultStatisticalChartModel statisticalModel() {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel("Quarterly Returns (%)");
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
            // median, q1, q3, min, max
            model.setBoxPlot(i, d[0], d[1], d[2], d[3], d[4], quarters[i]);
        }
        return model;
    }

    private static CircularFastMedicalModel medicalModel() {
        int sampleRate = 250; // 250 Hz - standard ECG
        double duration = 6.0; // 6 seconds
        int points = (int) (sampleRate * duration);
        CircularFastMedicalModel model = new CircularFastMedicalModel(points, 3);

        double heartRate = 72.0; // BPM
        double rrInterval = 60.0 / heartRate; // seconds between beats

        for (int i = 0; i < points; i++) {
            double t = i / (double) sampleRate;

            // Lead I - realistic ECG morphology
            double lead1 = generateECGBeat(t, rrInterval, 1.0);

            // Lead II - similar but slightly different amplitude
            double lead2 = generateECGBeat(t + 0.02, rrInterval, 1.2);

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
                new DefaultFlowChartModel.DefaultLink("orders", "validation", 100),
                new DefaultFlowChartModel.DefaultLink("validation", "risk", 95),
                new DefaultFlowChartModel.DefaultLink("validation", "rejected", 5),
                new DefaultFlowChartModel.DefaultLink("risk", "routing", 88),
                new DefaultFlowChartModel.DefaultLink("risk", "rejected", 7),
                new DefaultFlowChartModel.DefaultLink("routing", "exchange", 88),
                new DefaultFlowChartModel.DefaultLink("exchange", "filled", 82),
                new DefaultFlowChartModel.DefaultLink("exchange", "rejected", 6)
        );
        return new DefaultFlowChartModel(nodes, links).setName("Order Flow");
    }

    private static DefaultMatrixChartModel matrixModel() {
        // Correlation matrix between asset classes
        double[][] matrix = {
                {1.00, 0.85, 0.72, 0.45, 0.30},  // US Equities
                {0.85, 1.00, 0.68, 0.52, 0.35},  // Int'l Equities
                {0.72, 0.68, 1.00, 0.40, 0.25},  // Emerging Markets
                {0.45, 0.52, 0.40, 1.00, 0.60},  // Commodities
                {0.30, 0.35, 0.25, 0.60, 1.00}   // Fixed Income
        };
        List<String> labels = List.of("US Eq", "Int'l Eq", "EM", "Cmdty", "Bonds");
        return new DefaultMatrixChartModel(matrix, labels).setName("Correlation Matrix");
    }

    private static DefaultTernaryChartModel ternaryModel() {
        List<TernaryChartModel.TernaryPoint> points = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double a = 20 + (i % 6) * 3;
            double b = 50 + Math.sin(i * 0.4) * 10;
            double c = 100 - a - b;
            if (c < 5) c = 5;
            double scale = 100.0 / (a + b + c);
            points.add(new DefaultTernaryChartModel.DefaultTernaryPoint(a * scale, b * scale, c * scale));
        }
        return new DefaultTernaryChartModel(points, List.of("A", "B", "C")).setName("Ternary");
    }

    private static DefaultMultiDimensionalChartModel multiDimensionalModel() {
        List<double[]> data = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            double base = 50 + Math.sin(i * 0.2) * 10;
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
        DefaultHierarchicalChartModel.DefaultNode root = new DefaultHierarchicalChartModel.DefaultNode("Global Portfolio", 0);

        // Equities
        DefaultHierarchicalChartModel.DefaultNode equities = new DefaultHierarchicalChartModel.DefaultNode("Equities", 0);
        DefaultHierarchicalChartModel.DefaultNode usEq = new DefaultHierarchicalChartModel.DefaultNode("US", 0);
        usEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Large Cap", 25));
        usEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Mid Cap", 8));
        usEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Small Cap", 5));
        equities.setChild(usEq);
        DefaultHierarchicalChartModel.DefaultNode intlEq = new DefaultHierarchicalChartModel.DefaultNode("International", 0);
        intlEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Europe", 10));
        intlEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Asia Pacific", 8));
        intlEq.setChild(new DefaultHierarchicalChartModel.DefaultNode("Emerging", 6));
        equities.setChild(intlEq);

        // Fixed Income
        DefaultHierarchicalChartModel.DefaultNode bonds = new DefaultHierarchicalChartModel.DefaultNode("Fixed Income", 0);
        bonds.setChild(new DefaultHierarchicalChartModel.DefaultNode("Government", 12));
        bonds.setChild(new DefaultHierarchicalChartModel.DefaultNode("Corporate", 10));
        bonds.setChild(new DefaultHierarchicalChartModel.DefaultNode("High Yield", 4));

        // Alternatives
        DefaultHierarchicalChartModel.DefaultNode alts = new DefaultHierarchicalChartModel.DefaultNode("Alternatives", 0);
        alts.setChild(new DefaultHierarchicalChartModel.DefaultNode("Real Estate", 6));
        alts.setChild(new DefaultHierarchicalChartModel.DefaultNode("Commodities", 4));
        alts.setChild(new DefaultHierarchicalChartModel.DefaultNode("Cash", 2));

        root.setChild(equities);
        root.setChild(bonds);
        root.setChild(alts);

        return new DefaultHierarchicalChartModel(root).setName("Portfolio Allocation");
    }

    private static String simpleName(String className) {
        int idx = className.lastIndexOf('.');
        return idx >= 0 ? className.substring(idx + 1) : className;
    }
}
