/**
 * High-DPI display support for Swing rendering.
 *
 * <p>This package provides automatic detection and adaptation for high-resolution
 * displays (Retina, 4K, 5K, 8K) with proper scaling of fonts, strokes, and icons.
 *
 * <h2>Supported Scale Factors</h2>
 * <ul>
 *   <li><strong>100% (1.0x):</strong> Standard DPI (96 DPI)</li>
 *   <li><strong>125% (1.25x):</strong> Common on Windows laptops</li>
 *   <li><strong>150% (1.5x):</strong> Windows/macOS high-DPI</li>
 *   <li><strong>200% (2.0x):</strong> Retina/4K displays</li>
 *   <li><strong>250% (2.5x):</strong> 5K/8K displays</li>
 *   <li><strong>Custom:</strong> Any scale factor supported</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.arbergashi.charts.platform.swing.render.HighDpiRenderer} -
 *       Automatic DPI detection and scaling</li>
 * </ul>
 *
 * <h2>Automatic Features</h2>
 * <ul>
 *   <li><strong>Font Scaling:</strong> Sharp text at any DPI</li>
 *   <li><strong>Line Width Scaling:</strong> Consistent stroke appearance</li>
 *   <li><strong>Icon Scaling:</strong> SVG-based icons scale perfectly</li>
 *   <li><strong>Pixel Snapping:</strong> Prevents blurry lines</li>
 *   <li><strong>Per-Monitor:</strong> Different scales per display</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyChartPanel extends JPanel {
 *     @Override
 *     protected void paintComponent(Graphics g) {
 *         super.paintComponent(g);
 *         Graphics2D g2 = (Graphics2D) g;
 *
 *         // Apply High-DPI scaling
 *         HighDpiRenderer.applyScaling(g2, this);
 *
 *         // Draw normally - scaling is automatic
 *         g2.drawLine(0, 0, 100, 100);
 *         g2.drawString("Text", 10, 10);
 *     }
 * }
 * }</pre>
 *
 * <h2>Manual Scaling</h2>
 * <p>For fine-grained control:
 * <pre>{@code
 * // Scale a font
 * Font baseFont = new Font("Arial", Font.PLAIN, 12);
 * Font scaledFont = HighDpiRenderer.createScaledFont(baseFont, component);
 *
 * // Scale a stroke
 * BasicStroke stroke = HighDpiRenderer.createScaledStroke(2.0f, component);
 *
 * // Snap to pixel grid
 * double snapped = HighDpiRenderer.snapToPixel(10.3, component);
 * }</pre>
 *
 * <h2>Platform Support</h2>
 * <ul>
 *   <li><strong>Windows:</strong> Full support via Java 9+ HiDPI API</li>
 *   <li><strong>macOS:</strong> Native Retina support</li>
 *   <li><strong>Linux:</strong> X11/Wayland scale factor detection</li>
 * </ul>
 *
 * <h2>First Call Wins Policy</h2>
 * <p>The scale factor is cached on first access to prevent jitter when windows
 * move between displays. Call {@code HighDpiRenderer.resetCache()} if display
 * configuration changes (e.g., docking/undocking).
 *
 * @see com.arbergashi.charts.platform.swing.render.HighDpiRenderer
 * @see java.awt.GraphicsConfiguration#getDefaultTransform()
 * @since 2.0.0
 */
package com.arbergashi.charts.platform.swing.render;

