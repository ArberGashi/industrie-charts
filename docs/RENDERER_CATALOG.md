# Renderer Catalog (1.7.0-LTS)

ArberCharts ships with **158 concrete renderer classes** across standard, financial, statistical,
analysis, specialized, medical, and infrastructure overlays.

Note on duplicate class names:
- `SunburstRenderer` exists in `render.circular` and `render.specialized` (different models).
- `CandlestickHollowRenderer` exists in `render.financial` and `render.specialized`.
- `VectorFieldRenderer` exists in `render.analysis` and `render.specialized`.

## RendererAffinity Matrix (spatial and transform capability)

Use `RendererRegistry.getRendererCapabilities(id)` for runtime detection. The table below lists
known spatial-capable renderer classes and their affinity traits.

| Renderer Class | Spatial Batch | Coordinate Transform Provider | Notes |
| --- | --- | --- | --- |
| LineSpatialRenderer | Yes | No | Spatial line path renderer |
| Candlestick3DRenderer | Yes | No | 3D financial layer |
| VoxelCloudRenderer | Yes | No | Voxelized security layer |
| TernaryPlotRenderer | Yes | No | Spatial ternary mapping |
| SmithChartRenderer | Yes | Yes | Uses Smith transform |

Any renderer extending `AbstractSpatialLayer` or implementing `SpatialChunkRenderer` is
spatial-capable by contract.

## Standard

Count: **17**

- AreaRenderer
- BarRenderer
- BaselineAreaRenderer
- BubbleRenderer
- ColumnRenderer
- GroupedBarRenderer
- ImpulseRenderer
- LineRenderer
- LineSpatialRenderer
- PredictivePathRenderer
- RangeRenderer
- ScatterRenderer
- SplineRenderer
- StackedAreaRenderer
- StackedBarRenderer
- StepAreaRenderer
- StepRenderer

## Financial

Count: **29**

- ADXRenderer
- ATRRenderer
- AuditTrailRenderer
- BollingerBandsRenderer
- Candlestick3DRenderer
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
- LiquidityHeatmapRenderer
- MACDRenderer
- OBVRenderer
- ParabolicSARRenderer
- PivotPointsRenderer
- PointAndFigureAdvancedRenderer
- PointAndFigureRenderer
- PredictiveCandleRenderer
- RSIRenderer
- RenkoRenderer
- StochasticRenderer
- VolumeProfileRenderer
- VolumeRenderer
- WaterfallRenderer

## Statistical

Count: **17**

- BandRenderer
- BeeswarmRenderer
- BoxPlotRenderer
- ConfidenceIntervalRenderer
- DotPlotRenderer
- ECDFRenderer
- ErrorBarRenderer
- HistogramRenderer
- KDERenderer
- LiveDistributionOverlayRenderer
- QQPlotRenderer
- QuantileBandRenderer
- QuantileRegressionRenderer
- RidgeLineRenderer
- RugPlotRenderer
- StatisticalErrorBarRenderer
- ViolinPlotRenderer

## Analysis

Count: **19**

- AdaptiveFunctionRenderer
- AutocorrelationRenderer
- ChangePointRenderer
- EnvelopeRenderer
- FourierOverlayRenderer
- LiveFFTRenderer
- LoessRenderer
- MinMaxMarkerRenderer
- MovingAverageRenderer
- MovingCorrelationRenderer
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

Count: **17**

- CalibrationRenderer
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

Count: **37**

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
- RadarGlowRenderer
- SankeyProRenderer
- SankeyRenderer
- SmithChartRenderer
- SparklineRenderer
- SpectrogramRenderer
- StreamgraphRenderer
- SunburstRenderer
- TernaryContourRenderer
- TernaryPhasediagramRenderer
- TernaryPlotRenderer
- VSWRCircleRenderer
- VectorFieldRenderer
- VoronoiRenderer
- WindRoseRenderer

## Circular

Count: **15**

- ChordDiagramRenderer
- CircularLatencyOverlayRenderer
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
- SunburstRenderer

## Predictive

Count: **2**

- AnomalyGapRenderer
- PredictiveShadowRenderer

## Forensic

Count: **1**

- PlaybackStatusRenderer

## Security

Count: **1**

- VoxelCloudRenderer

## Common (Overlays and Infrastructure)

Count: **2**

- PerformanceAuditRenderer
- PhysicalScaleRenderer

## Framework / Base

Count: **1**

- AxisRenderer

## Abstract/Base Renderers

Count: **2**

- AbstractMedicalSweepRenderer
- BaseRenderer
