package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.financial.AuditTrailRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;

final class AuditTrailRendererAdapter extends BaseRenderer {
    private final LineRenderer base = new LineRenderer();
    private final AuditTrailRenderer audit = new AuditTrailRenderer();

    AuditTrailRendererAdapter() {
        super("audit_trail_adapter");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        base.render(canvas, model, context);
        audit.render(canvas, model, context);
    }
}
