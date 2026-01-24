package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.LiveFFTRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

import java.util.Random;

public class FFTPanelProvider {
    public static ArberChartPanel create() {
        // Professional spectrum demo:
        // Rotating-machine vibration spectrum (10 kHz sampling / 0..5 kHz band):
        // - fundamental + harmonics
        // - sidebands around the fundamental (modulation)
        // - a narrow interference peak
        // - realistic tilted noise floor
        // Deterministic dataset for stable QA/screenshot output.

        DefaultChartModel model = new DefaultChartModel("Magnitude (normalized)");

        int bins = 1024;
        double maxFreq = 5000.0;
        double binWidth = maxFreq / bins;

        Random rnd = new Random(1337);

        double f0 = 312.5;      // shaft rotation
        double mod = 18.0;      // modulation (sidebands)
        double interference = 1000.0;
        double interference2 = 1780.0;

        for (int bin = 0; bin < bins; bin++) {
            double f = bin * binWidth;

            // Tilted noise floor with a slight low-frequency emphasis.
            double floor = 0.045 + 0.02 * rnd.nextDouble();
            if (f > 20.0) {
                floor += 0.12 / Math.sqrt(f / 120.0);
            }

            double mag = floor;

            // Fundamental + harmonics.
            mag += peak(f, f0, 1.00, 6.5);
            mag += peak(f, 2 * f0, 0.55, 7.5);
            mag += peak(f, 3 * f0, 0.32, 8.5);
            mag += peak(f, 4 * f0, 0.22, 9.5);

            // Sidebands around fundamental (bearing modulation).
            mag += peak(f, f0 - mod, 0.18, 4.0);
            mag += peak(f, f0 + mod, 0.18, 4.0);
            mag += peak(f, 2 * f0 - mod, 0.10, 4.2);
            mag += peak(f, 2 * f0 + mod, 0.10, 4.2);

            // A narrow interference line (e.g., switching supply / EMI).
            mag += peak(f, interference, 0.12, 2.0);
            mag += peak(f, interference2, 0.08, 3.0);

            // Broad resonance bump.
            mag += peak(f, 2400.0, 0.10, 170.0);

            String label;
            if (Math.abs(f - f0) < 2 * binWidth) label = String.format("%.1f Hz (fundamental)", f);
            else if (Math.abs(f - 2 * f0) < 2 * binWidth) label = String.format("%.1f Hz (2× harmonic)", f);
            else if (Math.abs(f - 3 * f0) < 2 * binWidth) label = String.format("%.1f Hz (3× harmonic)", f);
            else if (Math.abs(f - interference) < 2 * binWidth) label = String.format("%.1f Hz (interference)", f);
            else if (Math.abs(f - interference2) < 2 * binWidth) label = String.format("%.1f Hz (harmonic noise)", f);
            else label = String.format("%.1f Hz", f);

            model.addPoint(f, mag, 0, label);
        }

        LiveFFTRenderer renderer = new LiveFFTRenderer();
        renderer.setAsBars(false); // line is easier to read and looks more professional

        return ArberChartBuilder.create()
                .withTitle("FFT Spectrum – Rotating Machine Vibration")
                // Grid is enforced centrally by DemoPanelFactory (AnalysisGridLayer).
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }

    private static double peak(double x, double center, double amplitude, double sigma) {
        double d = x - center;
        return amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
    }
}
