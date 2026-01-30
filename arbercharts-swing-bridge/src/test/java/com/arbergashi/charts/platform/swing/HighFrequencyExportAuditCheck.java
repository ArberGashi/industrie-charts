package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.ViewportAuditEvent;
import com.arbergashi.charts.api.ViewportAuditEventType;
import com.arbergashi.charts.api.ViewportAuditTrail;
import com.arbergashi.charts.api.forensic.StreamBufferStrategy;
import com.arbergashi.charts.api.forensic.StreamPlaybackDrive;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.platform.export.ChartExportService;
import com.arbergashi.charts.render.standard.LineRenderer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

/**
 * Validates that audit export hashes include live stream drop counts.
 */
public final class HighFrequencyExportAuditCheck {
    private HighFrequencyExportAuditCheck() {
    }

    public static void main(String[] args) throws Exception {
        DefaultChartModel model = new DefaultChartModel();
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());

        ViewportAuditTrail trail = new ViewportAuditTrail(4);
        trail.onEvent(new ViewportAuditEvent(
                ViewportAuditEventType.PAN,
                Instant.now(),
                0.0,
                1.0,
                0.0,
                1.0,
                Map.of("test", "audit")
        ));
        panel.setAuditLogger(trail);

        StubStreamDrive drive = new StubStreamDrive(0);
        panel.setStreamPlaybackDrive(drive);
        String hashZero = extractAuditHash(panel);

        drive.setDroppedCount(42);
        String hashDropped = extractAuditHash(panel);

        System.out.println("hash(drop=0)  : " + hashZero);
        System.out.println("hash(drop=42) : " + hashDropped);
        System.out.println("hashChanged   : " + !hashZero.equals(hashDropped));
    }

    private static String extractAuditHash(ArberChartPanel panel) throws Exception {
        Method method = ChartExportService.class.getDeclaredMethod("extractAuditHash", ArberChartPanel.class);
        method.setAccessible(true);
        return (String) method.invoke(null, panel);
    }

    private static final class StubStreamDrive implements StreamPlaybackDrive {
        private long dropped;

        private StubStreamDrive(long dropped) {
            this.dropped = dropped;
        }

        @Override
        public void connect(String source) {
        }

        @Override
        public StreamPlaybackDrive setCapacity(int capacity) {
            return this;
        }

        @Override
        public int getCapacity() {
            return 0;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public StreamPlaybackDrive setBufferStrategy(StreamBufferStrategy strategy) {
            return this;
        }

        @Override
        public StreamBufferStrategy getBufferStrategy() {
            return StreamBufferStrategy.DROP_OLDEST;
        }

        @Override
        public long getDroppedCount() {
            return dropped;
        }

        public void setDroppedCount(long dropped) {
            this.dropped = dropped;
        }

        @Override
        public boolean offer(double x, double y, double min, double max, double weight,
                             String label, byte flag, short sourceId, long timestampNanos) {
            return true;
        }

        @Override
        public void close() {
        }
    }
}
