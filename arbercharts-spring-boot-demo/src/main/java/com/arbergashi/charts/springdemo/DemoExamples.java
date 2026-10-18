package com.arbergashi.charts.springdemo;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.model.DefaultStatisticalChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.circular.DonutRenderer;
import com.arbergashi.charts.render.circular.RadarRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.render.standard.AreaRenderer;
import com.arbergashi.charts.render.standard.BarRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.render.standard.ScatterRenderer;
import com.arbergashi.charts.render.standard.StepRenderer;
import com.arbergashi.charts.render.statistical.BoxPlotRenderer;
import com.arbergashi.charts.render.statistical.KDERenderer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/** Deterministic, synthetic examples with a separate model and renderer per request. */
final class DemoExamples {
    private static final List<Example> EXAMPLES = List.of(
            new Example("line", "Line", "Time series", "Synthetic sensor signal",
                    "A smooth signal with a slow trend and periodic variation.", "Time (s)", "Signal",
                    256, "Samples", Presentation.CARTESIAN, 0, 60, 20, 100, LineRenderer::new),
            new Example("area", "Area", "Time series", "Synthetic capacity profile",
                    "A positive load profile with a fixed zero baseline.", "Time (s)", "Load (%)",
                    256, "Samples", Presentation.CARTESIAN, 0, 60, 0, 100, AreaRenderer::new),
            new Example("step", "Step", "Time series", "Synthetic controller output",
                    "Discrete control levels sampled over one minute.", "Time (s)", "Level (%)",
                    96, "Samples", Presentation.CARTESIAN, 0, 60, 0, 100, StepRenderer::new),
            new Example("bar", "Bar", "Comparison", "Synthetic production batches",
                    "Batch output across a fixed sequence of samples.", "Sample", "Output",
                    24, "Samples", Presentation.CARTESIAN, 0, 1, 0, 100, BarRenderer::new),
            new Example("scatter", "Scatter", "Comparison", "Synthetic measurement pairs",
                    "A deterministic cloud of measurements around an increasing trend.", "Input", "Response",
                    256, "Samples", Presentation.CARTESIAN, 0, 100, 0, 100, ScatterRenderer::new),
            new Example("candlestick", "Candlestick", "Financial", "Synthetic OHLC session",
                    "Generated open, high, low and close values; no market data.", "Session", "Price (a.u.)",
                    96, "Samples", Presentation.CARTESIAN, 0, 1, 90, 120, CandlestickRenderer::new),
            new Example("donut", "Donut", "Composition", "Synthetic energy mix",
                    "Five sources share a fixed set of observations. Each sector shows its share of the total.",
                    "Energy source", "Share (%)", 256, "Observations", Presentation.DONUT,
                    0, 1, 0, 100, DonutRenderer::new),
            new Example("radar", "Radar", "Comparison", "Synthetic system profile",
                    "Six normalized scores, each averaged from the selected number of measurements. Higher is better.",
                    "Six metrics", "Score (0-100)", 96, "Samples / metric", Presentation.RADAR,
                    0, 6, 0, 100, RadarRenderer::new),
            new Example("heatmap", "Heatmap", "Scientific", "Synthetic response field",
                    "Two smooth response peaks sampled on a square grid. The cell budget controls spatial resolution.",
                    "Position X (mm)", "Position Y (mm)", 1024, "Cell budget", Presentation.HEATMAP,
                    0, 10, 0, 6, HeatmapRenderer::new),
            new Example("boxplot", "Box plot", "Distribution", "Synthetic batch distributions",
                    "Six batches with median, quartiles and 1.5 IQR whiskers. Separate marks show outliers.",
                    "Batch", "Measurement (a.u.)", 96, "Samples / batch", Presentation.BOXPLOT,
                    0.5, 6.5, 0, 100, BoxPlotRenderer::new),
            new Example("kde", "Kernel density", "Distribution", "Synthetic mixed population",
                    "A Gaussian density estimate of two generated measurement populations, using a 0.18-unit bandwidth.",
                    "Measurement (a.u.)", "Density (1/a.u.)", 256, "Observations", Presentation.CARTESIAN,
                    0, 8, 0, 1, KDERenderer::new)
    );
    private static final String[] ENERGY_SOURCES = {"Solar", "Wind", "Hydro", "Storage", "Other"};
    private static final String[] METRICS = {"Speed", "Quality", "Uptime", "Capacity", "Recovery", "Efficiency"};
    private static final double[] RADAR_SCORES = {84, 93, 78, 67, 81, 72};

    private DemoExamples() {
    }

    static List<ExampleInfo> catalog() {
        return EXAMPLES.stream().map(Example::info).toList();
    }

    static Example find(String id) {
        return EXAMPLES.stream().filter(example -> example.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown chart example."));
    }

    enum Presentation {
        CARTESIAN, DONUT, RADAR, HEATMAP, BOXPLOT
    }

    record ExampleInfo(String id, String name, String category, String dataset, String description,
                       String xLabel, String yLabel, int defaultSamples, String sampleLabel,
                       String presentation, String renderer) {
    }

    record Example(String id, String name, String category, String dataset, String description,
                   String xLabel, String yLabel, int defaultSamples, String sampleLabel, Presentation presentation,
                   double minX, double maxX, double minY, double maxY,
                   Supplier<ChartRenderer> rendererFactory) {

        ExampleInfo info() {
            return new ExampleInfo(id, name, category, dataset, description, xLabel, yLabel,
                    defaultSamples, sampleLabel, presentation.name().toLowerCase(Locale.ROOT),
                    rendererFactory.get().getClass().getSimpleName());
        }

        Dataset create(int samples, ArberColor color) {
            return switch (id) {
                case "candlestick" -> financial(samples);
                case "donut" -> donut(samples, color);
                case "radar" -> radar(samples, color);
                case "heatmap" -> heatmap(samples, color);
                case "boxplot" -> boxplot(samples);
                case "kde" -> density(samples, color);
                default -> cartesian(samples, color);
            };
        }

        private Dataset financial(int samples) {
            DefaultFinancialChartModel model = new DefaultFinancialChartModel(dataset);
            for (int i = 0; i < samples; i++) {
                double t = i * 60.0 / (samples - 1);
                double open = 102 + 5 * Math.sin(t / 7) + t / 9;
                double close = open + 1.8 * Math.sin(i * 1.7 + 0.4);
                double high = Math.max(open, close) + 0.8 + 0.5 * Math.cos(i * 0.7);
                double low = Math.min(open, close) - 0.9 - 0.4 * Math.sin(i * 0.9);
                model.setOHLC(i + 1, open, high, low, close);
            }
            return new Dataset(model, rendererFactory.get(), 0, samples + 1, minY, maxY);
        }

        private Dataset cartesian(int samples, ArberColor color) {
            DefaultChartModel model = new DefaultChartModel(dataset, color);
            for (int i = 0; i < samples; i++) {
                double t = i * 60.0 / (samples - 1);
                double x;
                double y;
                switch (id) {
                    case "bar" -> {
                        x = i + 1;
                        y = 40 + 23 * Math.sin(i * 0.65) + 15 * Math.cos(i * 0.29);
                    }
                    case "scatter" -> {
                        x = 5 + 90 * ((i * 0.61803398875) % 1);
                        y = 15 + 0.64 * x + 12 * Math.sin(i * 2.39) + 5 * Math.cos(i * 0.43);
                    }
                    case "step" -> {
                        x = t;
                        y = 30 + 12 * (Math.floor(t / 8) % 5);
                    }
                    default -> {
                        x = t;
                        y = 52 + 17 * Math.sin(t / 7) + 6 * Math.cos(t / 1.8) + t / 6;
                    }
                }
                model.setPoint(x, y, 1.0, "");
            }
            return new Dataset(model, rendererFactory.get(), minX, id.equals("bar") ? samples + 1 : maxX,
                    minY, maxY);
        }

        private Dataset donut(int samples, ArberColor color) {
            int[] counts = new int[ENERGY_SOURCES.length];
            double[] cumulative = {0.34, 0.60, 0.78, 0.92, 1.0};
            for (int i = 0; i < samples; i++) {
                double fraction = (i + 0.5) / samples;
                int source = 0;
                while (fraction > cumulative[source]) source++;
                counts[source]++;
            }
            DefaultChartModel model = new DefaultChartModel(dataset, color);
            for (int i = 0; i < counts.length; i++) {
                model.setPoint(i, counts[i], counts[i], ENERGY_SOURCES[i]);
            }
            DonutRenderer renderer = new DonutRenderer().setCenterText(Integer.toString(samples))
                    .setCenterSubText("observations");
            return new Dataset(model, renderer, minX, maxX, minY, maxY);
        }

        private Dataset radar(int samples, ArberColor color) {
            DefaultChartModel model = new DefaultChartModel(dataset, color);
            for (int metric = 0; metric < METRICS.length; metric++) {
                double total = 0;
                for (int i = 0; i < samples; i++) {
                    total += RADAR_SCORES[metric] + 4 * Math.sin(i * 0.71 + metric * 1.3)
                            + 2 * Math.cos(i * 0.37 - metric);
                }
                model.setPoint(metric, total / samples, 1, METRICS[metric]);
            }
            return new Dataset(model, rendererFactory.get(), minX, maxX, minY, maxY);
        }

        private Dataset heatmap(int samples, ArberColor color) {
            int side = (int) Math.sqrt(samples);
            double[][] grid = new double[side][side];
            DefaultChartModel model = new DefaultChartModel(dataset, color);
            for (int row = 0; row < side; row++) {
                double y = minY + (row + 0.5) * (maxY - minY) / side;
                for (int col = 0; col < side; col++) {
                    double x = minX + (col + 0.5) * (maxX - minX) / side;
                    double first = Math.exp(-((x - 3.0) * (x - 3.0) / 2.0 + (y - 2.0) * (y - 2.0) / 0.8));
                    double second = Math.exp(-((x - 7.0) * (x - 7.0) / 2.8 + (y - 4.2) * (y - 4.2) / 1.1));
                    grid[row][col] = 5 + 90 * first + 68 * second;
                    model.setPoint(x, y, grid[row][col], "");
                }
            }
            HeatmapRenderer renderer = new HeatmapRenderer().setGridData(grid, minX, maxX, minY, maxY);
            renderer.setMultiColor(true);
            return new Dataset(model, renderer, minX, maxX, minY, maxY);
        }

        private Dataset boxplot(int samples) {
            BoxModel model = new BoxModel(dataset, 6);
            for (int batch = 0; batch < 6; batch++) {
                SplittableRandom random = new SplittableRandom(97L + batch);
                double[] values = new double[samples];
                double center = 31 + batch * 7.2;
                double spread = 3.5 + (batch % 3) * 1.2;
                for (int i = 0; i < samples; i++) values[i] = center + spread * normal(random);
                values[0] = center - 4.5 * spread;
                values[1] = center + 4.5 * spread;
                BoxSummary summary = summarize(values);
                model.setBoxPlot(batch + 1, summary.median(), summary.q1(), summary.q3(), summary.low(),
                        summary.high(), Character.toString('A' + batch));
                model.outliers[batch] = summary.outliers();
            }
            BoxPlotRenderer renderer = new BoxPlotRenderer();
            renderer.setMultiColor(true);
            return new Dataset(model, renderer, minX, maxX, minY, maxY);
        }

        private Dataset density(int samples, ArberColor color) {
            DefaultChartModel model = new DefaultChartModel(dataset, color);
            SplittableRandom random = new SplittableRandom(711);
            for (int i = 0; i < samples; i++) {
                boolean second = i % 3 == 0;
                double observation = (second ? 5.1 : 2.8) + (second ? 0.50 : 0.38) * normal(random);
                model.setPoint(i, observation, 1, "");
            }
            KDERenderer renderer = new KDERenderer().setBandwidth(0.18);
            double[] xRange = renderer.getPreferredXRange(model);
            double[] yRange = renderer.getPreferredYRange(model);
            return new Dataset(model, renderer, Math.floor(xRange[0]), Math.ceil(xRange[1]), 0,
                    Math.ceil(yRange[1] * 5) / 5);
        }
    }

    /** Uses linearly interpolated quantiles and Tukey's 1.5 IQR whiskers. */
    static BoxSummary summarize(double[] observations) {
        double[] sorted = observations.clone();
        Arrays.sort(sorted);
        double q1 = quantile(sorted, 0.25);
        double q3 = quantile(sorted, 0.75);
        double lowerFence = q1 - 1.5 * (q3 - q1);
        double upperFence = q3 + 1.5 * (q3 - q1);
        int low = 0;
        int high = sorted.length - 1;
        while (sorted[low] < lowerFence) low++;
        while (sorted[high] > upperFence) high--;
        double[] outliers = new double[low + sorted.length - 1 - high];
        System.arraycopy(sorted, 0, outliers, 0, low);
        System.arraycopy(sorted, high + 1, outliers, low, sorted.length - 1 - high);
        return new BoxSummary(quantile(sorted, 0.5), q1, q3, sorted[low], sorted[high], outliers);
    }

    private static double quantile(double[] sorted, double probability) {
        double index = (sorted.length - 1) * probability;
        int lower = (int) index;
        int upper = Math.min(lower + 1, sorted.length - 1);
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower);
    }

    private static double normal(SplittableRandom random) {
        return Math.sqrt(-2.0 * Math.log(1.0 - random.nextDouble())) * Math.cos(2 * Math.PI * random.nextDouble());
    }

    record BoxSummary(double median, double q1, double q3, double low, double high, double[] outliers) {
    }

    private static final class BoxModel extends DefaultStatisticalChartModel implements BoxPlotOutlierModel {
        private final double[][] outliers;

        private BoxModel(String name, int batches) {
            super(name);
            outliers = new double[batches][];
        }

        @Override
        public double[] getOutliers(int index) {
            return outliers[index];
        }
    }

    record Dataset(ChartModel model, ChartRenderer renderer, double minX, double maxX, double minY, double maxY) {
    }
}
