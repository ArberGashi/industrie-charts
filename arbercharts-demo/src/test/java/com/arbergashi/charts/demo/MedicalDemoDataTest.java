package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalDemoDataTest {
    @Test
    void vectorcardiogramHasAClosedLeadProjectionInsteadOfATimeTrace() {
        assertVcgLoop(model("VCGRenderer"));
    }

    @Test
    void spirometryHasDistinctExpiratoryAndInspiratoryBranches() {
        assertSpirometryLoop(model("SpirometryRenderer"));
    }

    static void assertVcgLoop(ChartModel model) {
        assertClosedContinuousLoop(model, 3, 2.2);
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean[] quadrants = new boolean[4];
        for (int i = 0; i < model.getPointCount(); i++) {
            double x = model.getX(i), y = model.getY(i);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            quadrants[(x >= 0 ? 2 : 0) + (y >= 0 ? 1 : 0)] = true;
        }
        assertTrue(minX < -0.5 && maxX > 0.5 && minY < -0.4 && maxY > 0.4,
                "Both projected leads must move in positive and negative directions");
        for (boolean occupied : quadrants) assertTrue(occupied, "The projected loop must span all four quadrants");
        assertTrue(enclosedArea(model) > 1.0, "A VCG projection must enclose a visible two-dimensional area");
    }

    static void assertSpirometryLoop(ChartModel model) {
        assertClosedContinuousLoop(model, 5, 13);
        double maxFlow = 0, minFlow = 0, peakVolume = 0, maxVolume = 0;
        int expiration = 0, inspiration = 0;
        for (int i = 0; i < model.getPointCount(); i++) {
            double volume = model.getX(i), flow = model.getY(i);
            assertTrue(volume >= -1e-9 && volume <= 4.6, "Volume must remain in the synthetic litre domain");
            maxVolume = Math.max(maxVolume, volume);
            if (flow > maxFlow) {
                maxFlow = flow;
                peakVolume = volume;
            }
            minFlow = Math.min(minFlow, flow);
            if (i > 0 && flow > 0.05) {
                expiration++;
                assertTrue(volume >= model.getX(i - 1), "Positive expiration follows increasing exhaled volume");
            }
            if (i > 0 && flow < -0.05) {
                inspiration++;
                assertTrue(volume <= model.getX(i - 1), "Negative inspiration returns along decreasing volume");
            }
        }
        assertTrue(maxVolume > 4.4 && maxFlow > 6 && minFlow < -3,
                "The loop requires a full volume excursion and opposite flow branches");
        assertTrue(peakVolume > 0.1 && peakVolume < 1.5, "The expiratory flow peak must occur early in expiration");
        assertTrue(expiration > 600 && inspiration > 600, "Both flow branches must contain sustained curves");
        assertTrue(enclosedArea(model) > 15, "The flow-volume loop must enclose an area, not retrace one line");
    }

    private static void assertClosedContinuousLoop(ChartModel model, double xSpan, double ySpan) {
        int last = model.getPointCount() - 1;
        assertEquals(model.getX(0), model.getX(last), 1e-8, "The measurement loop must close in X");
        assertEquals(model.getY(0), model.getY(last), 1e-8, "The measurement loop must close in Y");
        for (int i = 1; i <= last; i++) {
            double dx = (model.getX(i) - model.getX(i - 1)) / xSpan;
            double dy = (model.getY(i) - model.getY(i - 1)) / ySpan;
            assertTrue(Math.hypot(dx, dy) < 0.03, "Adjacent loop samples must not contain a wrap chord");
        }
    }

    private static double enclosedArea(ChartModel model) {
        double twiceArea = 0;
        for (int i = 1; i < model.getPointCount(); i++) {
            twiceArea += model.getX(i - 1) * model.getY(i) - model.getX(i) * model.getY(i - 1);
        }
        return Math.abs(twiceArea) * 0.5;
    }

    @Test
    void eegContainsDistinctSustainedOscillationsAcrossMultipleChannels() {
        CircularFastMedicalModel model = model("EEGRenderer");
        assertTrue(model.getChannelCount() >= 4, "The EEG showcase needs several actual channels");
        var peaks = new HashSet<Integer>();
        for (int channel = 0; channel < model.getChannelCount(); channel++) {
            double[] values = model.getRawChannelArray(channel);
            int crossings = zeroCrossings(values);
            assertTrue(crossings >= 80 && crossings <= 170, "EEG activity must oscillate throughout the six-second window");
            assertTrue(rms(values, 0, values.length) > 10 && rms(values, 0, values.length) < 40);
            int dominant = dominantFrequency(values, 1, 30);
            assertTrue(dominant >= 8 && dominant <= 13, "The illustrative EEG channel must contain an 8–13 Hz rhythm");
            assertTrue(peaks.add(dominant), "The demo channels must have distinguishable dominant rhythms");
        }
    }

    @Test
    void emgContainsHighFrequencyActivityBursts() {
        double[] values = model("EMGRenderer").getRawChannelArray(0);
        assertTrue(zeroCrossings(values) > 400, "EMG must show dense high-frequency activity, not ECG spikes");
        assertTrue(dominantFrequency(values, 1, 110) >= 30);
        assertTrue(rms(values, 250, 450) > 4 * rms(values, 0, 125), "The activity burst must exceed the resting background");
    }

    @Test
    void eogContainsPositiveAndNegativeGazePlateaus() {
        double[] values = model("EOGRenderer").getRawChannelArray(0);
        long positive = java.util.Arrays.stream(values).filter(v -> v > 0.55).count();
        long negative = java.util.Arrays.stream(values).filter(v -> v < -0.55).count();
        assertTrue(positive > 500 && negative > 500, "The gaze steps require sustained plateaus of both signs");
        assertTrue(zeroCrossings(values) <= 6, "Gaze motion must remain slow compared with ECG or EMG activity");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PPGRenderer", "IBPRenderer"})
    void pulseDemosHaveBroadPulsesAboveTheirAppropriateBaseline(String renderer) {
        double[] values = model(renderer).getRawChannelArray(0);
        double baseline = renderer.equals("IBPRenderer") ? 76 : 0;
        double amplitude = renderer.equals("IBPRenderer") ? 44 : 1;
        int elevated = 0, peaks = 0;
        boolean high = false;
        for (double value : values) {
            assertTrue(value >= baseline - 1e-9 && value <= baseline + amplitude + 1e-9);
            if (value > baseline + amplitude * 0.5) elevated++;
            boolean next = value > baseline + amplitude * 0.85;
            if (next && !high) peaks++;
            high = next;
        }
        assertTrue(peaks >= 6 && peaks <= 8, "The six-second sample must contain repeated complete pulses");
        assertTrue(elevated > 300, "Pulse waves must have a sustained systolic component, not narrow ECG R spikes");
    }

    @Test
    void capnographyContainsAnExpiratoryPlateauAndAZeroBaseline() {
        double[] values = model("CapnographyRenderer").getRawChannelArray(0);
        assertTrue(java.util.Arrays.stream(values).filter(v -> v < 0.1).count() > 500);
        assertTrue(java.util.Arrays.stream(values).filter(v -> v >= 35).count() > 500,
                "The capnogram requires a broad expiratory plateau in mmHg");
        for (double value : values) assertTrue(value >= 0 && value <= 40);
    }

    @Test
    void nirsContainsASlowPercentageTrend() {
        double[] values = model("NIRSRenderer").getRawChannelArray(0);
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            assertTrue(values[i] >= 60 && values[i] <= 80);
            if (i > 0) assertTrue(Math.abs(values[i] - values[i - 1]) < 0.02, "NIRS must remain a smooth trend");
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
        }
        assertTrue(max - min > 2, "The trend must vary visibly within its percentage domain");
    }

    @Test
    void ventilatorChannelsRepresentNormalizedPressureVolumeAndFlow() {
        CircularFastMedicalModel model = model("VentilatorWaveformRenderer");
        assertEquals(3, model.getChannelCount());
        assertTrue(model.getName().contains("normalized"), "Mixed physical quantities must explicitly use normalized units");
        int inflating = 0, deflating = 0;
        for (int i = 1; i < model.getPointCount(); i++) {
            double pressure = model.getY(i, 0), volume = model.getY(i, 1), flow = model.getY(i, 2);
            assertTrue(pressure >= 0.19 && pressure <= 0.86);
            assertTrue(volume >= 0 && volume <= 1.01);
            if (flow > 0.5 && volume > model.getY(i - 1, 1)) inflating++;
            if (flow < -0.1 && volume < model.getY(i - 1, 1)) deflating++;
        }
        assertTrue(inflating > 500 && deflating > 350, "Flow direction must agree with increasing and decreasing volume");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SpectrogramMedicalRenderer", "UltrasoundMModeRenderer"})
    void intensityViewsSupplyFrequencyOrDepthSeparatelyFromTheirPaletteValue(String renderer) {
        CircularFastMedicalModel model = model(renderer);
        assertEquals(2, model.getChannelCount());
        double minIntensity = 255, maxIntensity = 0;
        for (int i = 0; i < model.getPointCount(); i++) {
            double coordinate = model.getY(i, 0), intensity = model.getY(i, 1);
            assertTrue(coordinate > 5 && coordinate < 50, "The first channel is positive frequency or depth");
            assertTrue(intensity >= 0 && intensity <= 255, "The second channel must address the intensity palette");
            minIntensity = Math.min(minIntensity, intensity);
            maxIntensity = Math.max(maxIntensity, intensity);
        }
        assertTrue(maxIntensity - minIntensity > 150, "Intensity must vary meaningfully instead of reusing an ECG lead");
        double[] range = model.getDataRange();
        assertEquals(0, range[2]);
        assertTrue(range[3] > 20 && range[3] < 50, "Only frequency or depth may determine the Y viewport");
        for (int i = 0; i < model.getPointCount(); i++) assertTrue(model.getY(i, 0) <= range[3]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CalibrationRenderer", "ECGRenderer", "ECGRhythmRenderer", "MedicalSweepRenderer", "SweepEraseEKGRenderer"})
    void ecgShowcasesRetainTheirNarrowRPeaks(String renderer) {
        CircularFastMedicalModel model = model(renderer);
        assertEquals(3, model.getChannelCount());
        double[] values = model.getRawChannelArray(0);
        long high = java.util.Arrays.stream(values).filter(v -> v > 0.7).count();
        assertTrue(high >= 10 && high < 100, "Only the ECG group should retain narrow R-peak morphology");
    }

    private static CircularFastMedicalModel model(String renderer) {
        return assertInstanceOf(CircularFastMedicalModel.class,
                RendererDemoDataFactory.build("medical", "com.arbergashi.charts.render.medical." + renderer));
    }

    private static int zeroCrossings(double[] values) {
        int count = 0;
        for (int i = 1; i < values.length; i++) if ((values[i] >= 0) != (values[i - 1] >= 0)) count++;
        return count;
    }

    private static double rms(double[] values, int start, int end) {
        double squares = 0;
        for (int i = start; i < end; i++) squares += values[i] * values[i];
        return Math.sqrt(squares / (end - start));
    }

    private static int dominantFrequency(double[] values, int first, int last) {
        double strongest = -1;
        int peak = -1;
        for (int frequency = first; frequency <= last; frequency++) {
            double real = 0, imaginary = 0;
            for (int i = 0; i < values.length; i++) {
                double angle = 2 * Math.PI * frequency * i / 250.0;
                real += values[i] * Math.cos(angle);
                imaginary += values[i] * Math.sin(angle);
            }
            double power = real * real + imaginary * imaginary;
            if (power > strongest) {
                strongest = power;
                peak = frequency;
            }
        }
        return peak;
    }
}
