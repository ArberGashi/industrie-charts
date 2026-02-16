# ArberCharts Quick Start Guide

**Get started in under 5 minutes!**

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```kotlin
implementation("com.arbergashi:arbercharts-swing-bridge:2.0.0")
```

---

## Your First Chart (2 Lines)

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

**That's it!** Run it and you get a beautiful, interactive chart.

---

## 5-Minute Tutorial

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

### Step 3: Customize

```java
chart.title("Sales 2026")
     .subtitle("Q1 Performance")
     .xLabel("Month")
     .yLabel("Revenue (CHF)")
     .theme("obsidian");
```

### Step 4: Show or Export

```java
// Display in window
chart.show();

// Or export to file
chart.exportToPNG("sales-2026.png");
```

---

## Common Use Cases

### Real-Time Monitoring

```java
Charts.streamingLineChart(1000)
    .title("CPU Usage")
    .updateInterval(100)  // 10 Hz
    .startStreaming(() -> getCpuUsage())
    .show();
```

### Financial Data

```java
Charts.candlestickChart()
    .title("BTC/USD")
    .loadFromCSV("bitcoin-prices.csv")
    .show();
```

### Medical ECG

```java
Charts.ecgChart()
    .sweepMode(true)
    .gridEnabled(true)
    .startStreaming(() -> getHeartRateSignal())
    .show();
```

---

## Spring Boot Integration

### Step 1: Add Starter

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Step 2: Configure (Optional)

```yaml
arbercharts:
  theme: dark
  export:
    enabled: true
    formats: [png, svg, pdf]
```

### Step 3: Use Anywhere

```java
@RestController
public class ChartController {
    
    @GetMapping("/chart.png")
    public byte[] chart() {
        return Charts.lineChart()
            .addData(1, 10, 2, 20, 3, 15)
            .exportToPNG();
    }
}
```

**Zero configuration needed!** Auto-configured on startup.

---

## Themes

ArberCharts comes with 3 beautiful themes:

```java
chart.theme("dark");      // Default - dark background
chart.theme("light");     // Light background
chart.theme("obsidian");  // Professional dark theme
```

---

## Performance

ArberCharts is **Zero-GC** - no garbage collection pauses:

```java
// Renders at 10,000+ FPS with zero GC pauses
Charts.streamingLineChart(10000)
    .updateInterval(1)  // 1000 Hz!
    .startStreaming(() -> Math.random())
    .show();
```

**Perfect for:**
- Medical monitors (ECG, vital signs)
- Trading systems (real-time prices)
- Industrial dashboards (sensor data)

---

## API Philosophy

**ArberCharts follows 3 principles:**

1. **Simple things are simple** - Common charts in 2-5 lines
2. **Complex things are possible** - Full API access when needed
3. **Zero-GC always** - <1ms latency guaranteed

---

## Examples

### Minimal (2 lines)

```java
Charts.lineChart()
    .show();
```

### Typical (5 lines)

```java
Charts.lineChart()
    .title("Temperature")
    .addData(1, 20, 2, 21, 3, 20.5)
    .theme("dark")
    .show();
```

### Advanced (10 lines)

```java
Charts.streamingLineChart(1000)
    .title("Real-Time Sensor")
    .subtitle("Temperature Monitor")
    .xLabel("Time (s)")
    .yLabel("°C")
    .theme("obsidian")
    .updateInterval(50)
    .startStreaming(() -> sensor.read())
    .exportToPNG("output.png")
    .show();
```

---

## What's Next?

- **[User Guide](USER_GUIDE.md)** - Complete documentation
- **[Renderer Catalog](RENDERER_CATALOG.md)** - 158 chart types
- **[Examples](../examples/)** - More code samples
- **[API Reference](../javadoc/)** - Full API docs

---

## Support

**Questions?** gashi@pro-business.ch

**Found a bug?** [GitHub Issues](https://github.com/ArberGashi/charts/issues)

---

**Welcome to ArberCharts!** 🎉

The **simplest** and **fastest** Java charting framework.

