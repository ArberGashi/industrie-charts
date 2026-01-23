package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.CapnographyRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality Capnography demonstration panel.
 * <p>
 * Generates realistic end-tidal CO2 (ETCO2) waveforms showing the characteristic
 * capnogram phases. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class CapnographyPanelProvider {

    private static final int CAPACITY = 900;
    private static final int SAMPLE_RATE = 50; // Hz
    private static final double RESPIRATORY_RATE = 14.0; // breaths per minute
    private static final double ETCO2_NORMAL = 38.0; // mmHg

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("EtCO2");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 34);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("Capnography - EtCO2 Waveform")
                .addLayer(model, new CapnographyRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        final double[] time = {0};

        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = Math.max(1, SAMPLE_RATE / 60);
            for (int i = 0; i < samplesPerFrame; i++) {
                double rate = RESPIRATORY_RATE * (1.0 + 0.035 * Math.sin(time[0] * 0.05 * 2 * Math.PI));
                double breathPeriod = 60.0 / rate;
                double phase = (time[0] % breathPeriod) / breathPeriod;

                // I:E ratio typically 1:2
                double inspirationEnd = 0.33;

                double co2;
                if (phase < 0.05) {
                    // Phase 0: End of inspiration, baseline (near zero)
                    co2 = 1.0 + rand.nextDouble() * 0.5;
                } else if (phase < inspirationEnd) {
                    // Still inspiration - dead space gas
                    co2 = 1.0 + rand.nextDouble() * 0.5;
                } else if (phase < inspirationEnd + 0.05) {
                    // Phase I: Beginning of expiration (dead space washout)
                    double t = (phase - inspirationEnd) / 0.05;
                    co2 = 1.0 + t * 5.0;
                } else if (phase < inspirationEnd + 0.15) {
                    // Phase II: Rapid rise (mixing of dead space and alveolar gas)
                    double t = (phase - inspirationEnd - 0.05) / 0.10;
                    double etco2 = ETCO2_NORMAL + 1.8 * Math.sin(time[0] * 0.03);
                    co2 = 6.0 + t * (etco2 - 8.0);
                } else if (phase < 0.85) {
                    // Phase III: Alveolar plateau (slight upslope is normal)
                    double t = (phase - inspirationEnd - 0.15) / (0.85 - inspirationEnd - 0.15);
                    co2 = ETCO2_NORMAL - 2.5 + t * 4.5; // Slight upslope
                } else {
                    // Phase IV: Rapid descent (inspiration begins)
                    double t = (phase - 0.85) / 0.15;
                    co2 = ETCO2_NORMAL + 2.0 - t * (ETCO2_NORMAL + 1.0);
                }

                // Small baseline variation
                co2 += rand.nextDouble() * 0.3 - 0.15;
                co2 = Math.max(0, co2);

                model.add(time[0], new double[]{co2});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
