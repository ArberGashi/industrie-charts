package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultMatrixChartModel;
import com.arbergashi.charts.render.circular.ChordDiagramRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import java.util.List;

public class ChordChartPanelProvider {
    public static ArberChartPanel create() {
        // Inter-departmental Collaboration – Project hours shared between teams
        double[][] matrix = {
            {0, 88, 52, 110, 34, 26},   // Engineering
            {88, 0, 68, 30, 50, 22},    // Product
            {52, 68, 0, 84, 42, 18},    // Design
            {110, 30, 84, 0, 62, 20},   // QA
            {34, 50, 42, 62, 0, 28},    // DevOps
            {26, 22, 18, 20, 28, 0}     // Security
        };
        List<String> labels = List.of("Engineering", "Product", "Design", "QA", "DevOps", "Security");
        DefaultMatrixChartModel model = new DefaultMatrixChartModel(matrix, labels);

        return ArberChartBuilder.create()
                .withTitle("Inter-departmental Collaboration Hours")
                .addLayer(model, new ChordDiagramRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
