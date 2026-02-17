package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.BasicChartTheme;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.util.ChartAssets;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

final class DemoThemeSupport {

    private DemoThemeSupport() {
    }

    static String setupLookAndFeel() {
        verifyThemeResources();
        try {
            Class<?> interFontClass = Class.forName("com.formdev.flatlaf.fonts.inter.FlatInterFont");
            java.lang.reflect.Method installMethod = interFontClass.getMethod("installLazy");
            installMethod.invoke(null);
        } catch (Exception e) {
            // Inter font not available - will use system fonts
        }

        FlatLaf.registerCustomDefaultsSource("themes");

        String theme = normalizeTheme(System.getProperty("demo.theme", "dark").toLowerCase(Locale.US));
        if ("light".equals(theme)) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }

        Font interFont = com.arbergashi.charts.platform.swing.util.ChartFonts.getBaseFont();
        UIManager.put("Chart.font", interFont.deriveFont(Font.PLAIN, 11f));
        UIManager.put("defaultFont", interFont);

        ChartAssets.clearCache();
        return theme;
    }

    static void verifyThemeResources() {
        requireResource("themes/FlatDarkLaf.properties");
        requireResource("themes/FlatLightLaf.properties");
        requireResource("themes/charts.properties");
    }

    static String normalizeTheme(String themeName) {
        return "light".equalsIgnoreCase(themeName) ? "light" : "dark";
    }

    static void applyLookAndFeel(String themeName) {
        if ("light".equals(themeName)) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }
        FlatLaf.updateUI();
    }

    static DemoPalette currentPalette() {
        Color window = uiColor("Panel.background", "control");
        Color content = uiColor("Panel.background", "control");
        Color surface = uiColor("TextField.background", "Panel.background");
        Color sidebar = uiColor("Tree.background", "Panel.background");
        Color border = uiColor("Component.borderColor", "Separator.foreground");
        Color foreground = uiColor("Label.foreground", "textText");
        Color muted = uiColor("Component.grayForeground", "Label.disabledForeground");
        Color softMuted = withAlpha(muted, 210);
        return new DemoPalette(window, content, surface, sidebar, border, foreground, muted, softMuted);
    }

    static ChartTheme buildChartTheme(String themeName) {
        boolean light = "light".equals(themeName);
        String p = light ? "Demo.chart.light." : "Demo.chart.dark.";

        ArberColor bg = requireColor(p + "background");
        ArberColor fg = requireColor(p + "foreground");
        ArberColor grid = requireColor(p + "grid");
        ArberColor axis = requireColor(p + "axis");
        ArberColor accent = requireColor(p + "accent");

        ArberColor[] series = new ArberColor[]{
                requireColor(p + "series1"),
                requireColor(p + "series2"),
                requireColor(p + "series3"),
                requireColor(p + "series4"),
                requireColor(p + "series5")
        };

        Font base = UIManager.getFont("defaultFont");
        if (base == null) {
            base = new Font("SansSerif", Font.PLAIN, 11);
        }

        return BasicChartTheme.builder()
                .setBackground(bg)
                .setForeground(fg)
                .setGridColor(grid)
                .setAxisLabelColor(axis)
                .setAccentColor(accent)
                .setSeriesColors(series)
                .setBaseFont(new ArberFont(base.getName(), base.getStyle(), base.getSize2D()))
                .build();
    }

    static Color uiColor(String key, String fallbackKey) {
        Color c = UIManager.getColor(key);
        if (c != null) {
            return c;
        }
        c = UIManager.getColor(fallbackKey);
        if (c != null) {
            return c;
        }
        c = UIManager.getColor("Panel.foreground");
        if (c != null) {
            return c;
        }
        c = UIManager.getColor("Label.foreground");
        if (c != null) {
            return c;
        }
        return new JPanel().getForeground();
    }

    static void clearAssetCache() {
        ChartAssets.clearCache();
    }

    private static Color withAlpha(Color color, int alpha) {
        if (color == null) {
            return null;
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static ArberColor requireColor(String key) {
        ArberColor color = ChartAssets.getColor(key, null);
        if (color == null) {
            throw new IllegalStateException("Missing theme color in resources/themes/charts.properties: " + key);
        }
        return color;
    }

    private static void requireResource(String path) {
        if (DemoThemeSupport.class.getClassLoader().getResource(path) == null) {
            throw new IllegalStateException("Missing required demo theme resource: " + path);
        }
    }
}
