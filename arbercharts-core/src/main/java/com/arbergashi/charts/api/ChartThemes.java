package com.arbergashi.charts.api;

import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.util.ColorRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central access point for the framework's built-in theme defaults.
 *
 * <p><b>Framework contract:</b> Core rendering code must never depend on demo resources
 * or application-specific Look&amp;Feel state. Instead it should request a theme from the
 * active {@link PlotContext} and only fall back to the stable defaults exposed here.</p>
 *
 * <h2>Available Themes (v2.0)</h2>
 * <ul>
 *   <li><strong>dark</strong> - Default dark theme</li>
 *   <li><strong>light</strong> - Default light theme</li>
 *   <li><strong>nord</strong> - Nord color palette (arctic, bluish)</li>
 *   <li><strong>dracula</strong> - Dracula theme (purple accents)</li>
 *   <li><strong>monokai</strong> - Monokai Pro colors</li>
 *   <li><strong>obsidian</strong> - Deep black, high contrast</li>
 *   <li><strong>solarized-dark</strong> - Solarized dark variant</li>
 *   <li><strong>solarized-light</strong> - Solarized light variant</li>
 *   <li><strong>github-dark</strong> - GitHub dark mode colors</li>
 *   <li><strong>medical</strong> - IEC 60601 compliant (green grid)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get theme by name
 * ChartTheme theme = ChartThemes.getTheme("nord");
 * panel.setTheme(theme);
 *
 * // Or use specific getter
 * panel.setTheme(ChartThemes.getNordTheme());
 * }</pre>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
public final class ChartThemes {

    private ChartThemes() {
        // utility class
    }

    /** Theme registry for name-based lookup. */
    private static final Map<String, ChartTheme> THEME_REGISTRY = new ConcurrentHashMap<>();

    // ===== DEFAULT THEMES =====

    private static final ChartTheme DEFAULT_DARK = new BasicChartTheme(
            ColorRegistry.of(30, 30, 30, 255),     // background
            ColorRegistry.of(220, 220, 220, 255),  // foreground
            ColorRegistry.of(85, 85, 85, 255),     // grid
            ColorRegistry.of(170, 170, 170, 255),  // axis label
            ColorRegistry.ofArgb(0xFF3B82F6),      // accent (blue)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF3B82F6), ColorRegistry.ofArgb(0xFF22C55E), ColorRegistry.ofArgb(0xFFF59E0B),
                    ColorRegistry.ofArgb(0xFFA855F7), ColorRegistry.ofArgb(0xFF06B6D4), ColorRegistry.ofArgb(0xFFEF4444)
            },
            null
    );

    private static final ChartTheme DEFAULT_LIGHT = new BasicChartTheme(
            ColorRegistry.of(250, 250, 250, 255),  // background
            ColorRegistry.of(20, 20, 20, 255),     // foreground
            ColorRegistry.of(200, 200, 200, 255),  // grid
            ColorRegistry.of(80, 80, 80, 255),     // axis label
            ColorRegistry.ofArgb(0xFF2563EB),      // accent
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF2563EB), ColorRegistry.ofArgb(0xFF16A34A), ColorRegistry.ofArgb(0xFFD97706),
                    ColorRegistry.ofArgb(0xFF7C3AED), ColorRegistry.ofArgb(0xFF0891B2), ColorRegistry.ofArgb(0xFFDC2626)
            },
            null
    );

    // ===== NORD THEME (Arctic, bluish tones) =====
    private static final ChartTheme NORD = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF2E3440),      // background (nord0)
            ColorRegistry.ofArgb(0xFFECEFF4),      // foreground (nord6)
            ColorRegistry.ofArgb(0xFF4C566A),      // grid (nord3)
            ColorRegistry.ofArgb(0xFFD8DEE9),      // axis label (nord4)
            ColorRegistry.ofArgb(0xFF88C0D0),      // accent (nord8 - frost)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF88C0D0), // frost cyan
                    ColorRegistry.ofArgb(0xFFA3BE8C), // aurora green
                    ColorRegistry.ofArgb(0xFFEBCB8B), // aurora yellow
                    ColorRegistry.ofArgb(0xFFB48EAD), // aurora purple
                    ColorRegistry.ofArgb(0xFF81A1C1), // frost blue
                    ColorRegistry.ofArgb(0xFFBF616A)  // aurora red
            },
            null
    );

    // ===== DRACULA THEME (Purple accents, popular dark theme) =====
    private static final ChartTheme DRACULA = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF282A36),      // background
            ColorRegistry.ofArgb(0xFFF8F8F2),      // foreground
            ColorRegistry.ofArgb(0xFF44475A),      // grid (current line)
            ColorRegistry.ofArgb(0xFF6272A4),      // axis label (comment)
            ColorRegistry.ofArgb(0xFFBD93F9),      // accent (purple)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFFBD93F9), // purple
                    ColorRegistry.ofArgb(0xFF50FA7B), // green
                    ColorRegistry.ofArgb(0xFFF1FA8C), // yellow
                    ColorRegistry.ofArgb(0xFFFF79C6), // pink
                    ColorRegistry.ofArgb(0xFF8BE9FD), // cyan
                    ColorRegistry.ofArgb(0xFFFF5555)  // red
            },
            null
    );

    // ===== MONOKAI THEME (Classic code editor theme) =====
    private static final ChartTheme MONOKAI = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF272822),      // background
            ColorRegistry.ofArgb(0xFFF8F8F2),      // foreground
            ColorRegistry.ofArgb(0xFF49483E),      // grid
            ColorRegistry.ofArgb(0xFF75715E),      // axis label (comment)
            ColorRegistry.ofArgb(0xFFA6E22E),      // accent (green)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFFA6E22E), // green
                    ColorRegistry.ofArgb(0xFF66D9EF), // cyan
                    ColorRegistry.ofArgb(0xFFF92672), // pink/red
                    ColorRegistry.ofArgb(0xFFAE81FF), // purple
                    ColorRegistry.ofArgb(0xFFE6DB74), // yellow
                    ColorRegistry.ofArgb(0xFFFD971F)  // orange
            },
            null
    );

    // ===== OBSIDIAN THEME (Deep black, high contrast) =====
    private static final ChartTheme OBSIDIAN = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF1A1A1A),      // background (deep black)
            ColorRegistry.ofArgb(0xFFFFFFFF),      // foreground (pure white)
            ColorRegistry.ofArgb(0xFF333333),      // grid
            ColorRegistry.ofArgb(0xFFAAAAAA),      // axis label
            ColorRegistry.ofArgb(0xFF00D4FF),      // accent (electric blue)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF00D4FF), // electric blue
                    ColorRegistry.ofArgb(0xFF00FF7F), // spring green
                    ColorRegistry.ofArgb(0xFFFFD700), // gold
                    ColorRegistry.ofArgb(0xFFFF6B6B), // coral red
                    ColorRegistry.ofArgb(0xFFDA70D6), // orchid
                    ColorRegistry.ofArgb(0xFF20B2AA)  // light sea green
            },
            null
    );

    // ===== SOLARIZED DARK =====
    private static final ChartTheme SOLARIZED_DARK = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF002B36),      // base03
            ColorRegistry.ofArgb(0xFF839496),      // base0
            ColorRegistry.ofArgb(0xFF073642),      // base02
            ColorRegistry.ofArgb(0xFF586E75),      // base01
            ColorRegistry.ofArgb(0xFF268BD2),      // blue
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF268BD2), // blue
                    ColorRegistry.ofArgb(0xFF859900), // green
                    ColorRegistry.ofArgb(0xFFB58900), // yellow
                    ColorRegistry.ofArgb(0xFFD33682), // magenta
                    ColorRegistry.ofArgb(0xFF2AA198), // cyan
                    ColorRegistry.ofArgb(0xFFDC322F)  // red
            },
            null
    );

    // ===== SOLARIZED LIGHT =====
    private static final ChartTheme SOLARIZED_LIGHT = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFFFDF6E3),      // base3
            ColorRegistry.ofArgb(0xFF657B83),      // base00
            ColorRegistry.ofArgb(0xFFEEE8D5),      // base2
            ColorRegistry.ofArgb(0xFF93A1A1),      // base1
            ColorRegistry.ofArgb(0xFF268BD2),      // blue
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF268BD2), // blue
                    ColorRegistry.ofArgb(0xFF859900), // green
                    ColorRegistry.ofArgb(0xFFB58900), // yellow
                    ColorRegistry.ofArgb(0xFFD33682), // magenta
                    ColorRegistry.ofArgb(0xFF2AA198), // cyan
                    ColorRegistry.ofArgb(0xFFDC322F)  // red
            },
            null
    );

    // ===== GITHUB DARK =====
    private static final ChartTheme GITHUB_DARK = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF0D1117),      // background
            ColorRegistry.ofArgb(0xFFC9D1D9),      // foreground
            ColorRegistry.ofArgb(0xFF21262D),      // grid
            ColorRegistry.ofArgb(0xFF8B949E),      // axis label
            ColorRegistry.ofArgb(0xFF58A6FF),      // accent (link blue)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF58A6FF), // blue
                    ColorRegistry.ofArgb(0xFF3FB950), // green
                    ColorRegistry.ofArgb(0xFFD29922), // yellow
                    ColorRegistry.ofArgb(0xFFA371F7), // purple
                    ColorRegistry.ofArgb(0xFF79C0FF), // light blue
                    ColorRegistry.ofArgb(0xFFF85149)  // red
            },
            null
    );

    // ===== MEDICAL THEME (IEC 60601 compliant, green grid) =====
    private static final ChartTheme MEDICAL = new BasicChartTheme(
            ColorRegistry.ofArgb(0xFF000000),      // background (black for contrast)
            ColorRegistry.ofArgb(0xFF00FF00),      // foreground (medical green)
            ColorRegistry.ofArgb(0xFF004400),      // grid (dark green)
            ColorRegistry.ofArgb(0xFF00CC00),      // axis label
            ColorRegistry.ofArgb(0xFF00FF00),      // accent (green)
            new ArberColor[]{
                    ColorRegistry.ofArgb(0xFF00FF00), // green (primary trace)
                    ColorRegistry.ofArgb(0xFFFFFF00), // yellow (secondary)
                    ColorRegistry.ofArgb(0xFF00FFFF), // cyan (SpO2)
                    ColorRegistry.ofArgb(0xFFFF0000), // red (alarm)
                    ColorRegistry.ofArgb(0xFFFF00FF), // magenta
                    ColorRegistry.ofArgb(0xFFFFFFFF)  // white
            },
            null
    );

    // Initialize theme registry
    static {
        THEME_REGISTRY.put("dark", DEFAULT_DARK);
        THEME_REGISTRY.put("light", DEFAULT_LIGHT);
        THEME_REGISTRY.put("nord", NORD);
        THEME_REGISTRY.put("dracula", DRACULA);
        THEME_REGISTRY.put("monokai", MONOKAI);
        THEME_REGISTRY.put("obsidian", OBSIDIAN);
        THEME_REGISTRY.put("solarized-dark", SOLARIZED_DARK);
        THEME_REGISTRY.put("solarized-light", SOLARIZED_LIGHT);
        THEME_REGISTRY.put("github-dark", GITHUB_DARK);
        THEME_REGISTRY.put("medical", MEDICAL);
    }

    private static boolean hasAssetOverrides() {
        return com.arbergashi.charts.util.ChartAssets.getString("Chart.background", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.foreground", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.grid.color", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.axisLabelColor", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.accentColor", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("Chart.accent.blue", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series1", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series2", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series3", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series4", null) != null
                || com.arbergashi.charts.util.ChartAssets.getString("series5", null) != null;
    }

    private static ChartTheme buildFromAssets(ChartTheme fallback) {
        if (!hasAssetOverrides()) {
            return fallback;
        }
        com.arbergashi.charts.util.ChartAssets.clearCache();
        ArberColor background = com.arbergashi.charts.util.ChartAssets.getColor("Chart.background", fallback.getBackground());
        ArberColor foreground = com.arbergashi.charts.util.ChartAssets.getColor("Chart.foreground", fallback.getForeground());
        ArberColor grid = com.arbergashi.charts.util.ChartAssets.getColor("Chart.grid.color", fallback.getGridColor());
        ArberColor axis = com.arbergashi.charts.util.ChartAssets.getColor("Chart.axisLabelColor", fallback.getAxisLabelColor());
        ArberColor accent = com.arbergashi.charts.util.ChartAssets.getColor("Chart.accentColor",
                com.arbergashi.charts.util.ChartAssets.getColor("Chart.accent.blue", fallback.getAccentColor()));
        ArberColor[] series = new ArberColor[]{
                com.arbergashi.charts.util.ChartAssets.getColor("series1", fallback.getSeriesColor(0)),
                com.arbergashi.charts.util.ChartAssets.getColor("series2", fallback.getSeriesColor(1)),
                com.arbergashi.charts.util.ChartAssets.getColor("series3", fallback.getSeriesColor(2)),
                com.arbergashi.charts.util.ChartAssets.getColor("series4", fallback.getSeriesColor(3)),
                com.arbergashi.charts.util.ChartAssets.getColor("series5", fallback.getSeriesColor(4))
        };
        return new BasicChartTheme(background, foreground, grid, axis, accent, series, null);
    }

    /**
     * The default dark theme used as a stable fallback.
     */
    public static ChartTheme getDarkTheme() {
        return buildFromAssets(DEFAULT_DARK);
    }

    /**
     * The default light theme used as a stable fallback.
     */
    public static ChartTheme getLightTheme() {
        return buildFromAssets(DEFAULT_LIGHT);
    }

    /**
     * Gets the Nord theme (arctic, bluish tones).
     *
     * <p>Based on the popular Nord color palette.
     *
     * @return the Nord theme
     * @since 2.0.0
     */
    public static ChartTheme getNordTheme() {
        return NORD;
    }

    /**
     * Gets the Dracula theme (purple accents).
     *
     * <p>Based on the popular Dracula color scheme.
     *
     * @return the Dracula theme
     * @since 2.0.0
     */
    public static ChartTheme getDraculaTheme() {
        return DRACULA;
    }

    /**
     * Gets the Monokai theme (classic code editor colors).
     *
     * @return the Monokai theme
     * @since 2.0.0
     */
    public static ChartTheme getMonokaiTheme() {
        return MONOKAI;
    }

    /**
     * Gets the Obsidian theme (deep black, high contrast).
     *
     * <p>Ideal for trading terminals and dark environments.
     *
     * @return the Obsidian theme
     * @since 2.0.0
     */
    public static ChartTheme getObsidianTheme() {
        return OBSIDIAN;
    }

    /**
     * Gets the Solarized Dark theme.
     *
     * @return the Solarized Dark theme
     * @since 2.0.0
     */
    public static ChartTheme getSolarizedDarkTheme() {
        return SOLARIZED_DARK;
    }

    /**
     * Gets the Solarized Light theme.
     *
     * @return the Solarized Light theme
     * @since 2.0.0
     */
    public static ChartTheme getSolarizedLightTheme() {
        return SOLARIZED_LIGHT;
    }

    /**
     * Gets the GitHub Dark theme.
     *
     * <p>Matches GitHub's dark mode colors.
     *
     * @return the GitHub Dark theme
     * @since 2.0.0
     */
    public static ChartTheme getGitHubDarkTheme() {
        return GITHUB_DARK;
    }

    /**
     * Gets the Medical theme (IEC 60601 compliant).
     *
     * <p>Green-on-black color scheme compliant with medical device standards.
     * Suitable for ECG, EEG, and patient monitoring displays.
     *
     * @return the Medical theme
     * @since 2.0.0
     */
    public static ChartTheme getMedicalTheme() {
        return MEDICAL;
    }

    /**
     * Gets a theme by name.
     *
     * <p>Available theme names:
     * <ul>
     *   <li>{@code dark} - Default dark theme</li>
     *   <li>{@code light} - Default light theme</li>
     *   <li>{@code nord} - Nord arctic theme</li>
     *   <li>{@code dracula} - Dracula purple theme</li>
     *   <li>{@code monokai} - Monokai Pro theme</li>
     *   <li>{@code obsidian} - High contrast black theme</li>
     *   <li>{@code solarized-dark} - Solarized dark variant</li>
     *   <li>{@code solarized-light} - Solarized light variant</li>
     *   <li>{@code github-dark} - GitHub dark mode</li>
     *   <li>{@code medical} - IEC 60601 compliant</li>
     * </ul>
     *
     * @param name the theme name (case-insensitive)
     * @return the theme, or the default dark theme if not found
     * @since 2.0.0
     */
    public static ChartTheme getTheme(String name) {
        if (name == null) {
            return DEFAULT_DARK;
        }
        return THEME_REGISTRY.getOrDefault(name.toLowerCase(), DEFAULT_DARK);
    }

    /**
     * Returns all available theme names.
     *
     * @return array of theme names
     * @since 2.0.0
     */
    public static String[] getAvailableThemes() {
        return THEME_REGISTRY.keySet().toArray(new String[0]);
    }

    /**
     * Registers a custom theme.
     *
     * <p>Allows applications to add their own themes to the registry.
     *
     * @param name the theme name
     * @param theme the theme instance
     * @since 2.0.0
     */
    public static void registerTheme(String name, ChartTheme theme) {
        if (name != null && theme != null) {
            THEME_REGISTRY.put(name.toLowerCase(), theme);
        }
    }
}
