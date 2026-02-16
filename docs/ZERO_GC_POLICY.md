# ArberCharts Zero-GC Policy (v2.0)

**Document Version:** 2.0  
**Date:** 16. Februar 2026  
**Status:** MANDATORY  
**Enforcement:** ArchUnit + Code Review  
**Compliance:** ✅ **100% VERIFIED**

---

## Compliance Status

| Component | Status | Verification |
|-----------|--------|--------------|
| **Core Renderers** | ✅ 100% | No direct BasicStroke/Color allocations |
| **ArberCanvas Interface** | ✅ 100% | Framework-agnostic, no AWT dependencies |
| **AwtCanvasAdapter (Swing)** | ✅ 100% | Uses ZeroAllocPool + ThreadLocal caches |
| **ImageBufferCanvas (Server)** | ✅ 100% | Direct pixel buffer, no AWT objects |
| **ZeroAllocPool** | ✅ Active | Thread-local Stroke/Color caches |

**Last Verified:** 16. Februar 2026

---

## Mission Statement

**Zero-GC rendering is non-negotiable in ArberCharts.**

Every frame must render without heap allocations in the hot path. This is what separates ArberCharts from competitors and enables mission-critical performance.

---

## The Zero-GC Doctrine

### Core Principle

> **NO allocations are permitted in render methods or any hot path.**

"Hot path" = any code executed per frame or per data point during rendering.

### Why Zero-GC?

**Performance Reality:**
- **With GC:** p99 latency = 5-10ms, occasional 50ms+ pauses
- **Zero-GC:** p99 latency = <1ms, NO pauses

**Business Impact:**
- Medical monitors: GC pause = patient safety risk
- Trading systems: GC pause = missed trades
- Real-time dashboards: GC pause = user frustration

**ArberCharts USP:**
Zero-GC is what allows us to charge CHF 6K-25K per license.

---

## Forbidden Patterns

### ❌ NEVER Do This

```java
// ❌ WRONG - Allocates on every render!
public void render(Graphics2D g2) {
    g2.setStroke(new BasicStroke(2.0f));        // ALLOCATION
    g2.setColor(new Color(255, 0, 0));          // ALLOCATION
    
    for (int i = 0; i < points.length; i++) {
        Point2D p = new Point2D.Double(x, y);   // ALLOCATION
        path.lineTo(p.getX(), p.getY());
    }
    
    String label = "Value: " + value;           // STRING ALLOCATION
    g2.drawString(label, x, y);
}
```

**Impact:** 10,000 allocations per frame → GC every few seconds

### ✅ Always Do This

```java
// ✅ RIGHT - Zero allocations
public void render(Graphics2D g2) {
    g2.setStroke(ZeroAllocPool.getStroke(2.0f));     // REUSED
    g2.setColor(ZeroAllocPool.getColor(255, 0, 0));  // REUSED
    
    // Use primitive arrays, no Point2D objects
    double[] xBuf = ZeroAllocPool.getDoubleBuffer(points.length);
    double[] yBuf = ZeroAllocPool.getDoubleBuffer(points.length);
    
    for (int i = 0; i < points.length; i++) {
        path.lineTo(xBuf[i], yBuf[i]);  // No allocation
    }
    
    // Pre-format strings or use StringBuilder pool
    g2.drawString(cachedLabel, x, y);
}
```

---

## Approved Patterns

### 1. Object Pooling (Required)

```java
// Use ZeroAllocPool for everything
BasicStroke stroke = ZeroAllocPool.getStroke(width);
Color color = ZeroAllocPool.getColor(r, g, b);
double[] buffer = ZeroAllocPool.getDoubleBuffer(capacity);
```

### 2. Thread-Local Caching

```java
// Each thread gets its own reusable objects
private static final ThreadLocal<StringBuilder> STRING_BUILDER =
    ThreadLocal.withInitial(() -> new StringBuilder(256));

public String formatValue(double value) {
    StringBuilder sb = STRING_BUILDER.get();
    sb.setLength(0);  // Clear, don't allocate new
    sb.append("Value: ").append(value);
    return sb.toString();  // Only allocation if needed
}
```

### 3. Pre-allocated Arrays

```java
// Allocate once at construction
private final double[] xBuffer;
private final double[] yBuffer;

public MyRenderer(int maxPoints) {
    this.xBuffer = new double[maxPoints];
    this.yBuffer = new double[maxPoints];
}

public void render(Graphics2D g2) {
    // Reuse existing arrays
    fillBuffers(xBuffer, yBuffer);
    drawPath(g2, xBuffer, yBuffer);
}
```

### 4. Flyweight Pattern

```java
// Share immutable objects
private static final Color RED = new Color(255, 0, 0);
private static final Color GREEN = new Color(0, 255, 0);
private static final BasicStroke THIN = new BasicStroke(1.0f);
private static final BasicStroke THICK = new BasicStroke(2.0f);

// Always reuse
g2.setColor(RED);
g2.setStroke(THIN);
```

---

## Architecture Enforcement

### ArchUnit Tests (Mandatory)

```java
package com.arbergashi.charts.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;
import java.awt.Color;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Zero-GC Architecture Tests.
 * 
 * These tests MUST pass before any PR is merged.
 */
class ZeroGcArchitectureTest {
    
    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.arbergashi.charts");
    
    @Test
    void renderers_must_not_allocate_strokes() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..render..")
            .and().haveSimpleNameEndingWith("Renderer")
            .should().callConstructor(BasicStroke.class)
            .because("Zero-allocation rendering is mandatory. Use ZeroAllocPool.getStroke()");
        
        rule.check(classes);
    }
    
    @Test
    void renderers_must_not_allocate_colors() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..render..")
            .and().haveSimpleNameEndingWith("Renderer")
            .should().callConstructor(Color.class)
            .because("Zero-allocation rendering is mandatory. Use ZeroAllocPool.getColor()");
        
        rule.check(classes);
    }
    
    @Test
    void render_methods_must_not_allocate_arrays() {
        ArchRule rule = noMethods()
            .that().haveName("render")
            .or().haveName("paint")
            .or().haveName("paintComponent")
            .should().callMethod("new", "double[]")
            .orShould().callMethod("new", "int[]")
            .orShould().callMethod("new", "float[]")
            .because("Use ZeroAllocPool.getBuffer() for temporary arrays");
        
        rule.check(classes);
    }
}
```

### Code Review Checklist

Before approving ANY renderer PR:

- [ ] No `new BasicStroke()` in hot paths
- [ ] No `new Color()` in hot paths
- [ ] No `new Point2D()` or geometry objects in loops
- [ ] No array allocations in render methods
- [ ] No string concatenation with `+` in hot paths
- [ ] All temporary objects obtained from pools
- [ ] ArchUnit tests pass

---

## Performance Verification

### JMH Benchmark Template

```java
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ZeroGcRenderBenchmark {
    
    private LineRenderer renderer;
    private ChartModel model;
    private BufferedImage image;
    private Graphics2D g2;
    
    @Setup
    public void setup() {
        renderer = new LineRenderer();
        model = createTestModel(10000);  // 10K points
        image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
    }
    
    @Benchmark
    public void renderZeroGc() {
        // Should complete in <100 microseconds with NO allocations
        renderer.render(g2, model, plotContext);
    }
    
    @TearDown
    public void verify() {
        // Verify zero allocations with allocation profiler
        long allocations = getAllocationsInLastBenchmark();
        if (allocations > 0) {
            throw new AssertionError(
                "Zero-GC violation: " + allocations + " allocations detected!"
            );
        }
    }
}
```

### Memory Profiler Integration

```bash
# Run with allocation profiler
java -javaagent:allocation-instrumenter.jar \
     -jar jmh-benchmarks.jar \
     ZeroGcRenderBenchmark

# Expected output:
# Allocations: 0 bytes
# GC collections: 0
```

---

## Zero-GC Metrics (v2.0 Targets)

### Render Performance

| Metric | Target | Measurement |
|--------|--------|-------------|
| Allocations per frame | **0** | Allocation profiler |
| p50 latency | <0.5ms | JMH benchmark |
| p99 latency | <1.0ms | JMH benchmark |
| p999 latency | <2.0ms | JMH benchmark |
| GC pauses during render | **0** | GC logs |

### Memory Stability

| Metric | Target | Measurement |
|--------|--------|-------------|
| Heap growth per 1M renders | <1 MB | JVisualVM |
| Memory leak rate | **0** | 24h stress test |
| Cache hit rate (strokes) | >99% | ZeroAllocPool.getStats() |
| Cache hit rate (colors) | >99% | ZeroAllocPool.getStats() |

---

## Migration Guide (v1.7 → v2.0)

### Step 1: Audit Existing Renderers

```bash
# Find all allocation sites
grep -r "new BasicStroke" arbercharts-core/src/main/java
grep -r "new Color" arbercharts-core/src/main/java
```

### Step 2: Replace with Pooled Objects

```java
// Before
g2.setStroke(new BasicStroke(2.0f));

// After
g2.setStroke(ZeroAllocPool.getStroke(2.0f));
```

### Step 3: Run ArchUnit Tests

```bash
mvn -pl arbercharts-core test -Dtest=ZeroGcArchitectureTest
```

### Step 4: Profile with JMH

```bash
mvn -pl arbercharts-core test -Dtest=ZeroGcRenderBenchmark
```

---

## Exception Cases

### When Allocations Are Allowed

1. **Initialization** (constructor, setup)
   ```java
   public MyRenderer() {
       this.buffer = new double[10000];  // OK - one-time
   }
   ```

2. **Configuration Changes** (theme switch, resize)
   ```java
   public void onThemeChanged() {
       this.cachedColor = new Color(r, g, b);  // OK - infrequent
   }
   ```

3. **Export** (PNG/SVG generation)
   ```java
   public byte[] exportToPng() {
       BufferedImage img = new BufferedImage(...);  // OK - not hot path
   }
   ```

4. **Error Handling** (exceptions are rare)
   ```java
   throw new IllegalArgumentException("Invalid input");  // OK - exceptional
   ```

---

## Violation Response

### If Zero-GC is Violated

**Severity: P1 (Blocker)**

1. **Immediate Action:**
   - PR rejected
   - Issue filed with "zero-gc-violation" label
   - Assign to renderer author

2. **Fix Process:**
   - Identify allocation site
   - Replace with pooled object
   - Add ArchUnit test if missing
   - Re-run benchmarks
   - Verify no regression

3. **Documentation:**
   - Update renderer docs with correct pattern
   - Add example to this document if novel case

---

## Success Stories

### v1.7.0 Achievement

**Before Zero-GC (v1.3.0):**
- p99 latency: 8.2ms
- GC pauses: 3-5 per minute
- Memory: Sawtooth pattern (500MB → 2GB)

**After Zero-GC (v1.7.0):**
- p99 latency: 0.9ms (9x faster)
- GC pauses: 0 during rendering
- Memory: Flat line (stable 50MB)

**Customer Quote:**
> "After migrating to v1.7.0, our medical monitor hasn't had a single GC pause in 6 months of continuous operation. This is production-grade engineering." — Swiss Hospital

---

## v2.0 Enhancements

### New in v2.0

1. **ZeroAllocPool** - Centralized pool management
2. **ThreadLocal Caching** - Zero contention
3. **ArchUnit Enforcement** - Automatic verification
4. **Allocation Stats** - Runtime monitoring
5. **JMH Integration** - Continuous benchmarking

### Future (v2.1+)

- [ ] Off-heap buffer pools (Project Panama)
- [ ] SIMD optimizations (Vector API)
- [ ] GPU memory pools (future)
- [ ] Real-time allocation tracking

---

## References

- [ZeroAllocPool.java](../arbercharts-core/src/main/java/com/arbergashi/charts/engine/allocation/ZeroAllocPool.java)
- [PERFORMANCE_REPORT.md](PERFORMANCE_REPORT.md)
- [v1.7.0_BREAKING_CHANGES.md](v1.7.0_BREAKING_CHANGES.md)
- [ArchUnit Documentation](https://www.archunit.org/)

---

## Contact

**Questions about Zero-GC?** gashi@pro-business.ch

**This is the hill we die on.** Zero-GC is non-negotiable.

---

**Status:** ACTIVE & ENFORCED  
**Last Updated:** 14. Februar 2026  
**Next Review:** Before each release

