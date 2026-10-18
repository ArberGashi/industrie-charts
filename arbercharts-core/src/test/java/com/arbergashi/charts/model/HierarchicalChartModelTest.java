package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalChartModelTest {
    @Test
    void aRootWithChildrenIsNotEmptyEvenWithoutFlatPoints() {
        var root = new DefaultHierarchicalChartModel.DefaultNode("Root", 0);
        root.setChild(new DefaultHierarchicalChartModel.DefaultNode("Child", 8));
        ChartModel model = new DefaultHierarchicalChartModel(root);

        assertEquals(0, model.getPointCount());
        assertFalse(model.isEmpty(), "Render hosts must not discard a hierarchy without flat points");
    }

    @Test
    void aSingleRootIsDataButAnAbsentRootIsEmpty() {
        ChartModel singleRoot = new DefaultHierarchicalChartModel(
                new DefaultHierarchicalChartModel.DefaultNode("Root", 0));
        ChartModel absentRoot = new DefaultHierarchicalChartModel(null);

        assertFalse(singleRoot.isEmpty());
        assertTrue(absentRoot.isEmpty());
    }
}
