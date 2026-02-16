# ArberCharts 2.0.0 Release Notes

**Release Date:** February 14, 2026  
**Java Baseline:** 25  
**Spring Boot:** 4.0.2

---

## 🎯 Strategic Focus

ArberCharts 2.0.0 represents a strategic refocus on **JVM-only deployment**:
- **Swing Bridge** — Enterprise desktop applications
- **Spring Boot Starter** — Server-side & web applications
- **Core Library** — Headless rendering engine

### Removed Modules
The following platform bridges have been **removed** to ensure quality focus:
- ❌ Compose Bridge (Kotlin Desktop)
- ❌ Qt Bridge (native C++)
- ❌ Swift Bridge (macOS/iOS)

---

## ✨ New Features

### Zero-GC Architecture
- **Guaranteed zero allocations** in hot render paths
- StrokeCache and ColorCache fully integrated
- Lock-free CircularChartModel for real-time streaming
- Memory-stable at 10M+ data points

### Java 25 Features
- Virtual Threads ready
- Vector API integration (`jdk.incubator.vector`)
- Pattern Matching throughout
- Record Classes for DTOs

### Visual Verifier 2.0
Complete redesign of the testing platform:
- Clean layered architecture (Controller → Service → DTO)
- REST APIs for all 157 renderers
- Performance benchmarking with p50/p99/p999 metrics
- Web UI for visual verification

**Endpoints:**
```
GET /api/health              → Health status
GET /api/renderers           → All 157 renderers
GET /api/renderers/stats     → Category statistics
GET /api/render/{renderer}   → Render to PNG
GET /api/benchmark/{renderer} → Performance metrics
```

---

## 📊 Renderer Catalog

**157 Production Renderers** across 11 categories:

| Category     | Count |
|--------------|-------|
| Specialized  | 37    |
| Financial    | 29    |
| Analysis     | 19    |
| Standard     | 17    |
| Medical      | 17    |
| Statistical  | 17    |
| Circular     | 15    |
| Common       | 2     |
| Predictive   | 2     |
| Security     | 1     |
| Forensic     | 1     |

---

## 🏗️ Architecture

```
arbercharts-core              → Headless rendering engine
arbercharts-server-bridge     → Server-side PNG/SVG rendering
arbercharts-spring-boot-starter → Spring Boot auto-configuration
arbercharts-swing-bridge      → Desktop Swing integration
arbercharts-starter           → All-in-one bundle
arbercharts-visual-verifier   → Testing platform
arbercharts-demo              → Swing demo application
```

---

## 🚀 Quick Start

### Maven
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Simple Chart
```java
Charts.lineChart()
    .addData(1, 10, 2, 20, 3, 15, 4, 30)
    .show();
```

### Spring Boot
```java
@Autowired
private ServerRenderService renderService;

byte[] png = renderService.renderToPng(model, size, theme, renderer);
```

---

## ⚡ Performance

- **Render Time:** <5ms average
- **Memory:** Zero allocations in hot path
- **Throughput:** 200+ renders/second
- **Startup:** <1 second

---

## 📋 Migration from 1.7.0-LTS

1. Update version: `1.7.0-LTS` → `2.0.0`
2. Remove native bridge dependencies (Compose/Qt/Swift)
3. Java 25 is now required
4. All APIs remain compatible

---

## 📞 Support

- **Email:** gashi@pro-business.ch
- **Web:** https://www.arbergashi.com
- **GitHub:** https://github.com/ArberGashi/charts

---

**ArberCharts 2.0.0 — Enterprise-Grade Java Charting**

