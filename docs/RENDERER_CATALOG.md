# Renderer Catalog (2.5.0)

The [demo catalog](../arbercharts-demo/src/main/resources/data/renderer-catalog.txt)
contains **156 entries** across the categories below, including overlays and demo
adapters. This inventory describes the current source; supported model contracts
and host integration still depend on the selected renderer.

`SunburstRenderer` has separate circular and specialized implementations.
The specialized `CandlestickHollowRenderer` is a deprecated compatibility class;
use its financial counterpart for new integrations. The only `VectorFieldRenderer`
is in the analysis package.

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

- [AreaRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/AreaRenderer.java)
- [BarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/BarRenderer.java)
- [BaselineAreaRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/BaselineAreaRenderer.java)
- [BubbleRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/BubbleRenderer.java)
- [ColumnRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/ColumnRenderer.java)
- [GroupedBarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/GroupedBarRenderer.java)
- [ImpulseRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/ImpulseRenderer.java)
- [LineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/LineRenderer.java)
- [LineSpatialRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/LineSpatialRenderer.java)
- [PredictivePathRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/PredictivePathRenderer.java)
- [RangeRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/RangeRenderer.java)
- [ScatterRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/ScatterRenderer.java)
- [SplineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/SplineRenderer.java)
- [StackedAreaRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/StackedAreaRenderer.java)
- [StackedBarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/StackedBarRenderer.java)
- [StepAreaRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/StepAreaRenderer.java)
- [StepRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/standard/StepRenderer.java)

## Financial

Count: **29**

- [ADXRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/ADXRenderer.java)
- [ATRRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/ATRRenderer.java)
- [AuditTrailRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/AuditTrailRenderer.java)
- [BollingerBandsRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/BollingerBandsRenderer.java)
- [Candlestick3DRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/Candlestick3DRenderer.java)
- [CandlestickHollowRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/CandlestickHollowRenderer.java)
- [CandlestickRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/CandlestickRenderer.java)
- [FibonacciRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/FibonacciRenderer.java)
- [FibonacciRetracementRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/FibonacciRetracementRenderer.java)
- [GanttRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/GanttRenderer.java)
- [HeikinAshiRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/HeikinAshiRenderer.java)
- [HighLowRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/HighLowRenderer.java)
- [IchimokuCloudRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/IchimokuCloudRenderer.java)
- [IchimokuRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/IchimokuRenderer.java)
- [KagiRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/KagiRenderer.java)
- [LiquidityHeatmapRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/LiquidityHeatmapRenderer.java)
- [MACDRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/MACDRenderer.java)
- [OBVRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/OBVRenderer.java)
- [ParabolicSARRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/ParabolicSARRenderer.java)
- [PivotPointsRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/PivotPointsRenderer.java)
- [PointAndFigureAdvancedRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/PointAndFigureAdvancedRenderer.java)
- [PointAndFigureRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/PointAndFigureRenderer.java)
- [PredictiveCandleRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/PredictiveCandleRenderer.java)
- [RSIRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/RSIRenderer.java)
- [RenkoRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/RenkoRenderer.java)
- [StochasticRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/StochasticRenderer.java)
- [VolumeProfileRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/VolumeProfileRenderer.java)
- [VolumeRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/VolumeRenderer.java)
- [WaterfallRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/financial/WaterfallRenderer.java)

## Statistical

Count: **17**

- [BandRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/BandRenderer.java)
- [BeeswarmRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/BeeswarmRenderer.java)
- [BoxPlotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/BoxPlotRenderer.java)
- [ConfidenceIntervalRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/ConfidenceIntervalRenderer.java)
- [DotPlotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/DotPlotRenderer.java)
- [ECDFRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/ECDFRenderer.java)
- [ErrorBarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/ErrorBarRenderer.java)
- [HistogramRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/HistogramRenderer.java)
- [KDERenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/KDERenderer.java)
- [LiveDistributionOverlayRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/LiveDistributionOverlayRenderer.java)
- [QQPlotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/QQPlotRenderer.java)
- [QuantileBandRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/QuantileBandRenderer.java)
- [QuantileRegressionRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/QuantileRegressionRenderer.java)
- [RidgeLineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/RidgeLineRenderer.java)
- [RugPlotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/RugPlotRenderer.java)
- [StatisticalErrorBarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/StatisticalErrorBarRenderer.java)
- [ViolinPlotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/statistical/ViolinPlotRenderer.java)

## Analysis

Count: **19**

- [AdaptiveFunctionRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/AdaptiveFunctionRenderer.java)
- [AutocorrelationRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/AutocorrelationRenderer.java)
- [ChangePointRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/ChangePointRenderer.java)
- [EnvelopeRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/EnvelopeRenderer.java)
- [FourierOverlayRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/FourierOverlayRenderer.java)
- [LiveFFTRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/LiveFFTRenderer.java)
- [LoessRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/LoessRenderer.java)
- [MinMaxMarkerRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/MinMaxMarkerRenderer.java)
- [MovingAverageRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/MovingAverageRenderer.java)
- [MovingCorrelationRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/MovingCorrelationRenderer.java)
- [OutlierDetectionRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/OutlierDetectionRenderer.java)
- [PeakDetectionRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/PeakDetectionRenderer.java)
- [PolynomialRegressionRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/PolynomialRegressionRenderer.java)
- [ReferenceLineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/ReferenceLineRenderer.java)
- [RegressionLineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/RegressionLineRenderer.java)
- [SlopeRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/SlopeRenderer.java)
- [ThresholdRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/ThresholdRenderer.java)
- [TrendDecompositionRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/TrendDecompositionRenderer.java)
- [VectorFieldRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/analysis/VectorFieldRenderer.java)

## Medical

Count: **17**

- [CalibrationRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/CalibrationRenderer.java)
- [CapnographyRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/CapnographyRenderer.java)
- [ECGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/ECGRenderer.java)
- [ECGRhythmRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/ECGRhythmRenderer.java)
- [EEGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/EEGRenderer.java)
- [EMGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/EMGRenderer.java)
- [EOGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/EOGRenderer.java)
- [IBPRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/IBPRenderer.java)
- [MedicalSweepRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/MedicalSweepRenderer.java)
- [NIRSRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/NIRSRenderer.java)
- [PPGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/PPGRenderer.java)
- [SpectrogramMedicalRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/SpectrogramMedicalRenderer.java)
- [SpirometryRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/SpirometryRenderer.java)
- [SweepEraseEKGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/SweepEraseEKGRenderer.java)
- [UltrasoundMModeRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/UltrasoundMModeRenderer.java)
- [VCGRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/VCGRenderer.java)
- [VentilatorWaveformRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/medical/VentilatorWaveformRenderer.java)

## Specialized

Count: **36**

- [AlluvialRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/AlluvialRenderer.java)
- [ArcDiagramRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/ArcDiagramRenderer.java)
- [BulletChartRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/BulletChartRenderer.java)
- [CandlestickHollowRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/CandlestickHollowRenderer.java)
- [ChernoffFacesRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/ChernoffFacesRenderer.java)
- [ChordFlowRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/ChordFlowRenderer.java)
- [ControlChartRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/ControlChartRenderer.java)
- [DelaunayRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/DelaunayRenderer.java)
- [DendrogramRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/DendrogramRenderer.java)
- [DependencyWheelRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/DependencyWheelRenderer.java)
- [GanttResourceViewRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/GanttResourceViewRenderer.java)
- [HeatmapContourRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/HeatmapContourRenderer.java)
- [HeatmapRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/HeatmapRenderer.java)
- [HexbinRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/HexbinRenderer.java)
- [HorizonChartRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/HorizonChartRenderer.java)
- [HorizonRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/HorizonRenderer.java)
- [JoyplotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/JoyplotRenderer.java)
- [LollipopRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/LollipopRenderer.java)
- [MarimekkoRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/MarimekkoRenderer.java)
- [NetworkRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/NetworkRenderer.java)
- [ParallelCoordinatesRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/ParallelCoordinatesRenderer.java)
- [ParetoRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/ParetoRenderer.java)
- [RadarGlowRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/RadarGlowRenderer.java)
- [SankeyProRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/SankeyProRenderer.java)
- [SankeyRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/SankeyRenderer.java)
- [SmithChartRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/SmithChartRenderer.java)
- [SparklineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/SparklineRenderer.java)
- [SpectrogramRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/SpectrogramRenderer.java)
- [StreamgraphRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/StreamgraphRenderer.java)
- [SunburstRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/SunburstRenderer.java)
- [TernaryContourRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/TernaryContourRenderer.java)
- [TernaryPhasediagramRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/TernaryPhasediagramRenderer.java)
- [TernaryPlotRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/TernaryPlotRenderer.java)
- [VSWRCircleRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/VSWRCircleRenderer.java)
- [VoronoiRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/VoronoiRenderer.java)
- [WindRoseRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/specialized/WindRoseRenderer.java)

## Circular

Count: **15**

- [ChordDiagramRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/ChordDiagramRenderer.java)
- [CircularLatencyOverlayRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/CircularLatencyOverlayRenderer.java)
- [DonutRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/DonutRenderer.java)
- [GaugeBandsRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/GaugeBandsRenderer.java)
- [GaugeRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/GaugeRenderer.java)
- [NightingaleRoseRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/NightingaleRoseRenderer.java)
- [PieRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/PieRenderer.java)
- [PolarAdvancedRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/PolarAdvancedRenderer.java)
- [PolarLineRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/PolarLineRenderer.java)
- [PolarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/PolarRenderer.java)
- [RadarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/RadarRenderer.java)
- [RadialBarRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/RadialBarRenderer.java)
- [RadialStackedRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/RadialStackedRenderer.java)
- [SemiDonutRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/SemiDonutRenderer.java)
- [SunburstRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/circular/SunburstRenderer.java)

## Predictive

Count: **2**

- [AnomalyGapRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/predictive/AnomalyGapRenderer.java)
- [PredictiveShadowRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/predictive/PredictiveShadowRenderer.java)

## Forensic

Count: **1**

- [PlaybackStatusRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/forensic/PlaybackStatusRenderer.java)

## Security

Count: **1**

- [VoxelCloudRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/security/VoxelCloudRenderer.java)

## Common

Count: **2**

- [PerformanceAuditRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/common/PerformanceAuditRenderer.java)
- [PhysicalScaleRenderer](../arbercharts-core/src/main/java/com/arbergashi/charts/render/common/PhysicalScaleRenderer.java)

## Framework types outside the demo catalog

`AxisRenderer` provides axis rendering. `BaseRenderer`, `AbstractSpatialLayer`
and `AbstractMedicalSweepRenderer` are base classes rather than catalog entries.
`SignalIntegrityLayer` is installed through the chart builder and is not a demo
catalog entry.
