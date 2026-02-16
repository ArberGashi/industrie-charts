# ArberCharts v2.0 Migration Guide

**Document Version:** 1.0  
**Last Updated:** 14. Februar 2026  
**Target Audience:** Customers migrating from native bridges to JVM-only

---

## Executive Summary

ArberCharts 2.0 removes Swift, Qt, and Compose bridges to focus entirely on Java 25, Enterprise-Swing, and Spring Boot. This guide helps you migrate smoothly.

---

## Who Needs to Migrate?

### ✅ You Need to Migrate If:
- You use `arbercharts-swift-bridge`
- You use `arbercharts-qt-bridge`
- You use `arbercharts-compose-bridge`

### ✅ You're Unaffected If:
- You use `arbercharts-swing-bridge` (Enhanced in v2.0!)
- You use `arbercharts-spring-boot-starter` (Enhanced in v2.0!)
- You use `arbercharts-server-bridge` (Enhanced in v2.0!)

---

## Timeline & Support

| Version | Status | Support End Date | Notes |
|---------|--------|------------------|-------|
| v1.7.0-LTS | Current | December 31, 2027 | All bridges supported |
| v1.8.0 | Deprecation | Q2 2026 | Warnings added |
| v2.0.0 | Breaking | Q1 2027 | Native bridges removed |

**Support Guarantee:** v1.7.0-LTS receives security patches and critical bug fixes until end of 2027.

---

## Migration Option 1: Stay on v1.7.0-LTS

### When to Choose This

- You have tight deadlines and can't migrate immediately
- Your application is stable and doesn't need new features
- You're willing to stay on an older version for 2+ years

### What You Get

✅ Fully functional native bridges  
✅ Security patches until 12/2027  
✅ Critical bug fixes  
❌ No new features  
❌ No v2.0 enhancements  

### How to Stay on v1.7.0-LTS

Lock your dependency version:

**Maven:**
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swift-bridge</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("com.arbergashi:arbercharts-swift-bridge:1.7.0-LTS")
```

### Support Contact

If you choose to stay on v1.7.0-LTS, please notify us:
📧 gashi@pro-business.ch

This helps us plan maintenance resources.

---

## Migration Option 2: Migrate to Swing Bridge (Recommended)

### When to Choose This

- You want access to v2.0 enhancements
- You're building desktop applications
- You value cross-platform compatibility
- You want the best possible support

### What You Get

✅ Enterprise-grade Swing components  
✅ FlatLaf 4.x modern look & feel  
✅ Accessibility (WCAG 2.1 AA)  
✅ High-DPI support (150%, 200%, 250%)  
✅ Full v2.0 feature set  
✅ Best-in-class performance  
✅ Cross-platform (Windows, macOS, Linux)  

### Migration Effort Estimate

| Application Size | Estimated Time | Complexity |
|------------------|----------------|------------|
| Small (1-5 charts) | 1-2 days | Low |
| Medium (5-20 charts) | 3-5 days | Medium |
| Large (20+ charts) | 1-2 weeks | Medium |
| Complex (custom renderers) | 2-4 weeks | High |

### Step-by-Step Migration

#### Step 1: Update Dependencies

**Before (Swift Bridge):**
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swift-bridge</artifactId>
    <version>1.7.0-LTS</version>
</dependency>
```

**After (Swing Bridge):**
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>2.0.0</version>
</dependency>
```

#### Step 2: Replace UI Components

**Before (Swift/iOS):**
```swift
import ArberCharts

let chartView = ChartView()
chartView.model = CircularChartModel(capacity: 10000)
chartView.renderer = LineRenderer()
```

**After (Swing/Java):**
```java
import com.arbergashi.charts.platform.swing.ArberChartPanel;
import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;

ArberChartPanel chartPanel = new ArberChartPanel();
chartPanel.setModel(new CircularChartModel(10000));
chartPanel.setRenderer(new LineRenderer());
```

**After (Swing/Kotlin):**
```kotlin
import com.arbergashi.charts.platform.swing.ArberChartPanel
import com.arbergashi.charts.model.CircularChartModel
import com.arbergashi.charts.render.standard.LineRenderer

val chartPanel = ArberChartPanel().apply {
    model = CircularChartModel(10000)
    renderer = LineRenderer()
}
```

#### Step 3: Update Event Handling

**Before (Swift Touch Events):**
```swift
chartView.onTap = { point in
    print("Tapped at: \(point)")
}
```

**After (Swing Mouse Events):**
```java
chartPanel.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("Clicked at: " + e.getPoint());
    }
});
```

#### Step 4: Integrate into Application

**Java Swing Application:**
```java
import javax.swing.*;

public class MainApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ArberCharts Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            ArberChartPanel chart = new ArberChartPanel();
            chart.setModel(new CircularChartModel(10000));
            chart.setRenderer(new LineRenderer());
            
            frame.add(chart);
            frame.setSize(800, 600);
            frame.setVisible(true);
        });
    }
}
```

#### Step 5: Apply Modern Look & Feel

Make your Swing app look modern with FlatLaf:

```java
import com.formdev.flatlaf.FlatDarkLaf;

public class MainApp {
    public static void main(String[] args) {
        // Apply FlatLaf Dark theme
        FlatDarkLaf.setup();
        
        SwingUtilities.invokeLater(() -> {
            // Your application code
        });
    }
}
```

**Available Themes:**
- `FlatLightLaf` - Clean light theme
- `FlatDarkLaf` - Modern dark theme
- `FlatIntelliJLaf` - IntelliJ IDEA look
- `FlatDarculaLaf` - Darcula look

### Qt Bridge Migration

**Before (Qt/C++):**
```cpp
#include <ArberChartWidget.h>

ArberChartWidget* chart = new ArberChartWidget();
chart->setModel(new CircularChartModel(10000));
chart->setRenderer(new LineRenderer());
```

**After (Swing/Java):**
Same as Swift migration above. Use Java/Swing instead of C++/Qt.

**JNI Integration (if needed):**
If you need to call Java from C++, use JNI:
```cpp
// Call Java Swing chart from C++ application
JNIEnv* env = getJNIEnv();
jobject chart = env->NewObject(chartClass, constructor);
```

### Compose Bridge Migration

**Before (Compose Desktop):**
```kotlin
@Composable
fun ChartScreen() {
    ArberChart(
        model = remember { CircularChartModel(10000) },
        renderer = LineRenderer()
    )
}
```

**After (Swing in Compose):**
```kotlin
@Composable
fun ChartScreen() {
    SwingPanel(
        factory = {
            ArberChartPanel().apply {
                model = CircularChartModel(10000)
                renderer = LineRenderer()
            }
        }
    )
}
```

**Or Pure Swing:**
```kotlin
fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val chart = remember {
            ArberChartPanel().apply {
                model = CircularChartModel(10000)
                renderer = LineRenderer()
            }
        }
        
        SwingPanel(factory = { chart })
    }
}
```

---

## Migration Option 3: Direct Core Access (Advanced)

### When to Choose This

- You have a custom UI framework
- You need full control over rendering
- You're building a non-standard platform

### What You Get

✅ Direct access to core rendering  
✅ Platform-agnostic  
✅ Maximum flexibility  
❌ No pre-built UI components  
❌ You handle all platform integration  

### Implementation

```java
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.api.PlotContext;

// Implement your own canvas
class MyCustomCanvas implements ArberCanvas {
    @Override
    public void drawLine(double x1, double y1, double x2, double y2) {
        // Your rendering code
    }
    
    @Override
    public void drawRect(double x, double y, double w, double h) {
        // Your rendering code
    }
    
    // ... implement other methods
}

// Use it
ArberCanvas canvas = new MyCustomCanvas();
LineRenderer renderer = new LineRenderer();
renderer.render(canvas, model, plotContext);
```

---

## Migration Option 4: Custom Bridge Development

### When to Choose This

- You have unique platform requirements
- Your business depends on native bridges
- Budget allows custom development

### What We Offer

**Services:**
- Custom Swift bridge maintained separately
- Custom Qt bridge maintained separately
- Custom Compose bridge maintained separately
- Any other platform (Flutter, Electron, etc.)

**Pricing:**
- Starting at CHF 50,000
- Depends on platform complexity
- Includes source code and documentation

**Timeline:**
- 2-3 months development
- Ongoing maintenance available

**Contact:**
📧 gashi@pro-business.ch

---

## Migration Support

### Free Migration Support

For existing v1.7.0-LTS customers, we offer:

✅ 1-hour migration consultation call  
✅ Code review of your migration  
✅ Priority email support during migration  
✅ Access to migration examples  

**How to Get Support:**
1. Email gashi@pro-business.ch
2. Subject: "v2.0 Migration Support - [Your Company]"
3. Include: Current usage, target timeline

### Paid Migration Services

For complex migrations, we offer:

**Migration Package:**
- Full code migration by ArberCharts team
- Testing and validation
- Training for your team
- 3 months post-migration support

**Pricing:** Starting at CHF 15,000

**Contact:** gashi@pro-business.ch

---

## Testing Your Migration

### Test Checklist

Before deploying your migrated application:

- [ ] All charts render correctly
- [ ] Interactive features work (zoom, pan, crosshair)
- [ ] Export functionality works (PNG/SVG/PDF)
- [ ] Performance is acceptable
- [ ] Theme switching works
- [ ] High-DPI displays render correctly
- [ ] Memory usage is stable
- [ ] No exceptions in logs

### Compatibility Mode

If you need gradual migration:

```java
// Keep using v1.7.0 for some charts
@Deprecated
import com.arbergashi.charts.ui.ChartPanel; // Old API

// Use v2.0 for new charts
import com.arbergashi.charts.platform.swing.ArberChartPanel; // New API
```

Both can coexist during migration.

---

## FAQ

### Q: Can I use both v1.7.0 and v2.0 in the same application?

**A:** Yes, but not recommended. Use separate class loaders or modules if needed.

### Q: Will my custom renderers work in v2.0?

**A:** Yes, if they use the core API. Only bridge-specific code needs updates.

### Q: What if I find a bug in v1.7.0-LTS after v2.0 releases?

**A:** We'll fix critical bugs and security issues until end of 2027.

### Q: Can I get a trial of v2.0 before deciding?

**A:** Yes! v2.0-beta will be available in Q4 2026.

### Q: What happens if I don't migrate?

**A:** You can stay on v1.7.0-LTS until end of 2027. After that, no more updates.

### Q: Is there a discount for early migration?

**A:** Yes! Migrate before Q3 2026 and get 20% off your next license renewal.

---

## Success Stories

### Case Study: Financial Trading Platform

**Customer:** Large Swiss Bank  
**Before:** Qt Bridge for native Linux terminals  
**After:** Swing Bridge with FlatLaf  
**Migration Time:** 2 weeks  
**Result:** Better performance, easier deployment, happier developers

**Quote:** "The Swing migration was easier than expected. FlatLaf looks great, and we can now deploy updates without recompiling native code." - Lead Developer

---

## Additional Resources

- [V2_STRATEGY.md](V2_STRATEGY.md) - Full strategic rationale
- [AUDIT.md](AUDIT.md) - Technical audit and v2.0 analysis
- [USER_GUIDE.md](USER_GUIDE.md) - v2.0 user guide (coming Q4 2026)

---

## Contact

**Questions?** gashi@pro-business.ch  
**Migration Support:** gashi@pro-business.ch  
**Custom Development:** gashi@pro-business.ch

We're here to help make your migration smooth and successful!

---

**Last Updated:** 14. Februar 2026  
**Document Version:** 1.0

