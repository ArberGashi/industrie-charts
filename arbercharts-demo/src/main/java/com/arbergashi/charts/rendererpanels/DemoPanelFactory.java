package com.arbergashi.charts.rendererpanels;

import com.arbergashi.charts.rendererpanels.standard.*;
import com.arbergashi.charts.rendererpanels.financial.*;
import com.arbergashi.charts.rendererpanels.medical.*;
import com.arbergashi.charts.rendererpanels.statistical.*;
import com.arbergashi.charts.rendererpanels.specialized.*;
import com.arbergashi.charts.rendererpanels.analysis.*;
import com.arbergashi.charts.ui.ArberChartPanel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.ui.grid.DefaultGridLayer;
import com.arbergashi.charts.ui.grid.FinancialGridLayer;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.ui.grid.AnalysisGridLayer;
import com.arbergashi.charts.ui.grid.GridLayer;

/**
 * Factory for creating demo chart panels.
 * Fully decoupled: delegate actual creation to specific providers in sub-packages.
 */
public class DemoPanelFactory {

    public static JPanel createPanel(String title, ChartTheme theme) {
        DemoPanelUtils.configurePresentationDefaults();
        // The demo application passes the current chart theme (derived from the active FlatLaf mode).
        // This keeps the demo free from any extra theme-loader indirection.
        // If missing, fall back to stable framework defaults.
        ChartTheme effectiveTheme = theme != null
                ? theme
                : (isMedicalChart(title)
                    ? ChartThemes.defaultDark()
                    : ChartThemes.defaultLight());

        JPanel panel = switch (title) {
            case "Line Chart" -> LineChartPanelProvider.create();
            case "Bar Chart" -> BarChartPanelProvider.create();
            case "Stacked Bar" -> StackedBarPanelProvider.create();
            case "Grouped Bar" -> BarChartPanelProvider.create();
            case "Area Chart" -> AreaChartPanelProvider.create();
            case "Step Area" -> StepAreaPanelProvider.create();
            case "Baseline Area" -> BaselineAreaPanelProvider.create();
            case "Range Area" -> RangeAreaPanelProvider.create();
            case "Pie Chart" -> PieChartPanelProvider.create();
            case "Donut Chart" -> DonutChartPanelProvider.create();
            case "Semi Donut" -> SemiDonutChartPanelProvider.create();
            case "Polar Chart" -> PolarChartPanelProvider.create();
            case "Polar Line" -> PolarLinePanelProvider.create();
            case "Radar Chart" -> RadarChartPanelProvider.create();
            case "Nightingale Rose" -> NightingaleRosePanelProvider.create();
            case "Radial Bar" -> RadialBarPanelProvider.create();
            case "Radial Stacked" -> RadialStackedPanelProvider.create();
            case "Gauge" -> GaugeChartPanelProvider.create();
            case "Gauge Bands" -> GaugeBandsChartPanelProvider.create();
            case "Scatter Plot" -> ScatterPlotPanelProvider.create();
            case "Bubble Chart" -> BubbleChartPanelProvider.create();
            case "Candlestick" -> CandlestickChartPanelProvider.create();
            case "Hollow Candlestick" -> HollowCandlestickChartPanelProvider.create();
            case "High Low" -> HighLowChartPanelProvider.create();
            case "Heikin Ashi" -> HeikinAshiChartPanelProvider.create();
            case "Renko" -> RenkoChartPanelProvider.create();
            case "Waterfall" -> WaterfallChartPanelProvider.create();
            case "Kagi" -> KagiChartPanelProvider.create();
            case "Point & Figure" -> PointAndFigureChartPanelProvider.create();
            case "Gantt" -> GanttPanelProvider.create();
            case "Volume" -> VolumeChartPanelProvider.create();
            case "MACD" -> MACDChartPanelProvider.create();
            case "Stochastic" -> StochasticChartPanelProvider.create();
            case "ADX" -> ADXChartPanelProvider.create();
            case "ATR" -> ATRChartPanelProvider.create();
            case "Bollinger Bands" -> BollingerBandsChartPanelProvider.create();
            case "Parabolic SAR" -> ParabolicSARChartPanelProvider.create();
            case "Ichimoku" -> IchimokuChartPanelProvider.create();
            case "Fibonacci" -> FibonacciChartPanelProvider.create();
            case "Pivot Points" -> PivotPointsChartPanelProvider.create();
            case "OBV" -> OBVChartPanelProvider.create();
            case "ECG" -> ECGChartPanelProvider.create();
            case "EEG" -> EEGChartPanelProvider.create();
            case "EMG" -> EMGChartPanelProvider.create();
            case "PPG" -> PPGChartPanelProvider.create();
            case "Spirometry" -> SpirometryChartPanelProvider.create();
            case "Capnography" -> CapnographyPanelProvider.create();
            case "NIRS" -> NIRSPanelProvider.create();
            case "Ventilator" -> VentilatorWaveformPanelProvider.create();
            case "IBP" -> IBPPanelProvider.create();
            case "Ultrasound M-Mode" -> UltrasoundMModePanelProvider.create();
            case "VCG" -> VCGPanelProvider.create();
            case "EOG" -> EOGPanelProvider.create();
            case "Medical Sweep" -> MedicalSweepPanelProvider.create();
            case "Sweep EKG" -> SweepEKGPanelProvider.create();
            case "Heart Rate Variability" -> HeartRateVariabilityPanelProvider.create();
            case "Box Plot" -> BoxPlotChartPanelProvider.create();
            case "Violin Plot" -> ViolinPlotPanelProvider.create();
            case "Histogram" -> HistogramChartPanelProvider.create();
            case "KDE" -> KDEPanelProvider.create();
            case "QQ Plot" -> QQPlotPanelProvider.create();
            case "ECDF" -> ECDFPanelProvider.create();
            case "Error Bar" -> ErrorBarPanelProvider.create();
            case "Statistical Error Bar" -> StatisticalErrorBarPanelProvider.create();
            case "Confidence Interval" -> ConfidenceIntervalPanelProvider.create();
            case "Band" -> BandPanelProvider.create();
            case "Ridge Line" -> RidgeLinePanelProvider.create();
            case "Hexbin" -> HexbinPanelProvider.create();
            case "Sunburst" -> SunburstChartPanelProvider.create();
            case "Sankey" -> SankeyChartPanelProvider.create();
            case "Chord Diagram" -> ChordChartPanelProvider.create();
            case "Chernoff Faces" -> ChernoffFacesPanelProvider.create();
            case "Joyplot" -> JoyplotPanelProvider.create();
            case "Lollipop" -> LollipopPanelProvider.create();
            case "Heatmap" -> HeatmapPanelProvider.create();
            case "Streamgraph" -> StreamgraphPanelProvider.create();
            case "Voronoi" -> VoronoiChartPanelProvider.create();
            case "Delaunay" -> DelaunayPanelProvider.create();
            case "Dependency Wheel" -> DependencyWheelPanelProvider.create();
            case "Parallel Coordinates" -> ParallelCoordinatesPanelProvider.create();
            case "Marimekko" -> MarimekkoPanelProvider.create();
            case "Alluvial" -> AlluvialPanelProvider.create();
            case "Wind Rose" -> WindRosePanelProvider.create();
            case "Bullet Chart" -> BulletChartPanelProvider.create();
            case "Network" -> NetworkPanelProvider.create();
            case "Arc Diagram" -> ArcDiagramPanelProvider.create();
            case "Dendrogram" -> DendrogramPanelProvider.create();
            case "Pareto" -> ParetoPanelProvider.create();
            case "Ternary Phase" -> TernaryPhasePanelProvider.create();
            case "Ternary Contour" -> TernaryContourPanelProvider.create();
            case "Gantt Resource" -> GanttResourcePanelProvider.create();
            case "Control Chart" -> ControlChartPanelProvider.create();
            case "Horizon" -> HorizonPanelProvider.create();
            case "Vector Field" -> VectorFieldPanelProvider.create();
            case "FFT" -> FFTPanelProvider.create();
            case "Spectrogram" -> SpectrogramPanelProvider.create();
            case "Regression" -> RegressionPanelProvider.create();
            case "Polynomial Regression" -> PolynomialRegressionPanelProvider.create();
            case "Autocorrelation" -> AutocorrelationPanelProvider.create();
            case "Change Point" -> ChangePointPanelProvider.create();
            case "Outlier Detection" -> OutlierDetectionPanelProvider.create();
            case "Slope" -> SlopeChartPanelProvider.create();
            case "Adaptive Function" -> AdaptiveFunctionPanelProvider.create();
            default -> null;
        };

        if (panel != null) {
            ArberChartPanel chartPanel = null;
            if (panel instanceof ArberChartPanel arberChartPanel) {
                chartPanel = arberChartPanel;
            } else if (panel instanceof ChartHost host) {
                chartPanel = host.getChartPanel();
            }

            boolean showLegend = true;

            // Enforce ONE grid policy: category-based. No random mixing across panels.
            GridLayer gridLayer = resolveGridLayer(title);

            if (chartPanel != null) {
                ArberChartPanel configured = chartPanel.withTheme(effectiveTheme)
                        .withTooltips(true)
                        .withLegend(showLegend)
                        .withAnimations(true)
                        .withGridLayer(gridLayer);
                SwingUtilities.invokeLater(configured::resetZoom);
                chartPanel = configured;
            }
            if (panel instanceof ArberChartPanel && (isStatisticalChart(title) || isSpecializedChart(title) || isAnalysisChart(title))) {
                chartPanel.setMultiColorEnabled(true);
                panel = new ColorTogglePanel(chartPanel);
            }
            if (chartPanel == null) {
                return panel;
            }
            if (isStandardChart(title)) {
                DemoPanelUtils.PresentationMeta meta = standardMeta(title, chartPanel);
                return new DemoPresentationPanel(panel, chartPanel, meta);
            }
            if (isFinancialChart(title)) {
                DemoPanelUtils.PresentationMeta meta = financialMeta(title, chartPanel);
                return new DemoPresentationPanel(panel, chartPanel, meta);
            }
            if (isMedicalChart(title)) {
                DemoPanelUtils.PresentationMeta meta = medicalMeta(title, chartPanel);
                return new DemoPresentationPanel(panel, chartPanel, meta);
            }
            if (isSpecializedChart(title)) {
                DemoPanelUtils.PresentationMeta meta = specializedMeta(title, chartPanel);
                return new DemoPresentationPanel(panel, chartPanel, meta);
            }
            if (isStatisticalChart(title)) {
                DemoPanelUtils.PresentationMeta meta = statisticalMeta(title, chartPanel);
                return new DemoPresentationPanel(panel, chartPanel, meta);
            }
            if (isAnalysisChart(title)) {
                DemoPanelUtils.PresentationMeta meta = analysisMeta(title, chartPanel);
                return new DemoPresentationPanel(panel, chartPanel, meta);
            }
            return panel;
        }
        return createPlaceholderPanel(title).withTheme(effectiveTheme);
    }

    private static GridLayer resolveGridLayer(String title) {
        if (title == null) return new DefaultGridLayer();

        if (isMedicalChart(title)) {
            return new MedicalGridLayer();
        }
        if (isAnalysisChart(title)) {
            return new AnalysisGridLayer();
        }
        if (isFinancialChart(title)) {
            return new FinancialGridLayer();
        }
        return new DefaultGridLayer();
    }

    private static boolean isFinancialChart(String title) {
        return switch (title) {
            case "Candlestick", "Hollow Candlestick", "High Low", "Heikin Ashi", "Renko",
                 "Waterfall", "Kagi", "Point & Figure", "Volume",
                 "MACD", "Stochastic", "ADX", "ATR", "Bollinger Bands", "Parabolic SAR",
                 "Ichimoku", "Fibonacci", "Pivot Points", "OBV" -> true;
            default -> false;
        };
    }

    private static boolean isAnalysisChart(String title) {
        return switch (title) {
            case "Vector Field", "FFT", "Spectrogram", "Regression", "Polynomial Regression",
                 "Autocorrelation", "Change Point", "Outlier Detection", "Slope", "Adaptive Function" -> true;
            default -> false;
        };
    }

    private static boolean isMedicalChart(String title) {
        return switch (title) {
            case "ECG", "EEG", "EMG", "PPG", "Spirometry", "Capnography", "NIRS",
                 "Ventilator", "IBP", "Ultrasound M-Mode", "VCG", "EOG",
                 "Medical Sweep", "Sweep EKG", "Spectrogram", "Heart Rate Variability" -> true;
            default -> false;
        };
    }

    private static boolean isStandardChart(String title) {
        return switch (title) {
            case "Line Chart", "Bar Chart", "Stacked Bar", "Grouped Bar", "Area Chart", "Step Area",
                 "Baseline Area", "Range Area", "Pie Chart", "Donut Chart", "Semi Donut",
                 "Polar Chart", "Polar Line", "Radar Chart", "Nightingale Rose", "Radial Bar",
                 "Radial Stacked", "Gauge", "Gauge Bands", "Scatter Plot", "Bubble Chart" -> true;
            default -> false;
        };
    }

    private static boolean isSpecializedChart(String title) {
        return switch (title) {
            case "Sunburst", "Sankey", "Chord Diagram", "Chernoff Faces", "Joyplot",
                 "Lollipop", "Heatmap", "Streamgraph", "Voronoi", "Delaunay",
                 "Dependency Wheel", "Parallel Coordinates", "Marimekko", "Alluvial",
                 "Wind Rose", "Bullet Chart", "Network", "Arc Diagram", "Dendrogram",
                 "Pareto", "Ternary Phase", "Ternary Contour", "Gantt Resource",
                 "Control Chart", "Horizon" -> true;
            default -> false;
        };
    }

    private static boolean isStatisticalChart(String title) {
        return switch (title) {
            case "Box Plot", "Violin Plot", "Histogram", "KDE", "QQ Plot", "ECDF",
                 "Error Bar", "Statistical Error Bar", "Confidence Interval", "Band",
                 "Ridge Line", "Hexbin" -> true;
            default -> false;
        };
    }

    private static DemoPanelUtils.PresentationMeta standardMeta(String title, ArberChartPanel panel) {
        String subtitle;
        String[] tags;
        String[] metrics;
        switch (title) {
            case "Line Chart" -> {
                subtitle = "Weekly system metrics across three signals.";
                tags = new String[]{"Standard", "Time series", "Multi-layer"};
                metrics = new String[]{"Window: 7d"};
            }
            case "Bar Chart", "Grouped Bar" -> {
                subtitle = "Regional ARR performance split by plan.";
                tags = new String[]{"Categorical", "Comparison"};
                metrics = new String[]{"Regions: 6"};
            }
            case "Stacked Bar" -> {
                subtitle = "Revenue composition across regions.";
                tags = new String[]{"Stacked", "Composition"};
                metrics = new String[]{"Regions: 6"};
            }
            case "Area Chart" -> {
                subtitle = "Energy mix transitions over a decade.";
                tags = new String[]{"Stacked", "Trends"};
                metrics = new String[]{"Horizon: 10y"};
            }
            case "Step Area" -> {
                subtitle = "Stepwise tier transitions over time.";
                tags = new String[]{"Step", "Time series"};
                metrics = new String[]{"Steps: 12"};
            }
            case "Baseline Area" -> {
                subtitle = "Positive and negative deviations around zero.";
                tags = new String[]{"Baseline", "Deviation"};
                metrics = new String[]{"Baseline: 0"};
            }
            case "Range Area" -> {
                subtitle = "Forecast envelope with min and max bounds.";
                tags = new String[]{"Range", "Uncertainty"};
                metrics = new String[]{"Envelope: Min/Max"};
            }
            case "Pie Chart" -> {
                subtitle = "Market share composition at a glance.";
                tags = new String[]{"Composition", "Parts of whole"};
                metrics = new String[]{"Segments: 8"};
            }
            case "Donut Chart" -> {
                subtitle = "Portfolio allocation across categories.";
                tags = new String[]{"Composition"};
                metrics = new String[]{"Segments: 6"};
            }
            case "Semi Donut" -> {
                subtitle = "Goal progress with a single focal metric.";
                tags = new String[]{"Progress"};
                metrics = new String[]{"Goal: 78%"};
            }
            case "Polar Chart" -> {
                subtitle = "Angular distribution with radial encoding.";
                tags = new String[]{"Polar", "Distribution"};
                metrics = new String[]{"Angles: 12"};
            }
            case "Polar Line" -> {
                subtitle = "Directional trend mapped on polar axes.";
                tags = new String[]{"Polar", "Trend"};
                metrics = new String[]{"Angles: 24"};
            }
            case "Radar Chart" -> {
                subtitle = "Multi-metric profile comparison.";
                tags = new String[]{"Profile", "Multi-metric"};
                metrics = new String[]{"Axes: 6"};
            }
            case "Nightingale Rose" -> {
                subtitle = "Seasonal intensity across radial segments.";
                tags = new String[]{"Radial", "Seasonal"};
                metrics = new String[]{"Segments: 12"};
            }
            case "Radial Bar" -> {
                subtitle = "Ranked metrics in a radial layout.";
                tags = new String[]{"Ranking", "Radial"};
                metrics = new String[]{"Ranks: 8"};
            }
            case "Radial Stacked" -> {
                subtitle = "Segmented radial composition.";
                tags = new String[]{"Stacked", "Radial"};
                metrics = new String[]{"Segments: 6"};
            }
            case "Gauge" -> {
                subtitle = "Operational KPI with threshold styling.";
                tags = new String[]{"KPI", "Threshold"};
                metrics = new String[]{"Target: 75"};
            }
            case "Gauge Bands" -> {
                subtitle = "Multi-band gauge with alert zones.";
                tags = new String[]{"KPI", "Bands"};
                metrics = new String[]{"Bands: 4"};
            }
            case "Scatter Plot" -> {
                subtitle = "Correlation map with dense clusters.";
                tags = new String[]{"Correlation", "Distribution"};
                metrics = new String[]{"Points: 300"};
            }
            case "Bubble Chart" -> {
                subtitle = "Three-variable relationship in one view.";
                tags = new String[]{"Multivariate", "Correlation"};
                metrics = new String[]{"Points: 200"};
            }
            default -> {
                subtitle = "Core chart presentation with interactive tooling.";
                tags = new String[]{"Standard", "Interactive"};
                metrics = new String[]{"Focus: Overview"};
            }
        }
        return new DemoPanelUtils.PresentationMeta(title, subtitle, tags, DemoPanelUtils.buildMetrics(panel, metrics));
    }

    private static DemoPanelUtils.PresentationMeta financialMeta(String title, ArberChartPanel panel) {
        String subtitle;
        String[] tags;
        String[] metrics;
        switch (title) {
            case "Candlestick" -> {
                subtitle = "Daily OHLC with volume context.";
                tags = new String[]{"Price", "OHLC", "Trend"};
                metrics = new String[]{"Window: 180d"};
            }
            case "Hollow Candlestick" -> {
                subtitle = "Momentum-focused candle styling.";
                tags = new String[]{"Price", "Momentum"};
                metrics = new String[]{"Window: 180d"};
            }
            case "High Low" -> {
                subtitle = "Range emphasis for volatility scans.";
                tags = new String[]{"Range", "Volatility"};
                metrics = new String[]{"Window: 180d"};
            }
            case "Heikin Ashi" -> {
                subtitle = "Noise-reduced trend depiction.";
                tags = new String[]{"Smoothed", "Trend"};
                metrics = new String[]{"Window: 180d"};
            }
            case "Renko" -> {
                subtitle = "Price action filtering by brick size.";
                tags = new String[]{"Price Action", "Filter"};
                metrics = new String[]{"Brick: 1.5"};
            }
            case "Waterfall" -> {
                subtitle = "Cumulative contribution analysis.";
                tags = new String[]{"Contribution", "PnL"};
                metrics = new String[]{"Steps: 12"};
            }
            case "Kagi" -> {
                subtitle = "Direction-driven price chart.";
                tags = new String[]{"Trend", "Reversal"};
                metrics = new String[]{"Reversal: 1.8"};
            }
            case "Point & Figure" -> {
                subtitle = "Support/resistance via box plots.";
                tags = new String[]{"Support", "Resistance"};
                metrics = new String[]{"Box: 1.0"};
            }
            case "Volume" -> {
                subtitle = "Volume intensity alongside price.";
                tags = new String[]{"Volume", "Flow"};
                metrics = new String[]{"Window: 180d"};
            }
            case "MACD" -> {
                subtitle = "Momentum crossover signals.";
                tags = new String[]{"Momentum", "Signal"};
                metrics = new String[]{"Fast/Slow: 12/26"};
            }
            case "Stochastic" -> {
                subtitle = "Overbought/oversold oscillator.";
                tags = new String[]{"Oscillator"};
                metrics = new String[]{"K/D: 14/3"};
            }
            case "ADX" -> {
                subtitle = "Trend strength indicator.";
                tags = new String[]{"Trend Strength"};
                metrics = new String[]{"Period: 14"};
            }
            case "ATR" -> {
                subtitle = "Volatility regime tracking.";
                tags = new String[]{"Volatility"};
                metrics = new String[]{"Period: 14"};
            }
            case "Bollinger Bands" -> {
                subtitle = "Volatility bands around price.";
                tags = new String[]{"Bands", "Volatility"};
                metrics = new String[]{"Window: 20"};
            }
            case "Parabolic SAR" -> {
                subtitle = "Stop-and-reverse tracking.";
                tags = new String[]{"Stop", "Trend"};
                metrics = new String[]{"Step: 0.02"};
            }
            case "Ichimoku" -> {
                subtitle = "Multi-layer trend cloud.";
                tags = new String[]{"Cloud", "Trend"};
                metrics = new String[]{"Cloud: 9/26/52"};
            }
            case "Fibonacci" -> {
                subtitle = "Retracement levels for entries.";
                tags = new String[]{"Levels", "Retracement"};
                metrics = new String[]{"Levels: 6"};
            }
            case "Pivot Points" -> {
                subtitle = "Support/resistance pivots.";
                tags = new String[]{"Levels", "Support"};
                metrics = new String[]{"Window: 10"};
            }
            case "OBV" -> {
                subtitle = "Volume accumulation signal.";
                tags = new String[]{"Volume", "Signal"};
                metrics = new String[]{"Flow: Volume"};
            }
            default -> {
                subtitle = "Professional financial charting view.";
                tags = new String[]{"Financial", "Interactive"};
                metrics = new String[]{"Focus: Trend"};
            }
        }
        return new DemoPanelUtils.PresentationMeta(title, subtitle, tags, DemoPanelUtils.buildMetrics(panel, metrics));
    }

    private static DemoPanelUtils.PresentationMeta medicalMeta(String title, ArberChartPanel panel) {
        String subtitle;
        String[] tags;
        String[] metrics;
        switch (title) {
            case "ECG" -> {
                subtitle = "Lead II rhythm with calibrated grid.";
                tags = new String[]{"Cardiac", "Waveform"};
                metrics = new String[]{"Rate: 72 bpm", "Sample: 250 Hz", "Buffer: 3000"};
            }
            case "EEG" -> {
                subtitle = "Alpha/beta mix across cortical leads.";
                tags = new String[]{"Neural", "Multi-channel"};
                metrics = new String[]{"Channels: 8", "Sample: 250 Hz", "Window: 2 s"};
            }
            case "EMG" -> {
                subtitle = "Surface muscle activation bursts.";
                tags = new String[]{"Muscle", "Waveform"};
                metrics = new String[]{"Sample: 1000 Hz", "Mode: Live"};
            }
            case "PPG" -> {
                subtitle = "Peripheral perfusion waveform.";
                tags = new String[]{"Pulse", "Oximetry"};
                metrics = new String[]{"Sample: 125 Hz", "Buffer: 1800"};
            }
            case "Spirometry" -> {
                subtitle = "Forced expiratory flow loop.";
                tags = new String[]{"Respiratory", "Loop"};
                metrics = new String[]{"FVC: 4.5 L", "PEF: 9.5 L/s"};
            }
            case "Capnography" -> {
                subtitle = "EtCO2 waveform with respiratory phases.";
                tags = new String[]{"Respiratory", "EtCO2"};
                metrics = new String[]{"Rate: 14 bpm", "Sample: 50 Hz", "Buffer: 900"};
            }
            case "NIRS" -> {
                subtitle = "Cerebral oxygenation trend.";
                tags = new String[]{"Oxygenation", "Trend"};
                metrics = new String[]{"Baseline: 68%", "Range: 55-80%", "Sample: 10 Hz", "Buffer: 300"};
            }
            case "Ventilator" -> {
                subtitle = "Pressure, flow, and volume curves.";
                tags = new String[]{"Ventilation", "Multi-channel"};
                metrics = new String[]{"Channels: 3", "Rate: 14 bpm", "Sample: 50 Hz"};
            }
            case "IBP" -> {
                subtitle = "Arterial pressure with dicrotic notch.";
                tags = new String[]{"Blood Pressure", "Waveform"};
                metrics = new String[]{"Base: 120/80", "Sample: 125 Hz", "Buffer: 1500"};
            }
            case "Ultrasound M-Mode" -> {
                subtitle = "M-mode wall motion trace.";
                tags = new String[]{"Ultrasound", "M-Mode"};
                metrics = new String[]{"Sample: 50 Hz", "Frames: 480"};
            }
            case "VCG" -> {
                subtitle = "Vector loop in frontal plane.";
                tags = new String[]{"Cardiac", "Vector"};
                metrics = new String[]{"Loop: 480 pts"};
            }
            case "EOG" -> {
                subtitle = "Saccades, pursuits, blinks.";
                tags = new String[]{"Ocular", "Waveform"};
                metrics = new String[]{"Sample: 100 Hz", "Buffer: 1000"};
            }
            case "Medical Sweep" -> {
                subtitle = "Sweep-erase bedside monitor.";
                tags = new String[]{"Live", "Sweep"};
                metrics = new String[]{"Sample: 125 Hz", "Buffer: 800"};
            }
            case "Sweep EKG" -> {
                subtitle = "Sweep ECG with classic phosphor feel.";
                tags = new String[]{"Cardiac", "Sweep"};
                metrics = new String[]{"Sample: 250 Hz", "Buffer: 1000"};
            }
            case "Spectrogram" -> {
                subtitle = "Frequency intensity over time.";
                tags = new String[]{"Spectrogram", "Frequency"};
                metrics = new String[]{"Bands: 256"};
            }
            case "Heart Rate Variability" -> {
                subtitle = "RR interval tachogram.";
                tags = new String[]{"HRV", "Analysis"};
                metrics = new String[]{"Beats: 300", "Mean: 833 ms"};
            }
            default -> {
                subtitle = "Clinical-grade waveform visualization.";
                tags = new String[]{"Medical", "Interactive"};
                metrics = new String[]{"Mode: Live"};
            }
        }
        return new DemoPanelUtils.PresentationMeta(title, subtitle, tags, DemoPanelUtils.buildMetrics(panel, metrics));
    }

    private static DemoPanelUtils.PresentationMeta specializedMeta(String title, ArberChartPanel panel) {
        String subtitle;
        String[] tags;
        String[] metrics;
        switch (title) {
            case "Sunburst" -> {
                subtitle = "Hierarchical contributions across depth rings.";
                tags = new String[]{"Hierarchy", "Radial"};
                metrics = new String[]{"Focus: Hierarchy"};
            }
            case "Sankey" -> {
                subtitle = "Flow magnitude across stages.";
                tags = new String[]{"Flow", "Network"};
                metrics = new String[]{"Focus: Flow"};
            }
            case "Chord Diagram" -> {
                subtitle = "Bidirectional relationships in a circle.";
                tags = new String[]{"Network", "Relationship"};
                metrics = new String[]{"Focus: Links"};
            }
            case "Chernoff Faces" -> {
                subtitle = "Multivariate data mapped to faces.";
                tags = new String[]{"Multivariate", "Glyph"};
                metrics = new String[]{"Focus: Glyphs"};
            }
            case "Joyplot" -> {
                subtitle = "Stacked density ridges.";
                tags = new String[]{"Density", "Stacked"};
                metrics = new String[]{"Layers: 8"};
            }
            case "Lollipop" -> {
                subtitle = "Ranked values with emphasis on endpoints.";
                tags = new String[]{"Ranking", "Comparison"};
                metrics = new String[]{"Ranks: 12"};
            }
            case "Heatmap" -> {
                subtitle = "Intensity surface across two axes.";
                tags = new String[]{"Intensity", "Matrix"};
                metrics = new String[]{"Grid: 40x20"};
            }
            case "Streamgraph" -> {
                subtitle = "Layered flows over time.";
                tags = new String[]{"Stream", "Time series"};
                metrics = new String[]{"Layers: 6"};
            }
            case "Voronoi" -> {
                subtitle = "Spatial influence regions.";
                tags = new String[]{"Spatial", "Partition"};
                metrics = new String[]{"Cells: 80"};
            }
            case "Delaunay" -> {
                subtitle = "Triangulated spatial structure.";
                tags = new String[]{"Spatial", "Mesh"};
                metrics = new String[]{"Points: 80"};
            }
            case "Dependency Wheel" -> {
                subtitle = "Circular dependency matrix.";
                tags = new String[]{"Dependency", "Chord"};
                metrics = new String[]{"Focus: Dependencies"};
            }
            case "Parallel Coordinates" -> {
                subtitle = "High-dimensional comparisons.";
                tags = new String[]{"Multivariate", "Comparison"};
                metrics = new String[]{"Axes: 6"};
            }
            case "Marimekko" -> {
                subtitle = "Two-dimensional market composition.";
                tags = new String[]{"Composition", "Proportional"};
                metrics = new String[]{"Segments: 6"};
            }
            case "Alluvial" -> {
                subtitle = "Category flow across stages.";
                tags = new String[]{"Flow", "Composition"};
                metrics = new String[]{"Stages: 4"};
            }
            case "Wind Rose" -> {
                subtitle = "Directional distribution intensity.";
                tags = new String[]{"Directional", "Distribution"};
                metrics = new String[]{"Directions: 16"};
            }
            case "Bullet Chart" -> {
                subtitle = "KPI vs target and ranges.";
                tags = new String[]{"KPI", "Benchmark"};
                metrics = new String[]{"Target: 85"};
            }
            case "Network" -> {
                subtitle = "Node-link relationship layout.";
                tags = new String[]{"Network", "Topology"};
                metrics = new String[]{"Nodes: 24"};
            }
            case "Arc Diagram" -> {
                subtitle = "Connections along a single axis.";
                tags = new String[]{"Connectivity", "Linear"};
                metrics = new String[]{"Links: 20"};
            }
            case "Dendrogram" -> {
                subtitle = "Hierarchical clustering tree.";
                tags = new String[]{"Hierarchy", "Tree"};
                metrics = new String[]{"Levels: 5"};
            }
            case "Pareto" -> {
                subtitle = "Cumulative contribution curve.";
                tags = new String[]{"Pareto", "Cumulative"};
                metrics = new String[]{"Top: 20%"};
            }
            case "Ternary Phase" -> {
                subtitle = "Three-component mixture plot.";
                tags = new String[]{"Ternary", "Mixture"};
                metrics = new String[]{"Samples: 120"};
            }
            case "Ternary Contour" -> {
                subtitle = "Contour intensity on ternary axes.";
                tags = new String[]{"Ternary", "Contour"};
                metrics = new String[]{"Bands: 12"};
            }
            case "Gantt Resource" -> {
                subtitle = "Resource allocation timeline.";
                tags = new String[]{"Planning", "Resources"};
                metrics = new String[]{"Horizon: 12w"};
            }
            case "Control Chart" -> {
                subtitle = "Process stability with limits.";
                tags = new String[]{"SPC", "Quality"};
                metrics = new String[]{"Window: 250"};
            }
            case "Horizon" -> {
                subtitle = "High-density time series bands.";
                tags = new String[]{"Dense", "Time series"};
                metrics = new String[]{"Bands: 4"};
            }
            default -> {
                subtitle = "Specialized visualization showcase.";
                tags = new String[]{"Specialized", "Interactive"};
                metrics = new String[]{"Focus: Specialty"};
            }
        }
        return new DemoPanelUtils.PresentationMeta(title, subtitle, tags, DemoPanelUtils.buildMetrics(panel, metrics));
    }

    private static DemoPanelUtils.PresentationMeta statisticalMeta(String title, ArberChartPanel panel) {
        String subtitle;
        String[] tags;
        String[] metrics;
        switch (title) {
            case "Box Plot" -> {
                subtitle = "Distribution with quartiles and outliers.";
                tags = new String[]{"Distribution", "Outliers"};
                metrics = new String[]{"Groups: 4"};
            }
            case "Violin Plot" -> {
                subtitle = "Density with distribution shape.";
                tags = new String[]{"Density", "Distribution"};
                metrics = new String[]{"Groups: 4"};
            }
            case "Histogram" -> {
                subtitle = "Binned frequency view.";
                tags = new String[]{"Frequency", "Binning"};
                metrics = new String[]{"Bins: 20"};
            }
            case "KDE" -> {
                subtitle = "Kernel density estimate.";
                tags = new String[]{"Density", "Smooth"};
                metrics = new String[]{"Kernel: Gaussian"};
            }
            case "QQ Plot" -> {
                subtitle = "Quantile comparison vs normal.";
                tags = new String[]{"Quantiles", "Diagnostics"};
                metrics = new String[]{"Quantiles: 100"};
            }
            case "ECDF" -> {
                subtitle = "Cumulative distribution curve.";
                tags = new String[]{"Cumulative", "Distribution"};
                metrics = new String[]{"Samples: 500"};
            }
            case "Error Bar" -> {
                subtitle = "Mean with variability bounds.";
                tags = new String[]{"Uncertainty", "Intervals"};
                metrics = new String[]{"Groups: 6"};
            }
            case "Statistical Error Bar" -> {
                subtitle = "Confidence intervals across groups.";
                tags = new String[]{"Intervals", "Comparison"};
                metrics = new String[]{"Groups: 6"};
            }
            case "Confidence Interval" -> {
                subtitle = "Interval bands around estimates.";
                tags = new String[]{"Confidence", "Uncertainty"};
                metrics = new String[]{"Level: 95%"};
            }
            case "Band" -> {
                subtitle = "Upper/lower envelope bands.";
                tags = new String[]{"Envelope", "Range"};
                metrics = new String[]{"Bands: 2"};
            }
            case "Ridge Line" -> {
                subtitle = "Stacked distributions over index.";
                tags = new String[]{"Ridge", "Distribution"};
                metrics = new String[]{"Layers: 8"};
            }
            case "Hexbin" -> {
                subtitle = "Density in hexagonal bins.";
                tags = new String[]{"Density", "2D"};
                metrics = new String[]{"Bins: 40"};
            }
            default -> {
                subtitle = "Statistical insight at a glance.";
                tags = new String[]{"Statistical", "Insight"};
                metrics = new String[]{"Focus: Distribution"};
            }
        }
        return new DemoPanelUtils.PresentationMeta(title, subtitle, tags, DemoPanelUtils.buildMetrics(panel, metrics));
    }

    private static DemoPanelUtils.PresentationMeta analysisMeta(String title, ArberChartPanel panel) {
        String subtitle;
        String[] tags;
        String[] metrics;
        switch (title) {
            case "Vector Field" -> {
                subtitle = "Directional flow field with magnitude.";
                tags = new String[]{"Vector", "Field"};
                metrics = new String[]{"Grid: 20x20"};
            }
            case "FFT" -> {
                subtitle = "Frequency spectrum analysis.";
                tags = new String[]{"Frequency", "Spectrum"};
                metrics = new String[]{"Window: 2048"};
            }
            case "Spectrogram" -> {
                subtitle = "Time-frequency intensity map.";
                tags = new String[]{"Spectrogram", "Frequency"};
                metrics = new String[]{"Window: 256"};
            }
            case "Regression" -> {
                subtitle = "Trend fit with residual context.";
                tags = new String[]{"Regression", "Trend"};
                metrics = new String[]{"Fit: Linear"};
            }
            case "Polynomial Regression" -> {
                subtitle = "Curved fit for non-linear trends.";
                tags = new String[]{"Regression", "Non-linear"};
                metrics = new String[]{"Order: 3"};
            }
            case "Autocorrelation" -> {
                subtitle = "Lag correlation diagnostics.";
                tags = new String[]{"Correlation", "Lag"};
                metrics = new String[]{"Lags: 40"};
            }
            case "Change Point" -> {
                subtitle = "Detected regime shifts.";
                tags = new String[]{"Change", "Regime"};
                metrics = new String[]{"Detect: Window"};
            }
            case "Outlier Detection" -> {
                subtitle = "Anomaly flags over baseline.";
                tags = new String[]{"Anomaly", "Detection"};
                metrics = new String[]{"Threshold: 2.5"};
            }
            case "Slope" -> {
                subtitle = "Directional slope magnitude.";
                tags = new String[]{"Slope", "Direction"};
                metrics = new String[]{"Window: 24"};
            }
            case "Adaptive Function" -> {
                subtitle = "Adaptive response curve.";
                tags = new String[]{"Adaptive", "Signal"};
                metrics = new String[]{"Response: Live"};
            }
            default -> {
                subtitle = "Analytical insights and diagnostics.";
                tags = new String[]{"Analysis", "Insights"};
                metrics = new String[]{"Focus: Diagnostics"};
            }
        }
        return new DemoPanelUtils.PresentationMeta(title, subtitle, tags, DemoPanelUtils.buildMetrics(panel, metrics));
    }

    private static boolean shouldShowLegend(String title) {
        // Keep the default demo look clean. Show legend only where it adds value.
        return switch (title) {
            // Multi-series statistical
            case "Box Plot", "Violin Plot", "Error Bar", "Statistical Error Bar", "Confidence Interval", "Band", "Ridge Line",
                 "QQ Plot", "ECDF", "Histogram", "KDE" -> true;

            // Financial indicators
            case "MACD", "Stochastic", "ADX", "ATR", "Bollinger Bands", "Parabolic SAR", "Ichimoku", "Pivot Points", "OBV" -> true;

            // Medical multi-waveforms
            case "Ventilator", "Medical Sweep", "Sweep EKG", "Heart Rate Variability", "EEG", "ECG" -> true;

            // Analysis outputs
            case "Spectrogram", "FFT", "Autocorrelation", "Regression", "Polynomial Regression", "Change Point", "Outlier Detection", "Slope" -> true;

            default -> false;
        };
    }

    private static ArberChartPanel createPlaceholderPanel(String title) {
        DefaultChartModel model = new DefaultChartModel("Placeholder: " + title);
        model.addPoint(0, 0, 0, "");
        return ArberChartBuilder.create()
                .withTitle("Demo not yet implemented: " + title)
                .addLayer(model, new LineRenderer())
                .build();
    }

    private static final class ColorTogglePanel extends JPanel implements ChartHost {
        private final ArberChartPanel chartPanel;

        private ColorTogglePanel(ArberChartPanel chartPanel) {
            super(new java.awt.BorderLayout());
            this.chartPanel = chartPanel;

            javax.swing.JCheckBox toggle = new javax.swing.JCheckBox("Multi-color");
            toggle.setSelected(true);
            toggle.setOpaque(false);
            toggle.addActionListener(e -> chartPanel.setMultiColorEnabled(toggle.isSelected()));

            javax.swing.JPanel controls = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 6));
            controls.setOpaque(false);
            controls.add(toggle);

            add(controls, java.awt.BorderLayout.NORTH);
            add(chartPanel, java.awt.BorderLayout.CENTER);
        }

        @Override
        public ArberChartPanel getChartPanel() {
            return chartPanel;
        }
    }
}
