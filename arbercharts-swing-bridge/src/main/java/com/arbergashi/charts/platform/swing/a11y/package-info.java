/**
 * Basic accessibility metadata and contrast calculations for Swing charts.
 *
 * <p>{@link com.arbergashi.charts.platform.swing.a11y.AccessibilitySupport}
 * provides:
 * <ul>
 *   <li>A focusable component with an accessible name and description</li>
 *   <li>Explicit accessible-description change notifications</li>
 *   <li>RGB contrast-ratio calculations and text-contrast thresholds</li>
 * </ul>
 *
 * <p>The keyboard listener logs requests without navigating or selecting data
 * points. System high-contrast detection is not implemented, and data updates
 * do not trigger announcements automatically. The host application must provide
 * and validate keyboard interaction, focus presentation and assistive-technology
 * integration. This package does not establish WCAG compliance.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ArberChartPanel chart = new ArberChartPanel(model, renderer);
 * AccessibilitySupport.enable(chart);
 * AccessibilitySupport.announce(chart, "Chart data updated");
 * }</pre>
 *
 * <h2>Testing Accessibility</h2>
 * <p>Test with:
 * <ul>
 *   <li><strong>Windows:</strong> JAWS, NVDA</li>
 *   <li><strong>macOS:</strong> VoiceOver (Cmd+F5)</li>
 *   <li><strong>Linux:</strong> Orca</li>
 * </ul>
 *
 * @see com.arbergashi.charts.platform.swing.a11y.AccessibilitySupport
 * @see javax.accessibility.Accessible
 * @see <a href="https://www.w3.org/WAI/WCAG21/quickref/">WCAG 2.1 Quick Reference</a>
 * @since 2.0.0
 */
package com.arbergashi.charts.platform.swing.a11y;

