/**
 * Line-chart convenience API and the display-provider contract.
 *
 * <p>{@link com.arbergashi.charts.Charts#lineChart()} creates a line chart with
 * a circular model and a line renderer. Manual point insertion, a window title
 * and display through {@link com.arbergashi.charts.ChartDisplayProvider} are
 * implemented. The bundled provider requires {@code arbercharts-swing-bridge}
 * and a graphical desktop.
 *
 * <h2>Line Chart</h2>
 * <pre>{@code
 * import com.arbergashi.charts.Charts;
 *
 * Charts.lineChart()
 *     .title("Temperature")
 *     .addData(1, 20.5, 2, 21.0, 3, 20.8)
 *     .show();
 * }</pre>
 *
 * <p>The scatter, bar, candlestick and ECG convenience factories, automatic
 * streaming, and file-export helpers are not implemented. The subtitle,
 * axis-label and theme values stored
 * by {@link com.arbergashi.charts.SimpleChart} are not applied by the bundled
 * display provider. Use models, renderers and a chart host directly when those
 * features are needed.
 *
 * <h2>Swing Host</h2>
 * <p>Create and configure Swing components on the event dispatch thread. With
 * {@code arbercharts-swing-bridge} on the classpath:
 * <pre>{@code
 * import com.arbergashi.charts.api.ChartThemes;
 * import com.arbergashi.charts.model.CircularChartModel;
 * import com.arbergashi.charts.platform.swing.ArberChartPanel;
 * import com.arbergashi.charts.render.standard.LineRenderer;
 * import javax.swing.JFrame;
 * import javax.swing.SwingUtilities;
 *
 * SwingUtilities.invokeLater(() -> {
 *     var model = new CircularChartModel(1024);
 *     model.setPoint(1, 20.5, 1.0, null);
 *     model.setPoint(2, 21.0, 1.0, null);
 *     var panel = new ArberChartPanel(model, new LineRenderer());
 *     panel.setTheme(ChartThemes.getLightTheme());
 *
 *     var frame = new JFrame("Temperature");
 *     frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 *     frame.add(panel);
 *     frame.setSize(800, 450);
 *     frame.setLocationRelativeTo(null);
 *     frame.setVisible(true);
 * });
 * }</pre>
 *
 * <h2>Server PNG Output</h2>
 * <p>With {@code arbercharts-server-bridge} on the classpath, render an
 * application-owned model and close the service when its owner shuts down:
 * <pre>{@code
 * import com.arbergashi.charts.bridge.server.ServerRenderService;
 * import com.arbergashi.charts.model.CircularChartModel;
 * import java.awt.Dimension;
 *
 * var model = new CircularChartModel(3);
 * model.setPoint(1, 10, 1.0, null);
 * model.setPoint(2, 20, 1.0, null);
 * model.setPoint(3, 15, 1.0, null);
 * byte[] png;
 * try (var service = new ServerRenderService(1)) {
 *     png = service.renderToPng(model, new Dimension(800, 450));
 * }
 * }</pre>
 *
 * <p>The server bridge uses Java2D and requires {@code java.desktop}, even with
 * {@code -Djava.awt.headless=true}. The Core artifact remains independent of
 * desktop APIs. Model access and renderer reuse must follow their ownership
 * and concurrency contracts. Allocation rates and throughput depend on the
 * workload, backend and runtime; this package provides no universal latency
 * or zero-allocation guarantee.
 *
 * @since 2.0.0
 * @see com.arbergashi.charts.Charts
 * @see com.arbergashi.charts.SimpleChart
 * @see <a href="https://github.com/ArberGashi/industrie-charts/blob/main/docs/QUICK_START.md">Quick Start Guide</a>
 */
package com.arbergashi.charts;

