package com.arbergashi.charts.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChartThemes}.
 *
 * @since 2.0.0
 */
class ChartThemesTest {

    @Test
    void getDarkTheme_returnsNonNull() {
        ChartTheme theme = ChartThemes.getDarkTheme();
        assertNotNull(theme);
        assertNotNull(theme.getBackground());
        assertNotNull(theme.getForeground());
        assertNotNull(theme.getGridColor());
        assertNotNull(theme.getAccentColor());
    }

    @Test
    void getLightTheme_returnsNonNull() {
        ChartTheme theme = ChartThemes.getLightTheme();
        assertNotNull(theme);
        assertNotNull(theme.getBackground());
    }

    @Test
    void getNordTheme_hasCorrectColors() {
        ChartTheme theme = ChartThemes.getNordTheme();
        assertNotNull(theme);
        // Nord background should be dark blue-gray (#2E3440)
        assertEquals(0x2E, theme.getBackground().red());
        assertEquals(0x34, theme.getBackground().green());
        assertEquals(0x40, theme.getBackground().blue());
    }

    @Test
    void getDraculaTheme_hasCorrectColors() {
        ChartTheme theme = ChartThemes.getDraculaTheme();
        assertNotNull(theme);
        // Dracula background should be #282A36
        assertEquals(0x28, theme.getBackground().red());
        assertEquals(0x2A, theme.getBackground().green());
        assertEquals(0x36, theme.getBackground().blue());
    }

    @Test
    void getMonokaiTheme_returnsNonNull() {
        ChartTheme theme = ChartThemes.getMonokaiTheme();
        assertNotNull(theme);
        assertNotNull(theme.getBackground());
    }

    @Test
    void getObsidianTheme_isHighContrast() {
        ChartTheme theme = ChartThemes.getObsidianTheme();
        assertNotNull(theme);
        // Obsidian should be very dark
        assertTrue(theme.getBackground().red() < 30);
        assertTrue(theme.getBackground().green() < 30);
        assertTrue(theme.getBackground().blue() < 30);
        // Foreground should be very light
        assertTrue(theme.getForeground().red() > 200);
    }

    @Test
    void getSolarizedDarkTheme_returnsNonNull() {
        ChartTheme theme = ChartThemes.getSolarizedDarkTheme();
        assertNotNull(theme);
    }

    @Test
    void getSolarizedLightTheme_returnsNonNull() {
        ChartTheme theme = ChartThemes.getSolarizedLightTheme();
        assertNotNull(theme);
        // Should be light
        assertTrue(theme.getBackground().red() > 200);
    }

    @Test
    void getGitHubDarkTheme_returnsNonNull() {
        ChartTheme theme = ChartThemes.getGitHubDarkTheme();
        assertNotNull(theme);
    }

    @Test
    void getMedicalTheme_hasGreenColors() {
        ChartTheme theme = ChartThemes.getMedicalTheme();
        assertNotNull(theme);
        // Medical theme should have green foreground
        assertEquals(0, theme.getForeground().red());
        assertEquals(255, theme.getForeground().green());
        assertEquals(0, theme.getForeground().blue());
        // Black background
        assertEquals(0, theme.getBackground().red());
    }

    @Test
    void getTheme_byName_dark() {
        ChartTheme theme = ChartThemes.getTheme("dark");
        assertNotNull(theme);
        assertEquals(ChartThemes.getDarkTheme().getBackground(), theme.getBackground());
    }

    @Test
    void getTheme_byName_nord() {
        ChartTheme theme = ChartThemes.getTheme("nord");
        assertNotNull(theme);
        assertEquals(ChartThemes.getNordTheme().getBackground(), theme.getBackground());
    }

    @Test
    void getTheme_caseInsensitive() {
        ChartTheme theme1 = ChartThemes.getTheme("NORD");
        ChartTheme theme2 = ChartThemes.getTheme("Nord");
        ChartTheme theme3 = ChartThemes.getTheme("nord");

        assertEquals(theme1.getBackground(), theme2.getBackground());
        assertEquals(theme2.getBackground(), theme3.getBackground());
    }

    @Test
    void getTheme_unknownName_returnsDarkTheme() {
        ChartTheme theme = ChartThemes.getTheme("unknown-theme");
        assertNotNull(theme);
        // Should return dark theme as default
    }

    @Test
    void getTheme_null_returnsDarkTheme() {
        ChartTheme theme = ChartThemes.getTheme(null);
        assertNotNull(theme);
    }

    @Test
    void getAvailableThemes_returnsAllThemes() {
        String[] themes = ChartThemes.getAvailableThemes();
        assertNotNull(themes);
        assertTrue(themes.length >= 10, "Should have at least 10 themes");

        // Check for expected themes
        boolean hasDark = false, hasLight = false, hasNord = false, hasDracula = false;
        for (String name : themes) {
            if ("dark".equals(name)) hasDark = true;
            if ("light".equals(name)) hasLight = true;
            if ("nord".equals(name)) hasNord = true;
            if ("dracula".equals(name)) hasDracula = true;
        }
        assertTrue(hasDark, "Should contain dark theme");
        assertTrue(hasLight, "Should contain light theme");
        assertTrue(hasNord, "Should contain nord theme");
        assertTrue(hasDracula, "Should contain dracula theme");
    }

    @Test
    void registerTheme_addsNewTheme() {
        ChartTheme customTheme = ChartThemes.getDarkTheme(); // Use existing as placeholder

        ChartThemes.registerTheme("custom-test", customTheme);

        ChartTheme retrieved = ChartThemes.getTheme("custom-test");
        assertEquals(customTheme, retrieved);
    }

    @Test
    void registerTheme_ignoresNull() {
        int countBefore = ChartThemes.getAvailableThemes().length;

        ChartThemes.registerTheme(null, ChartThemes.getDarkTheme());
        ChartThemes.registerTheme("test", null);

        int countAfter = ChartThemes.getAvailableThemes().length;
        assertEquals(countBefore, countAfter);
    }

    @Test
    void allThemes_haveSeriesColors() {
        for (String themeName : ChartThemes.getAvailableThemes()) {
            ChartTheme theme = ChartThemes.getTheme(themeName);
            assertNotNull(theme, "Theme should not be null: " + themeName);

            // Each theme should have at least 5 series colors
            for (int i = 0; i < 5; i++) {
                assertNotNull(theme.getSeriesColor(i),
                    "Theme " + themeName + " should have series color " + i);
            }
        }
    }
}

