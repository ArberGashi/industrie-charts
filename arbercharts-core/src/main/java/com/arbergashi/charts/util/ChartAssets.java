package com.arbergashi.charts.util;

import javax.swing.UIManager;
import java.awt.Color;
import java.io.InputStream;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central configuration registry for ArberCharts.
 *
 * <p>This is a small, thread-safe key/value store intended for framework-level tunables
 * (e.g., default line widths, alpha values, thresholds). Reads are optimized via internal
 * caches for parsed primitives.</p>
 *
 * <p><b>Theme Integration:</b> Methods prefixed with "getUI" read from {@link UIManager},
 * enabling seamless integration with FlatLaf and other pluggable Look&amp;Feel themes.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartAssets {
    private static final Properties properties = new Properties();
    private static final Map<String, Float> floatCache = new ConcurrentHashMap<>();
    private static final Map<String, Integer> intCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> boolCache = new ConcurrentHashMap<>();
    private static final Map<Locale, ResourceBundle> bundleCache = new ConcurrentHashMap<>();
    private static volatile String i18nBaseName = "i18n.charts";

    static {
        loadDefaults();
    }
    private ChartAssets() {
    }

    /**
     * Returns a translation for the given key.
     *
     * @param key translation key (must not be {@code null})
     * @return translated text, or a fallback string in the form {@code "!" + key + "!"}
     */
    public static String getTranslation(String key) {
        return getTranslation(key, Locale.getDefault());
    }

    /**
     * Returns a translation for the given key.
     *
     * <p><b>Note:</b> The {@code locale} parameter is reserved for future i18n support.
     * The current implementation ignores it and uses the same lookup as {@link #getTranslation(String)}.</p>
     *
     * @param key    translation key (must not be {@code null})
     * @param locale locale hint (currently ignored)
     * @return translated text, or a fallback string in the form {@code "!" + key + "!"}
     */
    public static String getTranslation(String key, Locale locale) {
        if (key == null || key.isBlank()) return "";

        String override = properties.getProperty(key);
        if (override != null && !override.isBlank()) return override;

        Locale loc = (locale != null) ? locale : Locale.getDefault();
        String value = lookupBundleValue(loc, key);
        if (value != null && !value.isBlank()) return value;

        Locale fallback = getFallbackLocale();
        if (!fallback.equals(loc)) {
            value = lookupBundleValue(fallback, key);
            if (value != null && !value.isBlank()) return value;
        }

        return "!" + key + "!";
    }

    /**
     * Sets a property key/value pair.
     *
     * <p>Changing a property invalidates any cached parsed values (float/int/boolean) for this key.</p>
     *
     * @param key   property key (must not be {@code null} or blank)
     * @param value property value (must not be {@code null})
     * @throws NullPointerException     if {@code key} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code key} is empty or blank
     */
    public static void setProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        properties.setProperty(key, value);
        // Invalidate cache on change
        floatCache.remove(key);
        intCache.remove(key);
        boolCache.remove(key);
        handleI18nConfigChange(key, value);
    }

    /**
     * Removes a property key.
     *
     * <p>After removal, callers will observe default values again (because the property is no longer present).
     * This method also invalidates parsed caches for the key.</p>
     *
     * <p><b>Typical use cases:</b>
     * <ul>
     *   <li>Test cleanup between runs</li>
     *   <li>Resetting user overrides back to framework defaults</li>
     * </ul>
     * </p>
     *
     * @param key property key (must not be {@code null} or blank)
     */
    public static void removeProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        properties.remove(key);
        floatCache.remove(key);
        intCache.remove(key);
        boolCache.remove(key);
        handleI18nConfigChange(key, null);
    }

    /**
     * Returns a scaled icon by name.
     *
     * <p><b>Framework note:</b> This is a stub that exists for API completeness.
     * Swing-based applications should provide their own icon loading strategy.</p>
     *
     * @param name  icon identifier
     * @param scale scaling factor (e.g., 1.0 for 100%)
     * @return the icon, or {@code null} if not available
     */
    public static javax.swing.Icon getScaledIcon(String name, float scale) {
        // Stub API: parameters are intentionally unused today.
        unused(name);
        unused(scale);
        return null;
    }

    /**
     * Returns a string property.
     *
     * @param key          property key
     * @param defaultValue value returned when the key is not present
     * @return the property value, or {@code defaultValue}
     */
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Returns a float property.
     *
     * @param key          property key
     * @param defaultValue value returned when the key is not present or cannot be parsed
     * @return parsed float value
     */
    public static float getFloat(String key, float defaultValue) {
        return floatCache.computeIfAbsent(key, k -> {
            String val = properties.getProperty(k);
            if (val == null) return defaultValue;
            try {
                return Float.parseFloat(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        });
    }

    /**
     * Returns an integer property.
     *
     * @param key          property key
     * @param defaultValue value returned when the key is not present or cannot be parsed
     * @return parsed integer value
     */
    public static int getInt(String key, int defaultValue) {
        return intCache.computeIfAbsent(key, k -> {
            String val = properties.getProperty(k);
            if (val == null) return defaultValue;
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        });
    }

    /**
     * Returns a boolean property.
     *
     * @param key          property key
     * @param defaultValue value returned when the key is not present
     * @return parsed boolean value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return boolCache.computeIfAbsent(key, k -> {
            String val = properties.getProperty(k);
            if (val == null) return defaultValue;
            return Boolean.parseBoolean(val);
        });
    }

    /**
     * Clears all cached parsed values.
     *
     * <p>This does not modify the underlying {@link Properties}; it only clears the caches so
     * subsequent reads will re-parse values.</p>
     */
    public static void clearCache() {
        floatCache.clear();
        intCache.clear();
        boolCache.clear();
        bundleCache.clear();
    }

    /**
     * Returns a color property.
     *
     * @param key          property key
     * @param defaultColor value returned when the key is not present or cannot be parsed
     * @return parsed color value
     */
    public static java.awt.Color getColor(String key, java.awt.Color defaultColor) {
        String val = properties.getProperty(key);
        if (val == null || val.isBlank()) return defaultColor;
        try {
            // Support #rrggbb and #rrggbbaa
            return java.awt.Color.decode(val);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    private static void loadDefaults() {
        try (InputStream in = ChartAssets.class.getClassLoader().getResourceAsStream("i18n/charts.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception ignored) {
            // Defaults are optional; missing resource is allowed.
        }

        String base = properties.getProperty("chart.path.i18n", "i18n.charts");
        if (base != null && !base.isBlank()) {
            i18nBaseName = base;
        }
    }

    private static String lookupBundleValue(Locale locale, String key) {
        if (i18nBaseName == null || i18nBaseName.isBlank()) return null;

        ResourceBundle bundle = bundleCache.get(locale);
        if (bundle == null) {
            try {
                bundle = ResourceBundle.getBundle(i18nBaseName, locale);
                bundleCache.put(locale, bundle);
            } catch (MissingResourceException ignored) {
                return null;
            }
        }

        return bundle.containsKey(key) ? bundle.getString(key) : null;
    }

    private static Locale getFallbackLocale() {
        String raw = properties.getProperty("chart.i18n.fallback", "en");
        if (raw == null || raw.isBlank()) return Locale.ENGLISH;
        String tag = raw.replace('_', '-');
        Locale loc = Locale.forLanguageTag(tag);
        return loc.getLanguage().isEmpty() ? Locale.ENGLISH : loc;
    }

    private static void handleI18nConfigChange(String key, String value) {
        if ("chart.path.i18n".equals(key)) {
            String base = (value != null && !value.isBlank()) ? value : "i18n.charts";
            i18nBaseName = base;
            bundleCache.clear();
            return;
        }
        if ("chart.i18n.fallback".equals(key)) {
            bundleCache.clear();
        }
    }

    // ========================================================================
    // UIManager-based lookups (for FlatLaf theme properties integration)
    // ========================================================================

    /**
     * Returns a float value from UIManager.
     *
     * @param key          UIManager key (e.g., "Chart.analysisGrid.minorAlpha")
     * @param defaultValue fallback if key is missing or not a number
     * @return the float value
     */
    public static float getUIFloat(String key, float defaultValue) {
        Object val = UIManager.get(key);
        if (val instanceof Number num) {
            return num.floatValue();
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns an int value from UIManager.
     *
     * @param key          UIManager key
     * @param defaultValue fallback if key is missing or not a number
     * @return the int value
     */
    public static int getUIInt(String key, int defaultValue) {
        Object val = UIManager.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns a Color from UIManager.
     *
     * @param key          UIManager key
     * @param defaultColor fallback if key is missing
     * @return the Color value
     */
    public static Color getUIColor(String key, Color defaultColor) {
        Color c = UIManager.getColor(key);
        return c != null ? c : defaultColor;
    }

    /**
     * Returns a boolean from UIManager.
     *
     * @param key          UIManager key
     * @param defaultValue fallback if key is missing
     * @return the boolean value
     */
    public static boolean getUIBoolean(String key, boolean defaultValue) {
        Object val = UIManager.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s && !s.isBlank()) {
            return Boolean.parseBoolean(s);
        }
        return defaultValue;
    }

    private static void unused(Object ignored) {
        // Intentionally empty. Used to document and silence unused-parameter warnings for forward-compatible APIs.
    }

    private static void unused(float ignored) {
        // Intentionally empty. Used to document and silence unused-parameter warnings for forward-compatible APIs.
    }
}
