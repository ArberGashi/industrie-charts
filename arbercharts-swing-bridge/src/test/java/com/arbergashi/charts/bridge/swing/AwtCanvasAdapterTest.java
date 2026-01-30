package com.arbergashi.charts.bridge.swing;

import com.arbergashi.charts.api.types.ArberColor;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwtCanvasAdapterTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void lineToDrawsLineFromLastMove() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        AwtCanvasAdapter canvas = new AwtCanvasAdapter(g2);
        canvas.setColor(new ArberColor(0xFFFF0000));
        canvas.setStroke(1f);
        canvas.moveTo(1f, 1f);
        canvas.lineTo(8f, 1f);

        g2.dispose();

        int red = 0xFFFF0000;
        int hitCount = 0;
        for (int x = 1; x <= 8; x++) {
            if (image.getRGB(x, 1) == red) {
                hitCount++;
            }
        }
        assertTrue(hitCount >= 4, "Expected multiple red pixels on the line");
        assertEquals(red, image.getRGB(1, 1));
        assertEquals(red, image.getRGB(8, 1));
    }
}
