package com.arbergashi.charts.bridge;

import com.arbergashi.charts.ChartDisplayProvider;
import com.arbergashi.charts.SimpleChart;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.ChartRenderer;

import javax.swing.*;

/**
 * Swing implementation of {@link ChartDisplayProvider}.
 *
 * <p>This provider is automatically discovered via ServiceLoader when
 * arbercharts-swing-bridge is on the classpath.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Creates a properly configured JFrame with ArberChartPanel</li>
 *   <li>Thread-safe display using SwingUtilities.invokeLater</li>
 *   <li>FlatLaf dark theme by default</li>
 *   <li>Sensible default window size (800x600)</li>
 * </ul>
 *
 * <h2>ServiceLoader Registration</h2>
 * <p>Registered in: {@code META-INF/services/com.arbergashi.charts.ChartDisplayProvider}
 *
 * @since 2.0.0
 * @see ChartDisplayProvider
 */
public class SwingChartDisplayProvider implements ChartDisplayProvider {

    /**
     * Default window width.
     */
    private static final int DEFAULT_WIDTH = 800;

    /**
     * Default window height.
     */
    private static final int DEFAULT_HEIGHT = 600;

    /**
     * Creates a new Swing chart display provider.
     */
    public SwingChartDisplayProvider() {
        // Default constructor for ServiceLoader
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JFrame with an {@link ArberChartPanel} and displays the chart.
     * The frame is centered on screen with a default size of 800x600.
     */
    @Override
    public void showChart(SimpleChart chart, String title, ChartModel model, ChartRenderer renderer) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Create chart panel with model and renderer
                ArberChartPanel chartPanel = new ArberChartPanel(model, renderer);

                // Create and configure frame
                String windowTitle = title != null ? title : "ArberChart";
                JFrame frame = new JFrame(windowTitle);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(chartPanel);
                frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            } catch (Exception e) {
                // Show error dialog
                JOptionPane.showMessageDialog(null,
                    "Failed to create chart: " + e.getMessage(),
                    "ArberCharts Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

