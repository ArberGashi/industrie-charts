# ArberCharts Quick Start Guide

Create a Swing chart or render a PNG with the APIs available in ArberCharts 2.5.0.

---

## Installation

Use Eclipse Temurin JDK 25 from Adoptium. An existing Temurin 25 installation is sufficient.
Keep all ArberCharts dependencies at version **2.5.0**.

### Maven

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>2.5.0</version>
</dependency>
```

### Gradle

```kotlin
implementation("com.arbergashi:arbercharts-swing-bridge:2.5.0")
```

---

## Your First Chart

```java
import com.arbergashi.charts.Charts;

public class HelloChart {
    public static void main(String[] args) {
        Charts.lineChart()
            .addData(1, 10, 2, 20, 3, 15, 4, 30)
            .show();
    }
}
```

Run this class on a graphical desktop. The Swing bridge creates the chart window.

---

## Adding Data

### Step 1: Create a Chart

```java
var chart = Charts.lineChart();
```

### Step 2: Add Data

```java
// Easy way: alternating x,y values
chart.addData(1, 100, 2, 150, 3, 200, 4, 180);

// Or individually
chart.addPoint(5, 220);
chart.addPoint(6, 250);
```

### Step 3: Set the Window Title

```java
chart.title("Sales 2026");
```

### Step 4: Show the Chart

```java
chart.show();
```

For PNG output, use `ServerRenderService` as shown in the Spring Boot example below.

---

## Common Use Cases

- **Animated data and medical renderers:** the [Swing workbench](../arbercharts-demo/docs/WORKBENCH.md)
  uses actual chart models, renderers and panel lifecycle handling.
- **Financial data:** the [Spring Boot workbench](../arbercharts-spring-boot-demo/README.md)
  includes a runnable candlestick example with synthetic OHLC data.
- **Other chart families:** use the [renderer catalog](RENDERER_CATALOG.md)
  and the corresponding model types directly.

The `Charts` convenience API supports line-chart creation, data insertion
and display. Its streaming and export helpers, and the other convenience factories,
are not implemented. Use the model, renderer and host APIs demonstrated by the workbenches
for those features. Its stored subtitle, axis-label and theme values are not applied
by the bundled display provider; use the panel API below to configure presentation.

---

## Spring Boot Integration

### Step 1: Add Starter

In an existing Spring Boot 4.1.1 web application, add:

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>2.5.0</version>
</dependency>
```

### Step 2: Configure (Optional)

```yaml
arbercharts:
  server:
    pool-size: 4
    metrics-enabled: true
```

### Step 3: Render a PNG

```java
import com.arbergashi.charts.bridge.server.ServerRenderService;
import com.arbergashi.charts.model.CircularChartModel;
import java.awt.Dimension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChartController {
    private final ServerRenderService renderService;

    public ChartController(ServerRenderService renderService) {
        this.renderService = renderService;
    }

    @GetMapping(value = "/chart.png", produces = "image/png")
    public byte[] chart() {
        var model = new CircularChartModel(3);
        model.setPoint(1, 10, 1.0, null);
        model.setPoint(2, 20, 1.0, null);
        model.setPoint(3, 15, 1.0, null);
        return renderService.renderToPng(model, new Dimension(800, 450));
    }
}
```

The starter configures `ServerRenderService`, and Spring closes the service on
shutdown. Each request above owns its model. Headless rendering needs a Java runtime
containing `java.desktop`, but does not require a display server.

---

## Configure a Swing Panel

Use `ArberChartPanel` directly for panel themes and renderer selection. Create and
configure Swing components on the event dispatch thread:

```java
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.render.standard.LineRenderer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ConfiguredChart {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var model = new CircularChartModel(3);
            model.setPoint(1, 20, 1.0, null);
            model.setPoint(2, 21, 1.0, null);
            model.setPoint(3, 20.5, 1.0, null);

            var panel = new ArberChartPanel(model, new LineRenderer());
            panel.setTheme(ChartThemes.getLightTheme());

            var frame = new JFrame("Temperature");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(panel);
            frame.setSize(800, 450);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

For application-specific axes, grids and layers, see the
[rendering architecture](RENDERING_ARCHITECTURE.md) and the runnable workbenches.

## Performance

Renderers reuse buffers and support optional Vector API acceleration. Throughput
and allocation rates depend on the renderer, backend, data and runtime. See the
[verification strategy](VERIFICATION_STRATEGY.md) for the functional, allocation
and performance checks and their coverage limits.

---

## Further Reading

- **[User Guide](USER_GUIDE.md)** - Modules, build commands and integration references
- **[Renderer Catalog](RENDERER_CATALOG.md)** - 156 catalog entries
- **[Swing Workbench](../arbercharts-demo/docs/WORKBENCH.md)** - Desktop demo
- **[Spring Boot Workbench](../arbercharts-spring-boot-demo/README.md)** - Browser demo and server examples
- **[API Sources](../arbercharts-core/src/main/java/com/arbergashi/charts/)** - Core API and Javadoc comments

---

Report reproducible defects through [GitHub Issues](https://github.com/ArberGashi/industrie-charts/issues).

