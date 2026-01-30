package com.arbergashi.charts.bridge.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PaintingStyle
import com.arbergashi.charts.api.types.ArberColor
import com.arbergashi.charts.core.geometry.ArberRect
import com.arbergashi.charts.core.rendering.ArberCanvas
import com.arbergashi.charts.core.rendering.ArberMatrices
import com.arbergashi.charts.core.rendering.ArberMatrix
import com.arbergashi.charts.core.rendering.VoxelBuffer
import kotlin.math.max

class ComposeArberCanvas(
    private val canvas: Canvas
) : ArberCanvas {
    private var strokeWidth = 1f
    private var clip: ArberRect? = null
    private var color = Color.Black
    private var lastX = 0f
    private var lastY = 0f
    private val strokePaint = Paint().apply { style = PaintingStyle.Stroke }
    private val fillPaint = Paint().apply { style = PaintingStyle.Fill }

    override fun setColor(color: ArberColor) {
        this.color = Color(color.argb())
        strokePaint.color = this.color
        fillPaint.color = this.color
    }

    override fun setStroke(width: Float) {
        strokeWidth = width
        strokePaint.strokeWidth = width
    }

    override fun moveTo(x: Float, y: Float) {
        lastX = x
        lastY = y
    }

    override fun lineTo(x: Float, y: Float) {
        withClip {
            canvas.drawLine(Offset(lastX, lastY), Offset(x, y), strokePaint)
        }
        lastX = x
        lastY = y
    }

    override fun drawPolyline(xs: FloatArray?, ys: FloatArray?, count: Int) {
        if (xs == null || ys == null || count <= 1) return
        val n = minOf(count, xs.size, ys.size)
        if (n <= 1) return
        val path = Path()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until n) {
            path.lineTo(xs[i], ys[i])
        }
        withClip {
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun drawRect(x: Float, y: Float, w: Float, h: Float) {
        withClip {
            canvas.drawRect(Rect(x, y, x + w, y + h), strokePaint)
        }
    }

    override fun fillRect(x: Float, y: Float, w: Float, h: Float) {
        withClip {
            canvas.drawRect(Rect(x, y, x + w, y + h), fillPaint)
        }
    }

    override fun fillPolygon(xs: FloatArray?, ys: FloatArray?, count: Int) {
        if (xs == null || ys == null || count <= 2) return
        val n = minOf(count, xs.size, ys.size)
        if (n <= 2) return
        val path = Path()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until n) {
            path.lineTo(xs[i], ys[i])
        }
        path.close()
        withClip {
            canvas.drawPath(path, fillPaint)
        }
    }

    override fun drawVoxelField(buffer: VoxelBuffer?) {
        if (buffer == null) return
        val xs = buffer.x()
        val ys = buffer.y()
        val colors = buffer.argb()
        val n = buffer.count()
        val size = max(1f, strokeWidth)
        for (i in 0 until n) {
            if (colors != null && i < colors.size) {
                val c = Color(colors[i])
                strokePaint.color = c
                fillPaint.color = c
            }
            withClip {
                val x = xs[i]
                val y = ys[i]
                canvas.drawRect(Rect(x, y, x + size, y + size), fillPaint)
            }
        }
        strokePaint.color = color
        fillPaint.color = color
    }

    override fun getTransform(): ArberMatrix {
        return ArberMatrices.identity()
    }

    override fun setClip(clip: ArberRect?) {
        this.clip = clip
    }

    override fun getClip(): ArberRect? {
        return clip
    }

    private inline fun withClip(block: () -> Unit) {
        val localClip = clip
        if (localClip == null) {
            block()
            return
        }
        canvas.save()
        canvas.clipRect(
            Rect(
                localClip.x().toFloat(),
                localClip.y().toFloat(),
                localClip.maxX().toFloat(),
                localClip.maxY().toFloat()
            )
        )
        block()
        canvas.restore()
    }
}
