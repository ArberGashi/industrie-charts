package com.arbergashi.charts.visualverifier;

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

    /**
     * Builds demo data for a specific renderer.
     *
     * @param category renderer category (e.g., "Financial", "Medical")
     * @param className fully qualified renderer class name
     * @return appropriate ChartModel with demo data
     */
    public static ChartModel build(String category, String className) {
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
        // Normalize category to lowercase for consistent matching
        String cat = category != null ? category.toLowerCase() : "";
        return switch (cat) {
            case "financial" -> financialModel();
            case "statistical" -> statisticalModel();
            case "medical" -> medicalModel();
            case "circular" -> circularModel();
            default -> standardModel();
        };
    }

    private static DefaultChartModel standardModel() {
        DefaultChartModel model = new DefaultChartModel("Standard");
        int seed = RENDERER_SEED.get();
        int points = 320;
        double baseline = 20.0 + seededRange(seed, 0.0, 8.0, 1);
        for (int i = 0; i < points; i++) {
            double x = i;
            int evtA = 72 + seededInt(seed, 0, 30, 2);
            int evtB = 146 + seededInt(seed, 0, 34, 3);
            int evtC = 206 + seededInt(seed, 0, 26, 4);
            double trend = i < 140 ? i * (0.09 + seededRange(seed, 0.0, 0.03, 5))
                    : (i < 250 ? 15.5 - (i - 140) * (0.05 + seededRange(seed, 0.0, 0.02, 6))
                    : 8.9 + (i - 250) * (0.07 + seededRange(seed, 0.0, 0.03, 7)));
            double seasonal = Math.sin(i * (0.06 + seededRange(seed, 0.0, 0.02, 8))) * 18
                    + Math.cos(i * (0.018 + seededRange(seed, 0.0, 0.01, 9))) * 10;
            double event = (i == evtA) ? 24 : (i == evtB ? -18 : (i == evtC ? 20 : 0));
            double y = baseline + trend + seasonal + event;
            double spread = 7 + Math.abs(Math.sin(i * 0.16)) * 4.5;
            double min = y - spread;
            double max = y + spread;
            double weight = 8 + Math.abs(y * 0.35) + Math.abs(event) * 0.6;
            String label = (i == evtA) ? "Spike" : (i == evtB) ? "Correction" : (i == evtC) ? "Breakout" : null;
            model.setPoint(x, y, min, max, weight, label);
        }
        return model;
    }

    private static DefaultChartModel circularModel() {
        DefaultChartModel model = new DefaultChartModel("Circular");
        int seed = RENDERER_SEED.get();
        String[] labels = {"FX", "Rates", "Equity", "Commodities", "Crypto", "Cash"};
        double[] weights = {
                22 + seededRange(seed, 0, 7, 10),
                15 + seededRange(seed, 0, 5, 11),
                19 + seededRange(seed, 0, 7, 12),
                10 + seededRange(seed, 0, 5, 13),
                8 + seededRange(seed, 0, 5, 14),
                4 + seededRange(seed, 0, 3, 15)
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
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Financial");
        int seed = RENDERER_SEED.get();
        double price = 142.0;
        int points = 300;
        for (int i = 0; i < points; i++) {
            int cutA = 78 + seededInt(seed, 0, 26, 30);
            int cutB = 152 + seededInt(seed, 0, 24, 31);
            int evtA = 60 + seededInt(seed, 0, 20, 32);
            int evtB = 144 + seededInt(seed, 0, 20, 33);
            int evtC = 220 + seededInt(seed, 0, 20, 34);
            double regime = i < cutA ? 0.16 : (i < cutB ? -0.11 : 0.21);
            double volatility = 1.8 + Math.abs(Math.sin(i * 0.045)) * 2.4;
            double drift = Math.sin(i * 0.09) * 1.4 + Math.cos(i * 0.031) * 2.1;
            double shock = (i == evtA) ? 8.5 : (i == evtB ? -10.0 : (i == evtC ? 7.2 : 0.0));
            double wave = drift + regime + shock;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + volatility;
            double low = Math.min(open, close) - volatility;
            double baseVolume = 1_200 + 700 * Math.abs(Math.sin(i * 0.052));
            double eventVolume = Math.abs(shock) * 220;
            double volume = baseVolume + eventVolume;
            String label = (i == evtA) ? "Earnings +" : (i == evtB) ? "Macro -" : (i == evtC) ? "Guidance +" : null;
            model.setOHLC(i, open, high, low, close, volume, label);
            price = close;
        }
        return model;
    }

    private static DefaultStatisticalChartModel statisticalModel() {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel("Statistical");
        int seed = RENDERER_SEED.get();
        int points = 16;
        for (int i = 0; i < points; i++) {
            double center = 55 + Math.sin(i * 0.48) * 15 + (i > 10 ? 6 : 0);
            double iqr = 10 + Math.abs(Math.cos(i * 0.4)) * 4;
            double q1 = center - iqr * 0.5;
            double q3 = center + iqr * 0.5;
            double min = q1 - 6 - Math.abs(Math.sin(i * 0.19)) * 3.5;
            double max = q3 + 6 + Math.abs(Math.cos(i * 0.23)) * 3.8;
            double shift = seededRange(seed, -0.9, 0.9, 40 + i);
            model.setBoxPlot(i, center + shift, q1 + shift, q3 + shift, min + shift, max + shift, "S" + (i + 1));
        }
        return model;
    }

    private static CircularFastMedicalModel medicalModel() {
        int seed = RENDERER_SEED.get();
        int points = 1400;
        CircularFastMedicalModel model = new CircularFastMedicalModel(points, 3);
        double t = 0.0;
        for (int i = 0; i < points; i++) {
            double phase = (i % 80) / 80.0;
            double pWave = gaussian(phase, 0.18, 0.03) * 0.15;
            double qrs = gaussian(phase, 0.32, 0.012) * 1.25
                    - gaussian(phase, 0.29, 0.01) * 0.35
                    - gaussian(phase, 0.35, 0.01) * 0.28;
            double tWave = gaussian(phase, 0.62, 0.06) * 0.33;
            double baseline = Math.sin(t * 0.01) * 0.03;
            double y1 = (pWave + qrs + tWave + baseline) * (0.95 + seededRange(seed, 0.0, 0.12, 50));
            double y2 = Math.sin(t * 0.045) * 0.55 + Math.cos(t * 0.018) * 0.24 + gaussian(phase, 0.34, 0.014) * 0.42;
            double y3 = Math.sin(t * 0.02) * 0.35 + Math.cos(t * 0.061) * 0.16 + gaussian(phase, 0.62, 0.07) * 0.21;
            model.add(t, new double[]{y1, y2, y3});
            t += 1.0;
        }
        return model;
    }

    private static DefaultFlowChartModel flowModel() {
        int seed = RENDERER_SEED.get();
        List<DefaultFlowChartModel.DefaultNode> nodes = List.of(
                new DefaultFlowChartModel.DefaultNode("a", "Gateway"),
                new DefaultFlowChartModel.DefaultNode("b", "Analytics"),
                new DefaultFlowChartModel.DefaultNode("c", "Risk"),
                new DefaultFlowChartModel.DefaultNode("d", "Execution"),
                new DefaultFlowChartModel.DefaultNode("e", "Settlement")
        );
        List<DefaultFlowChartModel.DefaultLink> links = List.of(
                new DefaultFlowChartModel.DefaultLink("a", "b", 26 + seededInt(seed, 0, 8, 60)),
                new DefaultFlowChartModel.DefaultLink("a", "c", 14 + seededInt(seed, 0, 6, 61)),
                new DefaultFlowChartModel.DefaultLink("b", "d", 20 + seededInt(seed, 0, 8, 62)),
                new DefaultFlowChartModel.DefaultLink("c", "d", 10 + seededInt(seed, 0, 6, 63)),
                new DefaultFlowChartModel.DefaultLink("d", "e", 18 + seededInt(seed, 0, 7, 64))
        );
        return new DefaultFlowChartModel(nodes, links).setName("Flow");
    }

    private static DefaultMatrixChartModel matrixModel() {
        int seed = RENDERER_SEED.get();
        double[][] matrix = {
                {0.0, 0.76 + seededRange(seed, -0.07, 0.07, 70), 0.55 + seededRange(seed, -0.07, 0.07, 71), 0.34 + seededRange(seed, -0.07, 0.07, 72), -0.10 + seededRange(seed, -0.07, 0.07, 73)},
                {0.76 + seededRange(seed, -0.07, 0.07, 74), 0.0, 0.48 + seededRange(seed, -0.07, 0.07, 75), 0.31 + seededRange(seed, -0.07, 0.07, 76), -0.08 + seededRange(seed, -0.07, 0.07, 77)},
                {0.55 + seededRange(seed, -0.07, 0.07, 78), 0.48 + seededRange(seed, -0.07, 0.07, 79), 0.0, 0.41 + seededRange(seed, -0.07, 0.07, 80), 0.09 + seededRange(seed, -0.07, 0.07, 81)},
                {0.34 + seededRange(seed, -0.07, 0.07, 82), 0.31 + seededRange(seed, -0.07, 0.07, 83), 0.41 + seededRange(seed, -0.07, 0.07, 84), 0.0, 0.24 + seededRange(seed, -0.07, 0.07, 85)},
                {-0.10 + seededRange(seed, -0.07, 0.07, 86), -0.08 + seededRange(seed, -0.07, 0.07, 87), 0.09 + seededRange(seed, -0.07, 0.07, 88), 0.24 + seededRange(seed, -0.07, 0.07, 89), 0.0}
        };
        List<String> labels = List.of("US Eq", "EU Eq", "Rates", "Cmdty", "Vol");
        return new DefaultMatrixChartModel(matrix, labels).setName("Matrix");
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
        int seed = RENDERER_SEED.get();
        List<double[]> data = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            double base = 48 + Math.sin(i * (0.16 + seededRange(seed, 0.0, 0.05, 90))) * 10;
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
        DefaultHierarchicalChartModel.DefaultNode equities = new DefaultHierarchicalChartModel.DefaultNode("Equities", 0);
        equities.setChild(new DefaultHierarchicalChartModel.DefaultNode("US", 30 + seededInt(seed, 0, 8, 100)));
        equities.setChild(new DefaultHierarchicalChartModel.DefaultNode("Europe", 14 + seededInt(seed, 0, 6, 101)));
        equities.setChild(new DefaultHierarchicalChartModel.DefaultNode("EM", 8 + seededInt(seed, 0, 4, 102)));

        DefaultHierarchicalChartModel.DefaultNode fixedIncome = new DefaultHierarchicalChartModel.DefaultNode("Fixed Income", 0);
        fixedIncome.setChild(new DefaultHierarchicalChartModel.DefaultNode("Gov", 12 + seededInt(seed, 0, 6, 103)));
        fixedIncome.setChild(new DefaultHierarchicalChartModel.DefaultNode("IG Corp", 9 + seededInt(seed, 0, 5, 104)));
        fixedIncome.setChild(new DefaultHierarchicalChartModel.DefaultNode("HY", 4 + seededInt(seed, 0, 3, 105)));

        DefaultHierarchicalChartModel.DefaultNode alternatives = new DefaultHierarchicalChartModel.DefaultNode("Alternatives", 0);
        alternatives.setChild(new DefaultHierarchicalChartModel.DefaultNode("REIT", 3 + seededInt(seed, 0, 3, 106)));
        alternatives.setChild(new DefaultHierarchicalChartModel.DefaultNode("Commodities", 2 + seededInt(seed, 0, 2, 107)));

        root.setChild(equities);
        root.setChild(fixedIncome);
        root.setChild(alternatives);
        return new DefaultHierarchicalChartModel(root).setName("Hierarchy");
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

    private static double gaussian(double x, double mean, double sigma) {
        double d = (x - mean) / sigma;
        return Math.exp(-0.5 * d * d);
    }
}
