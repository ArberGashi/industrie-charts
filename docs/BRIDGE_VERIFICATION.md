# Bridge Verification (v1.7.0-LTS)

This document captures minimal manual verification steps for bridge modules that are used by external developers who access the core via stable interfaces.

## Scope
- Compose Bridge (`arbercharts-compose-bridge`)
- Swing Bridge (`arbercharts-swing-bridge`)
- Server Bridge (`arbercharts-server-bridge`)
- Qt Bridge (`arbercharts-qt-bridge`)
- Swift Bridge (`arbercharts-swift-bridge`)

## Status Summary
- **Swing Bridge**: automated smoke test added (line drawing correctness). Unit tests pass.
- **Compose Bridge**: manual verification required (no CI setup available).
- **Server Bridge**: manual verification required (headless render pipeline).
- **Qt Bridge**: manual verification required (QML/Quick integration + native buffer decoding).
- **Swift Bridge**: manual verification required (CoreGraphics render path).

## Manual Verification Checklist

### 1) Compose Bridge (Compose Desktop)
- Create a simple Compose window with `ArberChart` composable.
- Render a LineRenderer and a CandlestickRenderer.
- Verify:
  - Lines are continuous (no point-only artifacts).
  - Clip region is respected (no drawing outside plot bounds).
  - Text rendering is ignored gracefully if unsupported.

### 2) Swing Bridge
- Run: `mvn -pl arbercharts-swing-bridge test`
- Verify:
  - `AwtCanvasAdapterTest` passes (line drawn from `moveTo`→`lineTo`).
  - Existing Swing UI tests remain green.

### 3) Server Bridge (Headless)
- Instantiate `ServerRenderService` and render a LineRenderer + CandlestickRenderer.
- Verify:
  - Output PNG is non-empty and has expected dimensions.
  - No exceptions in `renderToImage`/`renderToPng`.

### 4) Qt Bridge (QML/Quick)
- Load `ArberQuickItem` in a minimal QML scene.
- Call `smokeTest()`; expect `true`.
- Render with sample data via `setData(...)` and verify:
  - Frame updates without crash.
  - Clipping is respected (e.g., use a smaller viewport).

### 5) Swift Bridge (SwiftUI)
- Use `ArberChartView` in a basic SwiftUI scene.
- Verify:
  - The view renders without crashing.
  - Lines and rectangles appear as expected.
  - Text opcode does not crash (even if it is ignored).

## Known Constraints
- Compose/Qt/Swift verification is currently manual due to missing CI setup.
- Native buffer parsing in Qt/Swift assumes well‑formed buffers; malformed buffers are not sanitized.

## Recommendation
Until CI is available for Compose/Qt/Swift, include a manual verification step in release sign‑off when bridge code changes.

## CI Notes (GitHub Actions)
- Core/Swing/Server run on macOS/Windows/Linux via `.github/workflows/ci-core.yml`.
- Compose builds on macOS/Linux via `.github/workflows/ci-compose.yml`.
- Qt/Swift build on macOS via `.github/workflows/ci-qt-swift.yml` and require native artifacts.

### Qt/Swift Native Artifacts
Qt and Swift builds need the native headers + dylib:\n`ARBER_NATIVE_PATH` must point to a folder containing `arbercharts-core.h` and `libarbercharts-core.dylib`.\nTwo options:\n- Set `ARBER_NATIVE_PATH` as a GitHub Actions secret.\n- Or provide `dist/native` in the repository (CI will auto‑detect it).

## Minimal Samples (Quick Start)

### Compose Desktop (Kotlin)
```kotlin
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arbergashi.charts.bridge.compose.ArberChart
import com.arbergashi.charts.model.DefaultChartModel
import com.arbergashi.charts.render.standard.LineRenderer

fun main() = application {
    val model = DefaultChartModel()
    for (i in 0 until 100) model.addPoint(i.toDouble(), kotlin.math.sin(i / 10.0))
    Window(onCloseRequest = ::exitApplication, title = "ArberCharts Compose") {
        ArberChart(model = model, renderer = LineRenderer())
    }
}
```

### Swing (Java)
```java
import com.arbergashi.charts.platform.ArberChartPanel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;

import javax.swing.*;

public class SwingSample {
    public static void main(String[] args) {
        DefaultChartModel model = new DefaultChartModel();
        for (int i = 0; i < 100; i++) {
            model.addPoint(i, Math.sin(i / 10.0));
        }
        ArberChartPanel panel = new ArberChartPanel(model, new LineRenderer());
        JFrame frame = new JFrame("ArberCharts Swing");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setSize(800, 400);
        frame.setVisible(true);
    }
}
```

### Server (Java)
```java
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.DefaultChartModel;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerSample {
    public static void main(String[] args) throws Exception {
        DefaultChartModel model = new DefaultChartModel();
        for (int i = 0; i < 200; i++) {
            model.addPoint(i, Math.cos(i / 15.0));
        }
        ServerRenderService service = new ServerRenderService();
        byte[] png = service.renderToPng(model, new Dimension(800, 400));
        Files.write(Path.of("chart.png"), png);
    }
}
```

### Qt (QML + C++)
QML:
```qml
import QtQuick 2.15
import QtQuick.Window 2.15

Window {
    width: 800; height: 400; visible: true
    ArberQuickItem { anchors.fill: parent }
}
```
C++ registration:
```cpp
#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include "ArberQuickItem.h"

int main(int argc, char* argv[]) {
    QGuiApplication app(argc, argv);
    qmlRegisterType<ArberQuickItem>("ArberCharts", 1, 0, "ArberQuickItem");
    QQmlApplicationEngine engine;
    engine.load(QUrl(QStringLiteral("qrc:/main.qml")));
    return app.exec();
}
```

### Swift (SwiftUI)
```swift
import SwiftUI

@main
struct ArberChartsDemoApp: App {
    var body: some Scene {
        WindowGroup {
            ArberChartView()
        }
    }
}
```
