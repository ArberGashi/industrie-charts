package com.arbergashi.charts;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.ChartRenderer;

/**
 * Service provider interface for displaying charts.
 *
 * <p>This interface enables a clean separation between the headless core
 * and platform-specific display implementations (Swing, JavaFX, etc.).
 *
 * <p>Implementations are loaded via {@link java.util.ServiceLoader}. The
 * arbercharts-swing-bridge module provides the default Swing implementation.
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class SwingChartDisplayProvider implements ChartDisplayProvider {
 *     public void showChart(SimpleChart chart, String title,
 *                           ChartModel model, ChartRenderer renderer) {
 *         SwingUtilities.invokeLater(() -> {
 *             var panel = new ArberChartPanel();
 *             panel.setModel(model);
 *             panel.setRenderer(renderer);
 *             // ... create and show JFrame
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h2>ServiceLoader Registration</h2>
 * <p>Create file: {@code META-INF/services/com.arbergashi.charts.ChartDisplayProvider}
 * with the fully qualified class name of the implementation.
 *
 * @since 2.0.0
 * @see SimpleChart#show()
 */
public interface ChartDisplayProvider {

    /**
     * Displays the chart in a platform-specific window.
     *
     * @param chart    the SimpleChart instance (for callbacks)
     * @param title    the window title (may be null)
     * @param model    the chart data model
     * @param renderer the chart renderer
     */
    void showChart(SimpleChart chart, String title, ChartModel model, ChartRenderer renderer);
}

