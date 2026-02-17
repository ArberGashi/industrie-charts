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
    private static final Set<String> GAUGE_RENDERERS = Set.of(
            "GaugeRenderer",
            "GaugeBandsRenderer"
    );
    private static final Set<String> SEMI_DONUT_RENDERERS = Set.of(
            "SemiDonutRenderer"
    );
    private static final Set<String> PIE_DONUT_RENDERERS = Set.of(
            "PieRenderer",
            "DonutRenderer"
    );
    private static final Set<String> NIGHTINGALE_RENDERERS = Set.of(
            "NightingaleRoseRenderer"
    );
    private static final Set<String> RADIAL_BAR_RENDERERS = Set.of(
            "RadialBarRenderer"
    );
    private static final Set<String> RADIAL_STACKED_RENDERERS = Set.of(
            "RadialStackedRenderer"
    );
    private static final Set<String> CHORD_RENDERERS = Set.of(
            "ChordDiagramRenderer"
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
    private static final Set<String> SPECTRAL_ANALYSIS_RENDERERS = Set.of(
            "AutocorrelationRenderer",
            "FourierOverlayRenderer",
            "LiveFFTRenderer"
    );
    private static final Set<String> TREND_ANALYSIS_RENDERERS = Set.of(
            "LoessRenderer",
            "MovingAverageRenderer",
            "PolynomialRegressionRenderer",
            "RegressionLineRenderer",
            "TrendDecompositionRenderer"
    );
    private static final Set<String> OVERLAY_ANALYSIS_RENDERERS = Set.of(
            "EnvelopeRenderer",
            "MinMaxMarkerRenderer",
            "PeakDetectionRenderer",
            "ReferenceLineRenderer",
            "ThresholdRenderer"
    );
    private static final Set<String> FUNCTION_ANALYSIS_RENDERERS = Set.of(
            "AdaptiveFunctionRenderer"
    );
    private static final Set<String> FIELD_ANALYSIS_RENDERERS = Set.of(
            "VectorFieldRenderer"
    );
    private static final Set<String> POLAR_ADV_RENDERERS = Set.of(
            "PolarAdvancedRenderer"
    );
    private static final Set<String> POLAR_RENDERERS = Set.of(
            "PolarRenderer"
    );
    private static final Set<String> POLAR_LINE_RENDERERS = Set.of(
            "PolarLineRenderer"
    );
    private static final Set<String> RADAR_RENDERERS = Set.of(
            "RadarRenderer"
    );
    private static final Set<String> HEATMAP_RENDERERS = Set.of(
            "HeatmapRenderer",
            "HeatmapContourRenderer"
    );
    private static final Set<String> SPECTROGRAM_RENDERERS = Set.of(
            "SpectrogramRenderer"
    );
    private static final Set<String> HORIZON_RENDERERS = Set.of(
            "HorizonRenderer",
            "HorizonChartRenderer",
            "SparklineRenderer"
    );
    private static final Set<String> PARETO_RENDERERS = Set.of(
            "ParetoRenderer"
    );
    private static final Set<String> LOLLIPOP_RENDERERS = Set.of(
            "LollipopRenderer"
    );
    private static final Set<String> MARIMEKKO_RENDERERS = Set.of(
            "MarimekkoRenderer"
    );
    private static final Set<String> VORONOI_RENDERERS = Set.of(
            "VoronoiRenderer",
            "DelaunayRenderer",
            "HexbinRenderer"
    );
    private static final Set<String> DENDROGRAM_RENDERERS = Set.of(
            "DendrogramRenderer"
    );
    private static final Set<String> PREDICTIVE_RENDERERS = Set.of(
            "PredictiveShadowRenderer",
            "AnomalyGapRenderer",
            "PredictiveCandleRenderer"
    );
    private static final Set<String> FORENSIC_RENDERERS = Set.of(
            "PlaybackStatusRenderer"
    );
    private static final Set<String> SECURITY_RENDERERS = Set.of(
            "VoxelCloudRenderer"
    );
    private static final Set<String> COMMON_RENDERERS = Set.of(
            "PerformanceAuditRenderer",
            "PhysicalScaleRenderer"
    );

    private RendererDemoDataFactory() {
    }

    static ChartModel build(String category, String className) {
        RENDERER_SEED.set(stableSeed(className));
        String simple = simpleName(className);
        if (FLOW_RENDERERS.contains(simple)) {
            return flowModel();
        }
        if (GAUGE_RENDERERS.contains(simple)) {
            return gaugeModel();
        }
        if (SEMI_DONUT_RENDERERS.contains(simple)) {
            return semiDonutModel();
        }
        if (PIE_DONUT_RENDERERS.contains(simple)) {
            return pieDonutModel();
        }
        if (NIGHTINGALE_RENDERERS.contains(simple)) {
            return nightingaleModel();
        }
        if (RADIAL_BAR_RENDERERS.contains(simple)) {
            return radialBarModel();
        }
        if (RADIAL_STACKED_RENDERERS.contains(simple)) {
            return radialStackedModel();
        }
        if (CHORD_RENDERERS.contains(simple)) {
            return chordFlowMatrixModel();
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
        if (SPECTRAL_ANALYSIS_RENDERERS.contains(simple)) {
            return spectralAnalysisModel();
        }
        if (TREND_ANALYSIS_RENDERERS.contains(simple)) {
            return trendAnalysisModel();
        }
        if (OVERLAY_ANALYSIS_RENDERERS.contains(simple)) {
            return overlayAnalysisModel();
        }
        if (FUNCTION_ANALYSIS_RENDERERS.contains(simple)) {
            return functionDomainModel();
        }
        if (FIELD_ANALYSIS_RENDERERS.contains(simple)) {
            return vectorFieldDomainModel();
        }
        if (POLAR_ADV_RENDERERS.contains(simple)) {
            return polarAdvancedModel();
        }
        if (POLAR_RENDERERS.contains(simple)) {
            return polarModel();
        }
        if (POLAR_LINE_RENDERERS.contains(simple)) {
            return polarLineModel();
        }
        if (RADAR_RENDERERS.contains(simple)) {
            return radarModel();
        }
        if (HEATMAP_RENDERERS.contains(simple)) {
            return heatmapModel();
        }
        if (SPECTROGRAM_RENDERERS.contains(simple)) {
            return spectrogramModel();
        }
        if (HORIZON_RENDERERS.contains(simple)) {
            return horizonModel();
        }
        if (PARETO_RENDERERS.contains(simple)) {
            return paretoModel();
        }
        if (LOLLIPOP_RENDERERS.contains(simple)) {
            return lollipopModel();
        }
        if (MARIMEKKO_RENDERERS.contains(simple)) {
            return marimekkoModel();
        }
        if (VORONOI_RENDERERS.contains(simple)) {
            return voronoiModel();
        }
        if (DENDROGRAM_RENDERERS.contains(simple)) {
            return dendrogramModel();
        }
        if (PREDICTIVE_RENDERERS.contains(simple)) {
            return predictiveModel();
        }
        if (FORENSIC_RENDERERS.contains(simple)) {
            return forensicModel();
        }
        if (SECURITY_RENDERERS.contains(simple)) {
            return securityVoxelModel();
        }
        if (COMMON_RENDERERS.contains(simple)) {
            return commonUtilityModel();
        }
        return switch (category) {
            case "standard" -> standardRendererModel(simple);
            case "financial" -> financialRendererModel(simple);
            case "statistical" -> statisticalRendererModel(simple);
            case "medical" -> medicalRendererModel(simple);
            case "specialized" -> specializedRendererModel(simple);
            case "circular" -> circularRendererModel(simple);
            case "predictive" -> predictiveRendererModel(simple);
            case "forensic" -> forensicRendererModel(simple);
            case "analysis" -> analysisRendererModel(simple);
            case "security" -> securityRendererModel(simple);
            case "common" -> commonRendererModel(simple);
            default -> standardRendererModel(simple);
        };
    }

    private static ChartModel standardRendererModel(String simple) {
        return switch (simple) {
            case "ScatterRenderer", "BubbleRenderer" -> distributionModel();
            case "RangeRenderer", "BaselineAreaRenderer", "StepAreaRenderer", "StackedAreaRenderer" -> trendAnalysisModel();
            default -> standardModel();
        };
    }

    private static ChartModel financialRendererModel(String simple) {
        return switch (simple) {
            case "VolumeRenderer", "VolumeProfileRenderer", "LiquidityHeatmapRenderer" -> distributionModel();
            case "WaterfallRenderer" -> paretoModel();
            default -> financialModel();
        };
    }

    private static ChartModel statisticalRendererModel(String simple) {
        return switch (simple) {
            case "HistogramRenderer", "QQPlotRenderer", "QuantileRegressionRenderer", "ViolinPlotRenderer", "BoxPlotRenderer" -> statisticalModel();
            case "BandRenderer", "ErrorBarRenderer", "ConfidenceIntervalRenderer", "QuantileBandRenderer", "StatisticalErrorBarRenderer" -> trendAnalysisModel();
            case "BeeswarmRenderer", "DotPlotRenderer", "RidgeLineRenderer", "LiveDistributionOverlayRenderer" -> distributionModel();
            default -> statisticalModel();
        };
    }

    private static ChartModel medicalRendererModel(String simple) {
        return switch (simple) {
            case "CalibrationRenderer", "SpectrogramMedicalRenderer", "SpirometryRenderer" -> medicalModel();
            default -> medicalModel();
        };
    }

    private static ChartModel specializedRendererModel(String simple) {
        return switch (simple) {
            case "ArcDiagramRenderer", "BulletChartRenderer", "ControlChartRenderer", "JoyplotRenderer", "RadarGlowRenderer", "WindRoseRenderer" -> trendAnalysisModel();
            case "CandlestickHollowRenderer", "GanttResourceViewRenderer", "ChernoffFacesRenderer" -> standardModel();
            default -> standardModel();
        };
    }

    private static ChartModel circularRendererModel(String simple) {
        return switch (simple) {
            case "CircularLatencyOverlayRenderer" -> gaugeModel();
            default -> circularModel();
        };
    }

    private static ChartModel predictiveRendererModel(String simple) {
        return predictiveModel();
    }

    private static ChartModel forensicRendererModel(String simple) {
        return forensicModel();
    }

    private static ChartModel analysisRendererModel(String simple) {
        return switch (simple) {
            case "ChangePointRenderer", "OutlierDetectionRenderer" -> anomalyModel();
            case "SlopeRenderer" -> slopeModel();
            case "MovingCorrelationRenderer" -> correlationModel();
            case "AdaptiveFunctionRenderer" -> functionDomainModel();
            case "VectorFieldRenderer" -> vectorFieldDomainModel();
            case "AutocorrelationRenderer", "LiveFFTRenderer", "FourierOverlayRenderer" -> spectralAnalysisModel();
            case "LoessRenderer", "MovingAverageRenderer", "PolynomialRegressionRenderer", "RegressionLineRenderer", "TrendDecompositionRenderer" -> trendAnalysisModel();
            default -> overlayAnalysisModel();
        };
    }

    private static ChartModel securityRendererModel(String simple) {
        return securityVoxelModel();
    }

    private static ChartModel commonRendererModel(String simple) {
        return commonUtilityModel();
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

    private static DefaultChartModel gaugeModel() {
        DefaultChartModel model = new DefaultChartModel("Latency SLA");
        int seed = RENDERER_SEED.get();
        double y = 68.0 + seededRange(seed, 0.0, 24.0, 2500);
        model.setPoint(0, y, y - 3.0, y + 3.0, y, "SLA Health");
        return model;
    }

    private static DefaultChartModel semiDonutModel() {
        DefaultChartModel model = new DefaultChartModel("Adoption Progress");
        int seed = RENDERER_SEED.get();
        double y = 72.0 + seededRange(seed, 0.0, 24.0, 2501);
        model.setPoint(0, y, y - 3.0, y + 3.0, y, "Completion");
        return model;
    }

    private static DefaultChartModel pieDonutModel() {
        DefaultChartModel model = new DefaultChartModel("Revenue Mix");
        int seed = RENDERER_SEED.get();
        String[] labels = {"Enterprise", "SMB", "Cloud", "On-Prem", "Support", "Training", "Partners"};
        double[] weights = {34, 19, 16, 12, 8, 6, 5};
        for (int i = 0; i < labels.length; i++) {
            double w = weights[i] + seededRange(seed, -2.0, 2.0, 260 + i);
            w = Math.max(1.0, w);
            model.setPoint(i, w, 0.0, w, w, labels[i]);
        }
        return model;
    }

    private static DefaultChartModel nightingaleModel() {
        DefaultChartModel model = new DefaultChartModel("Monthly Incident Load");
        int seed = RENDERER_SEED.get();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 0; i < months.length; i++) {
            double seasonal = 42 + Math.sin(i * 0.63 + seededRange(seed, 0.0, 0.35, 270)) * 14;
            double burst = (i == 2 || i == 8) ? 11 : 0;
            double value = Math.max(8.0, seasonal + burst);
            model.setPoint(i, value, value - 4.0, value + 4.0, value, months[i]);
        }
        return model;
    }

    private static DefaultChartModel radialBarModel() {
        DefaultChartModel model = new DefaultChartModel("Capability Ranking");
        int seed = RENDERER_SEED.get();
        String[] dimensions = {"Latency", "Scale", "Stability", "Security", "UX", "DX", "Coverage", "Efficiency"};
        double[] base = {88, 81, 92, 85, 77, 80, 84, 79};
        for (int i = 0; i < dimensions.length; i++) {
            double y = base[i] + seededRange(seed, -4.0, 4.0, 280 + i);
            y = Math.max(20.0, Math.min(100.0, y));
            model.setPoint(i, y, y - 3.0, y + 3.0, y, dimensions[i]);
        }
        return model;
    }

    private static DefaultChartModel radialStackedModel() {
        DefaultChartModel model = new DefaultChartModel("Workload Composition");
        int seed = RENDERER_SEED.get();
        String[] segments = {"API", "Batch", "Stream", "ETL", "ML", "Search", "Auth", "Cache", "Queue", "Edge"};
        for (int i = 0; i < segments.length; i++) {
            double baseline = 26 + seededRange(seed, -6.0, 8.0, 290 + i);
            double layer = 18 + seededRange(seed, -5.0, 9.0, 310 + i);
            baseline = Math.max(8.0, baseline);
            layer = Math.max(6.0, layer);
            model.setPoint(baseline, layer, baseline, baseline + layer, layer, segments[i]);
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
        int seed = RENDERER_SEED.get();
        double a = 10.0 + seededRange(seed, 0.0, 8.0, 2502);
        double b = a + 10.0 + seededRange(seed, 0.0, 16.0, 2503);
        model.setPoint(0, a, a - 2.0, a + 2.0, 1.0, "A");
        model.setPoint(1, b, b - 2.0, b + 2.0, 1.0, "B");
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
        int points = 128;
        for (int i = 0; i < points; i++) {
            double t = i * (Math.PI * 2.0 / points);
            double envelope = 0.58 + 0.2 * Math.sin(i * (0.11 + seededRange(seed, 0.0, 0.03, 40)));
            double resonance = 0.12 * Math.cos(i * (0.23 + seededRange(seed, 0.0, 0.04, 41)));
            double radius = Math.max(0.08, Math.min(0.96, envelope + resonance));
            double phaseSkew = 0.28 * Math.sin(i * (0.09 + seededRange(seed, 0.0, 0.02, 42)));
            double x = Math.cos(t + phaseSkew) * radius;
            double y = Math.sin(t - phaseSkew * 0.7) * radius;
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

    private static DefaultChartModel spectralAnalysisModel() {
        DefaultChartModel model = new DefaultChartModel("Spectral Analysis");
        int seed = RENDERER_SEED.get();
        int points = 320;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * (0.07 + seededRange(seed, 0.0, 0.02, 900))) * 22.0
                    + Math.sin(i * (0.17 + seededRange(seed, 0.0, 0.03, 901))) * 11.0
                    + Math.cos(i * (0.31 + seededRange(seed, 0.0, 0.03, 902))) * 6.5;
            if (i > 210 && i < 245) {
                y += 10.0;
            }
            if (i > 90 && i < 120) {
                y -= 8.0;
            }
            double band = 3.0 + Math.abs(Math.sin(i * 0.09)) * 2.0;
            model.setPoint(x, y, y - band, y + band, Math.abs(y) * 0.35 + 4.0, null);
        }
        return model;
    }

    private static DefaultChartModel trendAnalysisModel() {
        DefaultChartModel model = new DefaultChartModel("Trend Analysis");
        int seed = RENDERER_SEED.get();
        int points = 300;
        for (int i = 0; i < points; i++) {
            double x = i;
            double regime = i < 110 ? (i * 0.08) : (i < 210 ? 8.8 - (i - 110) * 0.05 : 3.8 + (i - 210) * 0.06);
            double y = regime
                    + Math.sin(i * (0.06 + seededRange(seed, 0.0, 0.02, 910))) * 9.0
                    + Math.cos(i * (0.19 + seededRange(seed, 0.0, 0.02, 911))) * 3.5;
            if (i == 72 || i == 188 || i == 252) {
                y += 11.0;
            }
            if (i == 138) {
                y -= 9.5;
            }
            double band = 2.4 + Math.abs(Math.cos(i * 0.11)) * 1.6;
            model.setPoint(x, y, y - band, y + band, Math.abs(y) * 0.4 + 5.0, null);
        }
        return model;
    }

    private static DefaultChartModel overlayAnalysisModel() {
        DefaultChartModel model = new DefaultChartModel("Overlay Analysis");
        int seed = RENDERER_SEED.get();
        int points = 240;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * (0.075 + seededRange(seed, 0.0, 0.01, 920))) * 18.0
                    + Math.cos(i * (0.022 + seededRange(seed, 0.0, 0.01, 921))) * 8.5;
            if (i == 54 || i == 151 || i == 203) {
                y += 20.0;
            }
            if (i == 108) {
                y -= 16.0;
            }
            double band = 2.0 + Math.abs(Math.sin(i * 0.08)) * 1.4;
            model.setPoint(x, y, y - band, y + band, Math.abs(y) * 0.35 + 4.5, null);
        }
        return model;
    }

    private static DefaultChartModel functionDomainModel() {
        DefaultChartModel model = new DefaultChartModel("Function Domain");
        int points = 220;
        double minX = -36.0;
        double maxX = 36.0;
        double span = maxX - minX;
        for (int i = 0; i < points; i++) {
            double x = minX + (i / (double) (points - 1)) * span;
            double y = Math.sin(x * 0.55) * 26.0 + Math.cos(x * 0.23) * 12.0;
            model.setPoint(x, y, y - 2.0, y + 2.0, Math.abs(y) * 0.3 + 3.0, null);
        }
        return model;
    }

    private static DefaultChartModel vectorFieldDomainModel() {
        DefaultChartModel model = new DefaultChartModel("Vector Domain");
        int n = 21;
        for (int i = 0; i < n; i++) {
            double x = -10.0 + i;
            double y = -10.0 + i;
            model.setPoint(x, y, y - 0.5, y + 0.5, 1.0, null);
        }
        return model;
    }

    private static DefaultChartModel polarAdvancedModel() {
        DefaultChartModel model = new DefaultChartModel("Polar Advanced");
        int seed = RENDERER_SEED.get();
        int n = 12;
        for (int i = 0; i < n; i++) {
            double phase = (i / (double) n) * Math.PI * 2.0;
            double base = 14.0 + Math.sin(phase * 1.7 + seededRange(seed, 0.0, 0.5, 201)) * 4.0;
            double value = 9.0 + Math.cos(phase * 1.3 + seededRange(seed, 0.0, 0.4, 202)) * 3.0;
            base = Math.max(4.0, base);
            value = Math.max(3.0, value);
            model.setPoint(base, value, 0.0, base + value, value, "Sector " + (i + 1));
        }
        return model;
    }

    private static DefaultChartModel polarModel() {
        DefaultChartModel model = new DefaultChartModel("Global Wind Field");
        int seed = RENDERER_SEED.get();
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (360.0 / points) * i;
            double base = 44.0 + Math.sin(i * 0.58 + seededRange(seed, 0.0, 0.45, 210)) * 16.0;
            double gust = 7.5 + Math.cos(i * 0.34 + seededRange(seed, 0.0, 0.35, 211)) * 3.8;
            double radius = Math.max(8.0, base + gust);
            double weight = Math.max(0.08, 0.52 + Math.sin(i * 0.41 + seededRange(seed, 0.0, 0.3, 212)) * 0.35);
            model.setPoint(angle, radius, radius - 4.0, radius + 4.0, Math.min(1.0, weight), "Dir " + (i * 15) + "°");
        }
        return model;
    }

    private static DefaultChartModel polarLineModel() {
        DefaultChartModel model = new DefaultChartModel("Orbital Profile");
        int seed = RENDERER_SEED.get();
        int points = 18;
        for (int i = 0; i < points; i++) {
            double angle = (360.0 / points) * i;
            double wave = 48.0 + Math.sin(i * 0.72 + seededRange(seed, 0.0, 0.4, 220)) * 18.0;
            double harmonic = Math.cos(i * 1.2 + seededRange(seed, 0.0, 0.35, 221)) * 9.0;
            double radius = Math.max(10.0, wave + harmonic);
            model.setPoint(angle, radius, radius - 3.5, radius + 3.5, Math.max(6.0, radius * 0.4), "Node " + (i + 1));
        }
        return model;
    }

    private static DefaultChartModel radarModel() {
        DefaultChartModel model = new DefaultChartModel("Platform Capability Matrix");
        int seed = RENDERER_SEED.get();
        String[] axes = {
                "Latency", "Throughput", "Resilience", "Coverage",
                "Security", "Scalability", "DevEx", "Observability"
        };
        double[] targets = {82, 88, 79, 91, 86, 84, 77, 89};
        for (int i = 0; i < axes.length; i++) {
            double y = targets[i] + seededRange(seed, -4.0, 4.0, 230 + i);
            y = Math.max(40.0, Math.min(98.0, y));
            model.setPoint(i, y, y - 5.0, y + 5.0, y, axes[i]);
        }
        return model;
    }

    private static DefaultChartModel heatmapModel() {
        DefaultChartModel model = new DefaultChartModel("Traffic Density");
        int seed = RENDERER_SEED.get();
        int points = 640;
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double c1x = 2.2 * Math.cos(2.0 * Math.PI * t);
            double c1y = 1.6 * Math.sin(2.0 * Math.PI * t);
            double c2x = -1.4 + 1.8 * Math.cos(6.0 * Math.PI * t);
            double c2y = 0.9 * Math.sin(4.0 * Math.PI * t);
            double x = 0.62 * c1x + 0.38 * c2x + seededRange(seed, -0.16, 0.16, 520 + i);
            double y = 0.62 * c1y + 0.38 * c2y + seededRange(seed, -0.14, 0.14, 2200 + i);
            double r2 = x * x + y * y;
            double hotA = Math.exp(-r2 * 0.82);
            double hotB = Math.exp(-((x - 1.25) * (x - 1.25) + (y + 0.55) * (y + 0.55)) * 1.4);
            double weight = 40.0 + (hotA * 68.0) + (hotB * 52.0);
            model.setPoint(x, y, y - 0.08, y + 0.08, weight, null);
        }
        return model;
    }

    private static DefaultChartModel spectrogramModel() {
        DefaultChartModel model = new DefaultChartModel("Spectral Signature");
        int seed = RENDERER_SEED.get();
        int points = 720;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = 0.42
                    + 0.26 * Math.sin(i * 0.035 + seededRange(seed, -0.2, 0.2, 540))
                    + 0.21 * Math.sin(i * 0.092 + seededRange(seed, -0.3, 0.3, 541))
                    + 0.11 * Math.cos(i * 0.16 + seededRange(seed, -0.4, 0.4, 542));
            y = Math.max(0.02, Math.min(0.98, y));
            model.setPoint(x, y, y - 0.02, y + 0.02, y * 100.0, null);
        }
        return model;
    }

    private static DefaultChartModel horizonModel() {
        DefaultChartModel model = new DefaultChartModel("Demand Volatility");
        int seed = RENDERER_SEED.get();
        int points = 300;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.08 + seededRange(seed, -0.2, 0.2, 560)) * 36.0
                    + Math.sin(i * 0.025 + seededRange(seed, -0.2, 0.2, 561)) * 18.0
                    + Math.cos(i * 0.15 + seededRange(seed, -0.2, 0.2, 562)) * 9.0;
            if (i > 95 && i < 140) {
                y += 18.0;
            }
            if (i > 205 && i < 240) {
                y -= 22.0;
            }
            model.setPoint(x, y, y - 4.0, y + 4.0, Math.abs(y), null);
        }
        return model;
    }

    private static DefaultChartModel paretoModel() {
        DefaultChartModel model = new DefaultChartModel("Incident Pareto");
        int seed = RENDERER_SEED.get();
        String[] causes = {
                "Input Validation", "Dependency Drift", "Timeout", "Memory Pressure", "Config Drift",
                "I/O Saturation", "Retry Storm", "TLS Handshake", "Network Jitter", "Disk IOPS",
                "Serialization", "Unknown"
        };
        double[] base = {44, 33, 28, 24, 21, 18, 14, 12, 10, 9, 7, 5};
        for (int i = 0; i < causes.length; i++) {
            double y = Math.max(2.0, base[i] + seededRange(seed, -3.0, 2.2, 580 + i));
            model.setPoint(i + 1, y, 0.0, y, y, causes[i]);
        }
        return model;
    }

    private static DefaultChartModel lollipopModel() {
        DefaultChartModel model = new DefaultChartModel("Segment Lift");
        int seed = RENDERER_SEED.get();
        String[] segments = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        for (int i = 0; i < segments.length; i++) {
            double wave = Math.sin(i * 0.72 + seededRange(seed, -0.25, 0.25, 610));
            double y = 38.0 + wave * 22.0 + seededRange(seed, -6.0, 6.0, 611 + i);
            y = Math.max(4.0, y);
            model.setPoint(i + 1, y, 0.0, y, y, segments[i]);
        }
        return model;
    }

    private static DefaultChartModel marimekkoModel() {
        DefaultChartModel model = new DefaultChartModel("Market Mix");
        int seed = RENDERER_SEED.get();
        String[][] groups = {
                {"Cloud", "On-Prem", "Hybrid"},
                {"Retail", "Enterprise", "Public"},
                {"North", "EMEA", "APAC"},
                {"Core", "Add-ons", "Support"}
        };
        double[] widths = {34, 26, 22, 18};
        for (int g = 0; g < groups.length; g++) {
            double total = 96.0 + seededRange(seed, -6.0, 6.0, 640 + g);
            for (int s = 0; s < groups[g].length; s++) {
                double share = (s == 0 ? 0.48 : (s == 1 ? 0.33 : 0.19));
                double y = Math.max(2.0, total * share + seededRange(seed, -3.0, 3.0, 660 + g * 10 + s));
                double width = s == 0 ? widths[g] : 0.0;
                model.setPoint(g, y, 0.0, y, width, groups[g][s]);
            }
        }
        return model;
    }

    private static DefaultChartModel voronoiModel() {
        DefaultChartModel model = new DefaultChartModel("Territory Seeds");
        int seed = RENDERER_SEED.get();
        int points = 42;
        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI * i) / points;
            double radius = 12.0 + (i % 7) * 9.0 + seededRange(seed, -3.0, 3.0, 700 + i);
            double x = Math.cos(angle) * radius + seededRange(seed, -6.0, 6.0, 740 + i);
            double y = Math.sin(angle) * radius + seededRange(seed, -6.0, 6.0, 780 + i);
            model.setPoint(x, y, y - 1.0, y + 1.0, 1.0, "Cell " + (i + 1));
        }
        return model;
    }

    private static DefaultChartModel dendrogramModel() {
        DefaultChartModel model = new DefaultChartModel("Cluster Hierarchy");
        int seed = RENDERER_SEED.get();
        int groups = 16;
        for (int g = 0; g < groups; g++) {
            double baseX = g * 2.0;
            double center = 72.0 - g * 2.4 + seededRange(seed, -1.5, 1.5, 820 + g);
            double spread = 8.0 + seededRange(seed, -1.2, 1.2, 840 + g);
            model.setPoint(baseX, center + spread, center + spread - 2.0, center + spread + 2.0, 1.0, null);
            model.setPoint(baseX + 1.0, center - spread, center - spread - 2.0, center - spread + 2.0, 1.0, null);
        }
        return model;
    }

    private static DefaultChartModel predictiveModel() {
        DefaultChartModel model = new DefaultChartModel("Predictive Drift");
        int seed = RENDERER_SEED.get();
        int points = 260;
        for (int i = 0; i < points; i++) {
            double x = i;
            double baseline = Math.sin(i * (0.052 + seededRange(seed, 0.0, 0.01, 960))) * 15.0
                    + Math.cos(i * (0.017 + seededRange(seed, 0.0, 0.01, 961))) * 6.0;
            double drift = i < 130 ? (i * 0.04) : (5.2 + (i - 130) * 0.085);
            double y = baseline + drift;
            if (i == 78 || i == 156 || i == 214) {
                y += 14.0;
            }
            if (i == 187) {
                y -= 12.0;
            }
            double band = 2.0 + Math.abs(Math.sin(i * 0.09)) * 1.6;
            model.setPoint(x, y, y - band, y + band, Math.abs(y) * 0.4 + 5.0, null);
        }
        return model;
    }

    private static DefaultChartModel forensicModel() {
        DefaultChartModel model = new DefaultChartModel("Forensic Timeline");
        int seed = RENDERER_SEED.get();
        int points = 220;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * (0.041 + seededRange(seed, 0.0, 0.01, 970))) * 11.0
                    + Math.cos(i * (0.015 + seededRange(seed, 0.0, 0.01, 971))) * 4.5;
            if (i > 62 && i < 78) {
                y += 9.5;
            }
            if (i > 148 && i < 166) {
                y -= 8.8;
            }
            if (i == 98 || i == 176) {
                y += 12.0;
            }
            double band = 1.6 + Math.abs(Math.cos(i * 0.1)) * 1.2;
            model.setPoint(x, y, y - band, y + band, Math.abs(y) * 0.35 + 4.0, null);
        }
        return model;
    }

    private static DefaultChartModel securityVoxelModel() {
        DefaultChartModel model = new DefaultChartModel("Security Voxel Cloud");
        int seed = RENDERER_SEED.get();
        int grid = 12;
        int idx = 0;
        for (int ix = 0; ix < grid; ix++) {
            double x = -0.9 + (1.8 * ix / (grid - 1.0));
            for (int iy = 0; iy < grid; iy++) {
                double y = -0.9 + (1.8 * iy / (grid - 1.0));
                double z = Math.sin((ix + seededRange(seed, -0.5, 0.5, 980)) * 0.52)
                        * Math.cos((iy + seededRange(seed, -0.5, 0.5, 981)) * 0.48) * 0.55;
                double centerBoost = Math.exp(-(x * x + y * y) * 1.8);
                double ridge = Math.exp(-((x - 0.45) * (x - 0.45) + (y + 0.25) * (y + 0.25)) * 5.5);
                double intensity = Math.max(0.0, Math.min(1.0, 0.25 + centerBoost * 0.45 + ridge * 0.55));
                // max value is used by VoxelCloudRenderer as anomaly intensity in [0..1]
                model.setPoint(x, y, z - 0.04, intensity, z, "V" + idx++);
            }
        }
        return model;
    }

    private static DefaultChartModel commonUtilityModel() {
        DefaultChartModel model = new DefaultChartModel("Utility Overlay");
        int seed = RENDERER_SEED.get();
        int points = 220;
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * (0.058 + seededRange(seed, 0.0, 0.01, 990))) * 14.0
                    + Math.cos(i * (0.021 + seededRange(seed, 0.0, 0.01, 991))) * 5.0
                    + (i * 0.03);
            double band = 1.8 + Math.abs(Math.sin(i * 0.09)) * 1.2;
            model.setPoint(x, y, y - band, y + band, Math.abs(y) * 0.35 + 4.0, null);
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

    private static DefaultMatrixChartModel chordFlowMatrixModel() {
        int seed = RENDERER_SEED.get();
        List<String> labels = List.of("Ingest", "Validate", "Score", "Route", "Execute", "Archive");
        int n = labels.size();
        double[][] matrix = new double[n][n];

        matrix[0][1] = 84 + seededRange(seed, -6.0, 6.0, 330);
        matrix[1][2] = 78 + seededRange(seed, -6.0, 6.0, 331);
        matrix[2][3] = 69 + seededRange(seed, -6.0, 6.0, 332);
        matrix[3][4] = 74 + seededRange(seed, -6.0, 6.0, 333);
        matrix[4][5] = 66 + seededRange(seed, -6.0, 6.0, 334);
        matrix[2][5] = 14 + seededRange(seed, -3.0, 3.0, 335);
        matrix[1][5] = 10 + seededRange(seed, -3.0, 3.0, 336);
        matrix[3][1] = 9 + seededRange(seed, -2.0, 2.0, 337);
        matrix[4][2] = 7 + seededRange(seed, -2.0, 2.0, 338);
        matrix[5][0] = 6 + seededRange(seed, -2.0, 2.0, 339);

        return new DefaultMatrixChartModel(matrix, labels).setName("Transaction Flow Chords");
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
        model.setPoint(0, 30 + seededInt(seed, 0, 8, 172), 0, 0, 1, "Platform/Data/Ingest/Kafka");
        model.setPoint(1, 18 + seededInt(seed, 0, 6, 173), 0, 0, 1, "Platform/Data/Ingest/API");
        model.setPoint(2, 14 + seededInt(seed, 0, 5, 174), 0, 0, 1, "Platform/Data/Transform/Normalization");
        model.setPoint(3, 12 + seededInt(seed, 0, 5, 175), 0, 0, 1, "Platform/Data/Transform/Enrichment");
        model.setPoint(4, 16 + seededInt(seed, 0, 6, 176), 0, 0, 1, "Platform/Compute/Realtime/Stream");
        model.setPoint(5, 11 + seededInt(seed, 0, 4, 177), 0, 0, 1, "Platform/Compute/Realtime/Rules");
        model.setPoint(6, 9 + seededInt(seed, 0, 4, 178), 0, 0, 1, "Platform/Compute/Batch/ETL");
        model.setPoint(7, 8 + seededInt(seed, 0, 3, 179), 0, 0, 1, "Platform/Compute/Batch/Backfill");
        model.setPoint(8, 10 + seededInt(seed, 0, 3, 180), 0, 0, 1, "Platform/Storage/Hot/Redis");
        model.setPoint(9, 7 + seededInt(seed, 0, 3, 181), 0, 0, 1, "Platform/Storage/Hot/Elastic");
        model.setPoint(10, 12 + seededInt(seed, 0, 3, 182), 0, 0, 1, "Platform/Storage/Cold/ObjectStore");
        model.setPoint(11, 6 + seededInt(seed, 0, 2, 183), 0, 0, 1, "Platform/Storage/Cold/Archive");
        model.setPoint(12, 9 + seededInt(seed, 0, 3, 184), 0, 0, 1, "Platform/UX/Portal/Analytics");
        model.setPoint(13, 7 + seededInt(seed, 0, 2, 185), 0, 0, 1, "Platform/UX/Portal/Operations");
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
