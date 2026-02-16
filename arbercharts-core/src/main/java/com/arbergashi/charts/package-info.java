/**
 * Simple, fluent API for creating charts with minimal code.
 *
 * <p><strong>Design Philosophy:</strong> ArberCharts should be simple to use for
 * common cases while remaining powerful for advanced scenarios.
 *
 * <h2>Quick Start (2 Lines)</h2>
 * <pre>{@code
 * Charts.lineChart()
 *     .addData(1, 10, 2, 20, 3, 15)
 *     .show();
 * }</pre>
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link com.arbergashi.charts.Charts} - Factory for creating charts</li>
 *   <li>{@link com.arbergashi.charts.SimpleChart} - Fluent API for configuration</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Minimal Code:</strong> Common charts in 2-5 lines</li>
 *   <li><strong>Sensible Defaults:</strong> Works out-of-box</li>
 *   <li><strong>Fluent API:</strong> Method chaining for readability</li>
 *   <li><strong>Zero Configuration:</strong> No XML, no properties files</li>
 *   <li><strong>Type-Safe:</strong> Compile-time validation</li>
 * </ul>
 *
 * <h2>Examples by Use Case</h2>
 *
 * <h3>Simple Line Chart</h3>
 * <pre>{@code
 * Charts.lineChart()
 *     .title("Temperature")
 *     .addData(1, 20.5, 2, 21.0, 3, 20.8)
 *     .show();
 * }</pre>
 *
 * <h3>Real-Time Monitoring</h3>
 * <pre>{@code
 * Charts.streamingLineChart(1000)
 *     .title("CPU Usage")
 *     .updateInterval(100)  // 10 Hz
 *     .startStreaming(() -> getCpuUsage())
 *     .show();
 * }</pre>
 *
 * <h3>Financial Chart</h3>
 * <pre>{@code
 * Charts.candlestickChart()
 *     .title("BTC/USD")
 *     .loadFromCSV("bitcoin-prices.csv")
 *     .show();
 * }</pre>
 *
 * <h3>Medical ECG</h3>
 * <pre>{@code
 * Charts.ecgChart()
 *     .sweepMode(true)
 *     .gridEnabled(true)
 *     .startStreaming(() -> getHeartRateSignal())
 *     .show();
 * }</pre>
 *
 * <h2>Comparison: Simple vs Advanced API</h2>
 *
 * <h3>Simple API (Recommended for 80% of use cases)</h3>
 * <pre>{@code
 * // 3 lines, no imports needed
 * Charts.lineChart()
 *     .addData(1, 10, 2, 20)
 *     .show();
 * }</pre>
 *
 * <h3>Advanced API (For custom renderers, etc.)</h3>
 * <pre>{@code
 * // 10+ lines, manual configuration
 * CircularChartModel model = new CircularChartModel(1000);
 * LineRenderer renderer = new LineRenderer();
 * ArberChartPanel panel = new ArberChartPanel();
 * panel.setModel(model);
 * panel.setRenderer(renderer);
 * // ... more configuration ...
 * }</pre>
 *
 * <p><strong>Rule of Thumb:</strong> Use Simple API unless you need:
 * <ul>
 *   <li>Custom renderer implementation</li>
 *   <li>Low-level model manipulation</li>
 *   <li>Advanced performance tuning</li>
 * </ul>
 *
 * <h2>Method Chaining</h2>
 * <p>All configuration methods return {@code this} for fluent chaining:
 * <pre>{@code
 * Charts.lineChart()
 *     .title("Sales")          // returns this
 *     .subtitle("Q1 2026")     // returns this
 *     .xLabel("Month")         // returns this
 *     .yLabel("Revenue")       // returns this
 *     .theme("obsidian")       // returns this
 *     .addData(1, 100)         // returns this
 *     .show();                 // returns this (can chain .exportToPNG())
 * }</pre>
 *
 * <h2>Zero-GC Guarantee</h2>
 * <p>The Simple API inherits Zero-GC guarantees from the core framework:
 * <ul>
 *   <li><strong>No allocations</strong> in render hot paths</li>
 *   <li><strong>&lt;1ms p99 latency</strong> guaranteed</li>
 *   <li><strong>10,000+ renders/sec</strong> sustained</li>
 * </ul>
 *
 * <h2>Spring Boot Integration</h2>
 * <p>Works seamlessly with Spring Boot:
 * <pre>{@code
 * @RestController
 * public class ChartController {
 *     @GetMapping("/chart.png")
 *     public byte[] chart() {
 *         return Charts.lineChart()
 *             .addData(data)
 *             .exportToPNG();
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @see com.arbergashi.charts.Charts
 * @see com.arbergashi.charts.SimpleChart
 * @see <a href="../../../docs/QUICK_START.md">Quick Start Guide</a>
 */
package com.arbergashi.charts;

