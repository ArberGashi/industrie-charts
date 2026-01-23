package com.arbergashi.charts.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internationalization (public API) facade.
 * <p>
 * Note: the underlying bundle/fallback logic is implemented in {@link ChartAssets}.
 * This class exists to provide a convenient developer experience.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartI18N {

    private static final Logger LOGGER = Logger.getLogger(ChartI18N.class.getName());
    private static volatile Locale defaultLocale;

    private ChartI18N() {
    }

    /**
     * Returns the localized string for the current default locale.
     */
    public static String getString(String key) {
        return getString(key, resolveLocale(null));
    }

    public static String getString(String key, Locale locale) {
        if (key == null || key.isBlank()) return "";
        Locale loc = resolveLocale(locale);
        try {
            return ChartAssets.getTranslation(key, loc);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Key missing: {0} for Locale: {1}", new Object[]{key, locale});
            return "!" + key + "!";
        }
    }

    /**
     * Specialized helper for SVG icon keys.
     */
    public static String getIconPath(String rendererKey) {
        return getString("icon." + rendererKey);
    }

    public static String format(String key, Object... args) {
        return format(key, resolveLocale(null), args);
    }

    public static String format(String key, Locale locale, Object... args) {
        String pattern = getString(key, locale);
        try {
            /* MessageFormat is relatively expensive; however format() is typically not called per-pixel. */
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    /**
     * Cache flush helper. Delegates to ChartAssets for consistent reload/theme-switch behavior.
     */
    public static void flushCache() {
        ChartAssets.clearCache();
    }

    /**
     * Sets a framework-wide default locale for i18n resolution.
     *
     * <p>When set, this locale is used by {@link #getString(String)} and {@link #format(String, Object...)}
     * unless a locale is provided explicitly.</p>
     *
     * @param locale default locale to use (null resets to JVM default)
     */
    public static void setDefaultLocale(Locale locale) {
        defaultLocale = locale;
    }

    /**
     * Returns the framework default locale override, or null when not set.
     *
     * @return the explicit default locale or null
     */
    public static Locale getDefaultLocale() {
        return defaultLocale;
    }

    private static Locale resolveLocale(Locale locale) {
        if (locale != null) return locale;
        if (defaultLocale != null) return defaultLocale;
        return Locale.getDefault();
    }
}
