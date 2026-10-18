package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CircularFastMedicalModelTest {

    @Test
    public void addWrapsAndRawArrayIsStable() {
        CircularFastMedicalModel m = new CircularFastMedicalModel(4, 2);
        assertEquals(0, m.getRawSize());
        m.add(0.0, new double[]{1.0, 2.0});
        m.add(1.0, new double[]{2.0, 3.0});
        m.add(2.0, new double[]{3.0, 4.0});
        m.add(3.0, new double[]{4.0, 5.0});
        assertEquals(4, m.getRawSize());
        // add beyond capacity -> wraps
        m.add(4.0, new double[]{5.0, 6.0});
        assertEquals(4, m.getRawSize());
        // getRawChannelArray returns backing array reference
        double[] ch0 = m.getRawChannelArray(0);
        double[] ch1 = m.getRawChannelArray(1);
        assertNotNull(ch0);
        assertNotNull(ch1);
        assertTrue(ch0.length >= 4);
        // getY and getYRaw consistency for recent point
        int head = m.getRawHeadIndex();
        int lastIndex = (head - 1 + m.getRawCapacity()) % m.getRawCapacity();
        assertEquals(m.getYRaw(lastIndex, 0), m.getY(m.getRawSize() - 1, 0));
    }

    @Test
    public void sweepIndexAndSizeBehavior() {
        CircularFastMedicalModel m = new CircularFastMedicalModel(3, 1);
        assertEquals(0, m.getSweepIndex());
        m.add(0.0, new double[]{1.0});
        assertEquals(1, m.getSweepIndex());
        m.add(1.0, new double[]{2.0});
        assertEquals(2, m.getSweepIndex());
        m.add(2.0, new double[]{3.0});
        assertEquals(0, m.getSweepIndex());
        m.clear();
        assertEquals(0, m.getRawSize());
    }

    @Test
    public void clearResetsAnIncompleteSweep() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(4, 1);
        model.add(1, new double[]{10});
        model.add(2, new double[]{20});
        assertEquals(2, model.getSweepIndex());
        long stamp = model.getUpdateStamp();

        model.clear();

        assertEquals(0, model.getRawSize());
        assertEquals(0, model.getHeadIndex());
        assertEquals(0, model.getSweepIndex());
        assertEquals(stamp + 1, model.getUpdateStamp());

        model.add(3, new double[]{30});
        assertEquals(1, model.getSweepIndex());
        assertEquals(30.0, model.getY(0));
    }
}
