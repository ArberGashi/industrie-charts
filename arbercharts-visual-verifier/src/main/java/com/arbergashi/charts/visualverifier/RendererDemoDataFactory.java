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
        int points = 240;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.08) * 28 + Math.cos(i * 0.03) * 14;
            double min = y - (8 + Math.sin(i * 0.2) * 4);
            double max = y + (8 + Math.cos(i * 0.2) * 4);
            double weight = Math.abs(y) + 10;
            String label = (i % 40 == 0) ? "P" + i : null;
            model.setPoint(x, y, min, max, weight, label);
        }
        return model;
    }

    private static DefaultChartModel circularModel() {
        DefaultChartModel model = new DefaultChartModel("Circular");
        String[] labels = {"FX", "Rates", "Equity", "Commodities", "Crypto", "Cash"};
        double[] weights = {26, 18, 22, 14, 12, 8};
        for (int i = 0; i < labels.length; i++) {
            double w = weights[i];
            model.setPoint(i, w, 0.0, w, w, labels[i]);
        }
        return model;
    }

    private static DefaultChartModel linkLabelModel() {
        DefaultChartModel model = new DefaultChartModel("Links");
        String[] pairs = {"A:B", "A:C", "B:D", "C:D", "C:E", "D:E", "E:A"};
        double[] weights = {12, 18, 10, 14, 9, 11, 7};
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
        int points = 180;
        for (int i = 0; i < points; i++) {
            double noise = Math.sin(i * 0.37) * 3 + Math.cos(i * 0.11) * 2;
            double sample = Math.sin(i * 0.09) * 18 + Math.cos(i * 0.04) * 9 + noise;
            double x = sample;
            double y = sample;
            double min = sample - 2.5;
            double max = sample + 2.5;
            double weight = Math.abs(sample) + 4;
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
        DefaultChartModel model = new DefaultChartModel("Correlation");
        int points = 240;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.05) * 24 + Math.cos(i * 0.02) * 9;
            double weight = y * 0.55 + Math.cos(i * 0.11) * 6;
            double min = y - 6;
            double max = y + 6;
            model.setPoint(x, y, min, max, weight, null);
        }
        return model;
    }

    private static DefaultFinancialChartModel financialModel() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("Financial");
        double price = 120.0;
        int points = 240;
        for (int i = 0; i < points; i++) {
            double wave = Math.sin(i * 0.08) * 2.5 + Math.cos(i * 0.03) * 4.0;
            double open = price;
            double close = price + wave;
            double high = Math.max(open, close) + 1.2;
            double low = Math.min(open, close) - 1.2;
            double volume = 700 + 400 * Math.abs(Math.sin(i * 0.05));
            model.setOHLC(i, open, high, low, close, volume, null);
            price = close;
        }
        return model;
    }

    private static DefaultStatisticalChartModel statisticalModel() {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel("Statistical");
        int points = 12;
        for (int i = 0; i < points; i++) {
            double center = 50 + Math.sin(i * 0.6) * 18;
            double q1 = center - 12 - Math.cos(i) * 2;
            double q3 = center + 12 + Math.sin(i) * 2;
            double min = q1 - 8 - i * 0.3;
            double max = q3 + 8 + i * 0.2;
            model.setBoxPlot(i, center, q1, q3, min, max, "S" + (i + 1));
        }
        return model;
    }

    private static CircularFastMedicalModel medicalModel() {
        int points = 1400;
        CircularFastMedicalModel model = new CircularFastMedicalModel(points, 3);
        double t = 0.0;
        for (int i = 0; i < points; i++) {
            double y1 = Math.sin(t * 0.05) * 0.9 + Math.sin(t * 0.2) * 0.2;
            double y2 = Math.cos(t * 0.03) * 0.7 + Math.sin(t * 0.11) * 0.3;
            double y3 = Math.sin(t * 0.02) * 0.4 + Math.cos(t * 0.07) * 0.2;
            model.add(t, new double[]{y1, y2, y3});
            t += 1.0;
        }
        return model;
    }

    private static DefaultFlowChartModel flowModel() {
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
        return new DefaultFlowChartModel(nodes, links).setName("Flow");
    }

    private static DefaultMatrixChartModel matrixModel() {
        double[][] matrix = {
                {0, 6, 2, 4, 3},
                {3, 0, 5, 1, 2},
                {4, 2, 0, 7, 5},
                {1, 5, 3, 0, 6},
                {2, 3, 4, 5, 0}
        };
        List<String> labels = List.of("Risk", "FX", "Rates", "Equity", "Macro");
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
        DefaultHierarchicalChartModel.DefaultNode root = new DefaultHierarchicalChartModel.DefaultNode("Portfolio", 0);
        DefaultHierarchicalChartModel.DefaultNode fx = new DefaultHierarchicalChartModel.DefaultNode("FX", 0);
        fx.setChild(new DefaultHierarchicalChartModel.DefaultNode("EURUSD", 32));
        fx.setChild(new DefaultHierarchicalChartModel.DefaultNode("USDJPY", 24));
        DefaultHierarchicalChartModel.DefaultNode rates = new DefaultHierarchicalChartModel.DefaultNode("Rates", 0);
        rates.setChild(new DefaultHierarchicalChartModel.DefaultNode("UST", 18));
        rates.setChild(new DefaultHierarchicalChartModel.DefaultNode("Bund", 12));
        DefaultHierarchicalChartModel.DefaultNode eq = new DefaultHierarchicalChartModel.DefaultNode("Equity", 0);
        eq.setChild(new DefaultHierarchicalChartModel.DefaultNode("US", 22));
        eq.setChild(new DefaultHierarchicalChartModel.DefaultNode("EU", 16));
        root.setChild(fx);
        root.setChild(rates);
        root.setChild(eq);
        return new DefaultHierarchicalChartModel(root).setName("Hierarchy");
    }

    private static String simpleName(String className) {
        int idx = className.lastIndexOf('.');
        return idx >= 0 ? className.substring(idx + 1) : className;
    }
}
