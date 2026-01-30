package com.arbergashi.charts.bridge.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.arbergashi.charts.api.ChartRenderHints
import com.arbergashi.charts.api.ChartTheme
import com.arbergashi.charts.api.DefaultPlotContext
import com.arbergashi.charts.core.geometry.ArberRect
import com.arbergashi.charts.model.ChartModel
import com.arbergashi.charts.render.ChartRenderer

@Composable
fun ArberChart(
    model: ChartModel,
    renderer: ChartRenderer,
    modifier: Modifier = Modifier,
    theme: ChartTheme? = null,
    renderHints: ChartRenderHints? = null,
    viewMinX: Double = Double.NaN,
    viewMaxX: Double = Double.NaN,
    viewMinY: Double = Double.NaN,
    viewMaxY: Double = Double.NaN
) {
    Canvas(modifier = modifier) {
        val bounds = ArberRect(0.0, 0.0, size.width.toDouble(), size.height.toDouble())
        val context = DefaultPlotContext(
            bounds,
            model,
            viewMinX,
            viewMaxX,
            viewMinY,
            viewMaxY,
            theme,
            renderHints
        )
        drawIntoCanvas { canvas ->
            val arberCanvas = ComposeArberCanvas(canvas)
            renderer.render(arberCanvas, model, context)
        }
    }
}
