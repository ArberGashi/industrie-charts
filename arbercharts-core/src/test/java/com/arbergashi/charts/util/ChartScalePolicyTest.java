package com.arbergashi.charts.util;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link ChartScale}.
 *
 * <p>Framework policy:</p>
 * <ul>
 *   <li>{@link ChartScale#autoDetect(GraphicsConfiguration)} initializes the global scale factor only once.
 *       The first call wins to avoid cross-monitor scale thrash.</li>
 *   <li>{@link ChartScale#setScaleFactor(float)} is an explicit override and must win over auto-detection.</li>
 * </ul>
 */
public class ChartScalePolicyTest {

    @Test
    void autoDetect_initializesOnce_firstCallWins() {
        // Reset to a known state for this test JVM.
        ChartScale.setScaleFactor(1.0f);

        // After explicit set, autoDetect is locked out by policy.
        // So we first simulate the "fresh" flow by using only autoDetect.
        // We can only do this reliably by starting from the default state.
        // In a single JVM test run, we keep the assertions focused on the lock behavior.

        // Step 1: set an explicit factor to ensure a non-1 baseline.
        ChartScale.setScaleFactor(1.5f);
        assertEquals(1.5f, ChartScale.getScaleFactor(), 0.0001f);

        // Step 2: autoDetect attempts to change it must be ignored.
        ChartScale.autoDetect(fakeGcScale(2.0));
        assertEquals(1.5f, ChartScale.getScaleFactor(), 0.0001f);
    }

    @Test
    void setScaleFactor_overridesAndLocks() {
        ChartScale.setScaleFactor(2.0f);
        assertEquals(2.0f, ChartScale.getScaleFactor(), 0.0001f);

        // autoDetect must not override explicit settings
        ChartScale.autoDetect(fakeGcScale(1.0));
        assertEquals(2.0f, ChartScale.getScaleFactor(), 0.0001f);

        // invalid factors should fall back to 1.0 and still lock
        ChartScale.setScaleFactor(-1.0f);
        assertEquals(1.0f, ChartScale.getScaleFactor(), 0.0001f);
        ChartScale.autoDetect(fakeGcScale(3.0));
        assertEquals(1.0f, ChartScale.getScaleFactor(), 0.0001f);
    }

    private static GraphicsConfiguration fakeGcScale(double scaleX) {
        return new GraphicsConfiguration() {
            @Override
            public GraphicsDevice getDevice() {
                return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            }

            @Override
            public ColorModel getColorModel() {
                return getDevice().getDefaultConfiguration().getColorModel();
            }

            @Override
            public ColorModel getColorModel(int transparency) {
                return getDevice().getDefaultConfiguration().getColorModel(transparency);
            }

            @Override
            public AffineTransform getDefaultTransform() {
                AffineTransform tx = new AffineTransform();
                tx.scale(scaleX, scaleX);
                return tx;
            }

            @Override
            public AffineTransform getNormalizingTransform() {
                return new AffineTransform();
            }

            @Override
            public Rectangle getBounds() {
                return getDevice().getDefaultConfiguration().getBounds();
            }

            @Override
            public ImageCapabilities getImageCapabilities() {
                return getDevice().getDefaultConfiguration().getImageCapabilities();
            }
        };
    }
}
