package com.arbergashi.charts.bridge.compose

import com.arbergashi.charts.core.rendering.ArberCanvasProvider

class ComposeCanvasProvider : ArberCanvasProvider {
    override fun getId(): String {
        return "compose"
    }

    override fun getPriority(): Int {
        return 100
    }

    override fun isSupported(): Boolean {
        return true
    }
}
