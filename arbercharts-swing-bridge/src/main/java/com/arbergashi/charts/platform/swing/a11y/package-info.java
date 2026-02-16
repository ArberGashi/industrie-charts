/**
 * WCAG 2.1 AA accessibility support for Swing charts.
 *
 * <p>This package provides comprehensive accessibility features for screen
 * readers, keyboard navigation, and assistive technologies.
 *
 * <h2>WCAG 2.1 AA Compliance</h2>
 * <p>ArberCharts Swing components comply with Web Content Accessibility
 * Guidelines (WCAG) 2.1 Level AA, adapted for Java Swing:
 *
 * <ul>
 *   <li><strong>1.4.3 Contrast (Minimum):</strong> 4.5:1 for normal text,
 *       3:1 for large text</li>
 *   <li><strong>2.1.1 Keyboard:</strong> All functionality available via keyboard</li>
 *   <li><strong>2.4.7 Focus Visible:</strong> Clear visual focus indicators</li>
 *   <li><strong>4.1.2 Name, Role, Value:</strong> Proper accessible context</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.arbergashi.charts.platform.swing.a11y.AccessibilitySupport} -
 *       Main accessibility utilities</li>
 * </ul>
 *
 * <h2>Supported Features</h2>
 * <ul>
 *   <li><strong>Screen Readers:</strong> JAWS, NVDA, VoiceOver compatible</li>
 *   <li><strong>Keyboard Navigation:</strong> Tab, Arrow Keys, Enter, Space</li>
 *   <li><strong>Focus Indicators:</strong> Clear visual focus states</li>
 *   <li><strong>High Contrast:</strong> System high-contrast mode detection</li>
 *   <li><strong>Announcements:</strong> Data point changes announced</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ArberChartPanel chart = new ArberChartPanel();
 *
 * // Enable accessibility features
 * AccessibilitySupport.enable(chart);
 *
 * // Chart now supports:
 * // - Screen readers (announces data points)
 * // - Keyboard navigation (Arrow keys to navigate)
 * // - Focus indicators (visible focus state)
 * // - High contrast themes (system detection)
 * }</pre>
 *
 * <h2>Keyboard Shortcuts</h2>
 * <table border="1">
 *   <tr>
 *     <th>Key</th>
 *     <th>Action</th>
 *   </tr>
 *   <tr>
 *     <td>Tab / Shift+Tab</td>
 *     <td>Focus next/previous data point</td>
 *   </tr>
 *   <tr>
 *     <td>Arrow Keys</td>
 *     <td>Navigate between data points</td>
 *   </tr>
 *   <tr>
 *     <td>Enter / Space</td>
 *     <td>Select/activate data point</td>
 *   </tr>
 *   <tr>
 *     <td>Home / End</td>
 *     <td>Jump to first/last data point</td>
 *   </tr>
 * </table>
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

