package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.common.PerformanceAuditRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.util.LatencyTracker;

final class PerformanceAuditRendererAdapter extends BaseRenderer {
    private final LineRenderer base = new LineRenderer();
    private final PerformanceAuditRenderer audit;

    PerformanceAuditRendererAdapter(LatencyTracker tracker) {
        super("performance_audit_adapter");
        this.audit = new PerformanceAuditRenderer(tracker);
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        base.render(canvas, model, context);
        audit.render(canvas, context, getResolvedTheme(context));
    }
}
