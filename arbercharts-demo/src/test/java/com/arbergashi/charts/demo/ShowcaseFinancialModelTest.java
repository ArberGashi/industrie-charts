package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.DefaultFinancialChartModel;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ShowcaseFinancialModelTest {
    @Test
    void liveUpdatesKeepHistoryAndSizeStableWhilePublishingEveryNewBarValue() {
        DefaultFinancialChartModel source = new DefaultFinancialChartModel("Quotes");
        source.setOHLC(1, 80, 85, 78, 82, 1_000, "History");
        source.setOHLC(2, 82, 90, 81, 86, 2_000, "Live");
        source.getProvenanceFlagsData()[1] = 3;
        source.getSourceIdsData()[1] = 42;
        source.getTimestampNanosData()[1] = 1234;
        ShowcaseFinancialModel model = new ShowcaseFinancialModel(source);
        long stamp = model.getUpdateStamp();
        AtomicInteger notifications = new AtomicInteger();
        model.setChangeListener(notifications::incrementAndGet);
        for (int tick = 0; tick < 10_000; tick++) {
            model.updateLiveBar(82, 92, 79, 87 + Math.sin(tick * 0.01), 2_500 + tick);
        }
        assertEquals(2, model.getPointCount());
        assertEquals(1, model.getX(0));
        assertEquals(80, model.getOpen(0));
        assertEquals(85, model.getHigh(0));
        assertEquals(78, model.getLow(0));
        assertEquals(82, model.getClose(0));
        assertEquals(1_000, model.getVolume(0));
        assertEquals("History", model.getLabel(0));
        assertEquals(2, model.getX(1));
        assertEquals(82, model.getOpen(1));
        assertEquals(92, model.getHigh(1));
        assertEquals(79, model.getLow(1));
        assertEquals(87 + Math.sin(9_999 * 0.01), model.getClose(1), 1e-12);
        assertEquals(model.getClose(1), model.getY(1));
        assertNotEquals(source.getClose(1), model.getClose(1));
        assertEquals(12_499, model.getVolume(1));
        assertEquals("Live", model.getLabel(1));
        assertEquals(3, model.getProvenanceFlag(1));
        assertEquals(42, model.getSourceId(1));
        assertEquals(1234, model.getTimestampNanos(1));
        assertEquals(10_000, notifications.get());
        assertEquals(stamp + 10_000, model.getUpdateStamp());
        assertEquals(86, source.getClose(1), "The copied demo history must not mutate its source");
    }

    @Test
    void anEmptyHistoryDoesNotInventALiveBarOrPublishAnUpdate() {
        ShowcaseFinancialModel model = new ShowcaseFinancialModel(new DefaultFinancialChartModel("Empty"));
        long stamp = model.getUpdateStamp();
        AtomicInteger notifications = new AtomicInteger();
        model.setChangeListener(notifications::incrementAndGet);
        model.updateLiveBar(1, 2, 0, 1, 3);
        assertEquals(0, model.getPointCount());
        assertEquals(stamp, model.getUpdateStamp());
        assertEquals(0, notifications.get());
    }
}
