# Renderer Catalog

ArberCharts ships with **139 renderer families**. This catalog lists every renderer by name.
Note: Sunburst appears twice by design. The specialized renderer builds the hierarchy from
path-style labels (e.g., "root/child/grandchild") and weighted values, which is convenient for
flat datasets. The circular renderer targets `HierarchicalChartModel` directly and supports
interactive drill-down with explicit tree nodes. They are different implementations, not
duplicates.

Total renderer classes listed: **139**

## Standard

Count: **15**

- AreaRenderer
- BarRenderer
- BaselineAreaRenderer
- BubbleRenderer
- ColumnRenderer
- GroupedBarRenderer
- ImpulseRenderer
- LineRenderer
- RangeRenderer
- ScatterRenderer
- SplineRenderer
- StackedAreaRenderer
- StackedBarRenderer
- StepAreaRenderer
- StepRenderer

## Financial

Count: **25**

- ADXRenderer
- ATRRenderer
- BollingerBandsRenderer
- CandlestickHollowRenderer
- CandlestickRenderer
- FibonacciRenderer
- FibonacciRetracementRenderer
- GanttRenderer
- HeikinAshiRenderer
- HighLowRenderer
- IchimokuCloudRenderer
- IchimokuRenderer
- KagiRenderer
- MACDRenderer
- OBVRenderer
- ParabolicSARRenderer
- PivotPointsRenderer
- PointAndFigureAdvancedRenderer
- PointAndFigureRenderer
- RSIRenderer
- RenkoRenderer
- StochasticRenderer
- VolumeProfileRenderer
- VolumeRenderer
- WaterfallRenderer

## Statistical

Count: **15**

- BandRenderer
- BeeswarmRenderer
- BoxPlotRenderer
- ConfidenceIntervalRenderer
- DotPlotRenderer
- ECDFRenderer
- ErrorBarRenderer
- HistogramRenderer
- KDERenderer
- QQPlotRenderer
- QuantileRegressionRenderer
- RidgeLineRenderer
- RugPlotRenderer
- StatisticalErrorBarRenderer
- ViolinPlotRenderer

## Analysis

Count: **18**

- AdaptiveFunctionRenderer
- AutocorrelationRenderer
- ChangePointRenderer
- EnvelopeRenderer
- FourierOverlayRenderer
- LiveFFTRenderer
- LoessRenderer
- MinMaxMarkerRenderer
- MovingAverageRenderer
- OutlierDetectionRenderer
- PeakDetectionRenderer
- PolynomialRegressionRenderer
- ReferenceLineRenderer
- RegressionLineRenderer
- SlopeRenderer
- ThresholdRenderer
- TrendDecompositionRenderer
- VectorFieldRenderer

## Medical

Count: **16**

- CapnographyRenderer
- ECGRenderer
- ECGRhythmRenderer
- EEGRenderer
- EMGRenderer
- EOGRenderer
- IBPRenderer
- MedicalSweepRenderer
- NIRSRenderer
- PPGRenderer
- SpectrogramMedicalRenderer
- SpirometryRenderer
- SweepEraseEKGRenderer
- UltrasoundMModeRenderer
- VCGRenderer
- VentilatorWaveformRenderer

## Specialized

Count: **34**

- AlluvialRenderer
- ArcDiagramRenderer
- BulletChartRenderer
- CandlestickHollowRenderer
- ChernoffFacesRenderer
- ChordFlowRenderer
- ControlChartRenderer
- DelaunayRenderer
- DendrogramRenderer
- DependencyWheelRenderer
- GanttResourceViewRenderer
- HeatmapContourRenderer
- HeatmapRenderer
- HexbinRenderer
- HorizonChartRenderer
- HorizonRenderer
- JoyplotRenderer
- LollipopRenderer
- MarimekkoRenderer
- NetworkRenderer
- ParallelCoordinatesRenderer
- ParetoRenderer
- SankeyProRenderer
- SankeyRenderer
- SparklineRenderer
- SpectrogramRenderer
- StreamgraphRenderer
- SunburstRenderer (specialized, path-label model)
- TernaryContourRenderer
- TernaryPhasediagramRenderer
- TernaryPlotRenderer
- VectorFieldRenderer
- VoronoiRenderer
- WindRoseRenderer

## Circular

Count: **14**

- ChordDiagramRenderer
- DonutRenderer
- GaugeBandsRenderer
- GaugeRenderer
- NightingaleRoseRenderer
- PieRenderer
- PolarAdvancedRenderer
- PolarLineRenderer
- PolarRenderer
- RadarRenderer
- RadialBarRenderer
- RadialStackedRenderer
- SemiDonutRenderer
- SunburstRenderer (circular, HierarchicalChartModel)

## General

Count: **1**

- ChartRenderer

## Abstract/Base Renderers

Count: **1**

- AbstractMedicalSweepRenderer
