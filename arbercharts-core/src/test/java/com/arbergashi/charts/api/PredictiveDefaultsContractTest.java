package com.arbergashi.charts.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PredictiveDefaultsContractTest {

    @Test
    void predictiveHarmonicDefaultsArePinnedInBundles() throws Exception {
        assertBundleDefaults("/i18n/charts.properties");
        assertBundleDefaults("/i18n/charts_en.properties");
    }

    private static void assertBundleDefaults(String resource) throws IOException {
        Properties props = new Properties();
        try (InputStream in = PredictiveDefaultsContractTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "Missing resource: " + resource);
            props.load(in);
        }

        assertEquals("harmonic", props.getProperty("Chart.predictive.mode"));
        assertEquals("0.26", props.getProperty("Chart.predictive.lineAlpha"));
        assertEquals("true", props.getProperty("Chart.predictive.anomaly.enabled"));
        assertEquals("2.0", props.getProperty("Chart.predictive.anomaly.sigmaFactor"));
        assertEquals("false", props.getProperty("Chart.freeze.enabled"));
        assertEquals("0.80", props.getProperty("Chart.predictive.harmonic.periodAlpha"));
        assertEquals("0.25", props.getProperty("Chart.predictive.harmonic.resetThreshold"));
    }
}
