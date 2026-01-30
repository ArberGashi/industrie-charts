package com.arbergashi.charts.bridge.compose

import com.arbergashi.charts.api.types.ArberColor
import com.arbergashi.charts.core.geometry.ArberRect
import com.arbergashi.charts.core.rendering.ArberCanvas
import com.arbergashi.charts.core.rendering.ArberMatrix
import com.arbergashi.charts.core.rendering.ArberMatrices
import com.arbergashi.charts.core.rendering.VoxelBuffer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect

class SkiaArberCanvas(private val canvas: Canvas) : ArberCanvas {
    private val strokePaint = Paint().apply {
        mode = PaintMode.STROKE
        isAntiAlias = true
        strokeWidth = 1f
    }
    private val fillPaint = Paint().apply {
        mode = PaintMode.FILL
        isAntiAlias = true
    }
    private var lastX = 0f
    private var lastY = 0f
    private var clip: ArberRect? = null
    private var clipSaveCount = 0

    override fun setColor(color: ArberColor) {
        strokePaint.color = color.argb()
        fillPaint.color = color.argb()
    }

    override fun setStroke(width: Float) {
        strokePaint.strokeWidth = width
    }

    override fun moveTo(x: Float, y: Float) {
        lastX = x
        lastY = y
    }

    override fun lineTo(x: Float, y: Float) {
        canvas.drawLine(lastX, lastY, x, y, strokePaint)
        lastX = x
        lastY = y
    }

    override fun drawPolyline(xs: FloatArray?, ys: FloatArray?, count: Int) {
        if (xs == null || ys == null || count <= 1) return
        val n = minOf(count, xs.size, ys.size)
        var x0 = xs[0]
        var y0 = ys[0]
        for (i in 1 until n) {
            val x1 = xs[i]
            val y1 = ys[i]
            canvas.drawLine(x0, y0, x1, y1, strokePaint)
            x0 = x1
            y0 = y1
        }
    }

    override fun drawRect(x: Float, y: Float, w: Float, h: Float) {
        canvas.drawRect(Rect.makeXYWH(x, y, w, h), strokePaint)
    }

    override fun fillRect(x: Float, y: Float, w: Float, h: Float) {
        canvas.drawRect(Rect.makeXYWH(x, y, w, h), fillPaint)
    }

    override fun fillPolygon(xs: FloatArray?, ys: FloatArray?, count: Int) {
        if (xs == null || ys == null || count <= 2) return
        val n = minOf(count, xs.size, ys.size)
        val path = Path()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until n) {
            path.lineTo(xs[i], ys[i])
        }
        path.closePath()
        canvas.drawPath(path, fillPaint)
    }

    override fun drawVoxelField(buffer: VoxelBuffer?) {
        if (buffer == null) return
        val xs = buffer.x()
        val ys = buffer.y()
        val colors = buffer.argb()
        val n = buffer.count()
        val size = strokePaint.strokeWidth.coerceAtLeast(1f)
        for (i in 0 until n) {
            if (colors != null && i < colors.size) {
                fillPaint.color = colors[i]
            }
            canvas.drawRect(Rect.makeXYWH(xs[i], ys[i], size, size), fillPaint)
        }
    }

    override fun getTransform(): ArberMatrix {
        return ArberMatrices.identity()
    }

    override fun setClip(clip: ArberRect?) {
        this.clip = clip
        if (clipSaveCount != 0) {
            canvas.restoreToCount(clipSaveCount)
            clipSaveCount = 0
        }
        if (clip != null) {
            clipSaveCount = canvas.save()
            val rect = Rect.makeXYWH(clip.x.toFloat(), clip.y.toFloat(), clip.width.toFloat(), clip.height.toFloat())
            canvas.clipRect(rect)
        }
    }

    override fun getClip(): ArberRect? {
        return clip
    }
}
