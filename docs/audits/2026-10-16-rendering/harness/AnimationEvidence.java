package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class AnimationEvidence {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultChartModel model = new DefaultChartModel("Signal");
                for (int i = 0; i <= 100; i++) model.setXY(i, Math.sin(i * 0.13) * 12 + Math.cos(i * 0.035) * 8);
                AtomicLong clock = new AtomicLong();
                ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer(), clock::get);
                panel.setTheme(ChartThemes.getLightTheme());
                panel.setLegend(false);
                panel.setSize(600, 330);
                panel.addNotify();
                panel.doLayout();
                panel.getDebugContext();
                JComponent canvas = panel.getOverlayCanvasForTesting();
                canvas.dispatchEvent(new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, 1, 0,
                        360, 160, 360, 160, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -6, -6.0));
                BufferedImage sheet = new BufferedImage(1240, 780, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = sheet.createGraphics();
                try {
                    graphics.setColor(new Color(237, 240, 244));
                    graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
                    graphics.setColor(new Color(40, 52, 66));
                    graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                    graphics.drawString("Swing zoom — actual EDT frames at controlled elapsed times", 20, 28);
                    for (int frame = 0; frame < 4; frame++) {
                        clock.set(frame * 60_000_000L);
                        panel.advanceZoomAnimation(clock.get());
                        BufferedImage image = new BufferedImage(600, 330, BufferedImage.TYPE_INT_RGB);
                        Graphics2D chartGraphics = image.createGraphics();
                        try { panel.printAll(chartGraphics); } finally { chartGraphics.dispose(); }
                        int x = 20 + (frame % 2) * 610;
                        int y = 72 + (frame / 2) * 354;
                        graphics.setColor(new Color(40, 52, 66));
                        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                        graphics.drawString(frame * 60 + " ms", x, y - 8);
                        graphics.drawImage(image, x, y, null);
                    }
                } finally {
                    graphics.dispose();
                    panel.removeNotify();
                }
                ImageIO.write(sheet, "png", new File(args[0]));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }
}
