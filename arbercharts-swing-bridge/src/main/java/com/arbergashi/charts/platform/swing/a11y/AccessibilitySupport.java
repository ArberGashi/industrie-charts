package com.arbergashi.charts.platform.swing.a11y;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Basic accessibility metadata and contrast utilities for Swing chart components.
 *
 * <p>{@link #enable(JComponent)} makes a component focusable and assigns an
 * accessible name and description. {@link #announce(JComponent, String)} publishes
 * description changes through the Java Accessibility API.
 *
 * <p>The installed keyboard listener logs navigation requests but does not move
 * or select data points. High-contrast detection is not implemented. This helper
 * does not establish accessibility compliance; the host application must provide
 * and validate its keyboard interaction, focus presentation and assistive-technology
 * integration.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ArberChartPanel chart = new ArberChartPanel(model, renderer);
 * AccessibilitySupport.enable(chart);
 * AccessibilitySupport.announce(chart, "Chart data updated");
 * }</pre>
 *
 * @since 2.0.0
 * @see javax.accessibility.Accessible
 * @see <a href="https://www.w3.org/WAI/WCAG21/quickref/">WCAG 2.1 Guidelines</a>
 */
public final class AccessibilitySupport {
    private static final System.Logger LOGGER = System.getLogger(AccessibilitySupport.class.getName());

    private AccessibilitySupport() {
        // Utility class
    }

    /**
     * Assigns basic accessibility metadata and makes a chart component focusable.
     *
     * <p>Also installs a keyboard listener that logs navigation requests. It does
     * not implement data-point navigation or automatic announcements.
     *
     * @param component the chart component to enhance
     */
    public static void enable(JComponent component) {
        // Make component focusable
        component.setFocusable(true);

        // Observe keyboard requests without changing chart selection
        component.addKeyListener(new ChartKeyboardNavigator());

        // Configure accessible context
        AccessibleContext context = component.getAccessibleContext();
        context.setAccessibleName("Chart");
        context.setAccessibleDescription("Interactive data visualization chart");

        // Store a role hint as a client property; this does not change AccessibleContext's role
        component.putClientProperty("AccessibleRole", AccessibleRole.PANEL);
    }

    /**
     * Publishes text as an accessible-description change.
     *
     * <p>How the change is announced depends on the component's accessibility
     * implementation and the assistive technology in use.
     *
     * @param component the component context
     * @param text the text to announce
     */
    public static void announce(JComponent component, String text) {
        AccessibleContext context = component.getAccessibleContext();
        String oldDescription = context.getAccessibleDescription();
        context.setAccessibleDescription(text);

        // Notify accessibility infrastructure of change
        context.firePropertyChange(
            AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY,
            oldDescription,
            text
        );
    }

    /**
     * Returns the default high-contrast flag without querying the operating system.
     *
     * <p>Platform-specific detection is not implemented. The host application
     * must obtain and apply its preferred contrast setting separately.
     *
     * @return always {@code false}
     */
    public static boolean isHighContrastMode() {
        return false;
    }

    /**
     * Gets recommended minimum contrast ratio for text.
     *
     * <p>WCAG 2.1 AA requires:
     * <ul>
     *   <li>4.5:1 for normal text</li>
     *   <li>3:1 for large text (18pt+ or 14pt+ bold)</li>
     * </ul>
     *
     * @param isLargeText true if text is 18pt+ or 14pt+ bold
     * @return minimum contrast ratio
     */
    public static double getMinimumContrastRatio(boolean isLargeText) {
        return isLargeText ? 3.0 : 4.5;
    }

    /**
     * Calculates contrast ratio between two colors.
     *
     * <p>Formula: (L1 + 0.05) / (L2 + 0.05)
     * where L1 is the lighter color luminance and L2 is darker.
     *
     * @param rgb1 first color (RGB int)
     * @param rgb2 second color (RGB int)
     * @return contrast ratio (1.0 to 21.0)
     */
    public static double calculateContrastRatio(int rgb1, int rgb2) {
        double lum1 = calculateRelativeLuminance(rgb1);
        double lum2 = calculateRelativeLuminance(rgb2);

        double lighter = Math.max(lum1, lum2);
        double darker = Math.min(lum1, lum2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Calculates relative luminance of an RGB color.
     *
     * <p>Formula from WCAG 2.1:
     * L = 0.2126 * R + 0.7152 * G + 0.0722 * B
     *
     * @param rgb color as RGB integer
     * @return relative luminance (0.0 to 1.0)
     */
    private static double calculateRelativeLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        double rSRGB = r / 255.0;
        double gSRGB = g / 255.0;
        double bSRGB = b / 255.0;

        double rLin = (rSRGB <= 0.03928) ? rSRGB / 12.92 : Math.pow((rSRGB + 0.055) / 1.055, 2.4);
        double gLin = (gSRGB <= 0.03928) ? gSRGB / 12.92 : Math.pow((gSRGB + 0.055) / 1.055, 2.4);
        double bLin = (bSRGB <= 0.03928) ? bSRGB / 12.92 : Math.pow((bSRGB + 0.055) / 1.055, 2.4);

        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin;
    }

    /**
     * Internal listener that logs chart-navigation key requests.
     *
     * <p>Key events do not change the chart model, focus or selected data point.
     */
    private static class ChartKeyboardNavigator implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_UP:
                    LOGGER.log(System.Logger.Level.DEBUG, "Accessibility: Previous data point (not yet implemented)");
                    break;

                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_DOWN:
                    LOGGER.log(System.Logger.Level.DEBUG, "Accessibility: Next data point (not yet implemented)");
                    break;

                case KeyEvent.VK_HOME:
                    LOGGER.log(System.Logger.Level.DEBUG, "Accessibility: First data point (not yet implemented)");
                    break;

                case KeyEvent.VK_END:
                    LOGGER.log(System.Logger.Level.DEBUG, "Accessibility: Last data point (not yet implemented)");
                    break;

                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    LOGGER.log(System.Logger.Level.DEBUG, "Accessibility: Select data point (not yet implemented)");
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not needed
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // Not needed
        }
    }
}

