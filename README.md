# ArberCharts 2.0.0

![Zero-GC](https://img.shields.io/badge/Zero--GC-Guaranteed-brightgreen) ![Java 25](https://img.shields.io/badge/Java-25-orange) ![Renderers](https://img.shields.io/badge/Renderers-157-blue) ![License](https://img.shields.io/badge/License-Proprietary-red)

**ArberCharts** is an enterprise-grade Java charting framework built for mission-critical systems.
It delivers deterministic rendering, zero-GC performance, and a deep renderer catalog across
Financial, Medical, Statistical, Specialized, and Analysis domains.

## 🚀 What's New in v2.0.0

- **Java 25 Baseline** — Virtual Threads, Vector API, Pattern Matching
- **Streamlined Architecture** — Focus on Swing + Spring Boot
- **157 Production Renderers** — All verified and documented
- **Zero-GC Guarantee** — No allocations in hot render paths
- **Visual Verifier** — Server-side testing platform included

## ⚡ Quick Start (2 Lines of Code)

```java
Charts.lineChart()
    .addData(1, 10, 2, 20, 3, 15, 4, 30)
    .show();
```

**That's it!** Beautiful, interactive charts with zero configuration.

### More Examples

```java
// Real-time streaming (4 lines)
Charts.streamingLineChart(1000)
    .title("CPU Usage")
    .startStreaming(() -> getCpuUsage())
    .show();

// With styling (6 lines)
Charts.lineChart()
    .title("Sales 2026")
    .xLabel("Month")
    .yLabel("Revenue (CHF)")
    .theme("obsidian")
    .addData(1, 100, 2, 150, 3, 200)
    .show();
```

**[→ Full Quick Start Guide](docs/QUICK_START.md)**

---

- **ZERO-GC Rendering** 🔥 - Guaranteed zero allocations in hot paths, <1ms p99 latency
- **158 renderers** with production‑grade visuals
- **Lock‑free streaming models** for real‑time data (medical/finance)
- **Headless core** with platform bridges for desktop and server
- **Deterministic playback** for audits and forensic replays
- **Export pipeline** for PNG/SVG/PDF

## 📦 Maven Dependencies

### Swing Applications
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Spring Boot Applications
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Full Bundle (All-in-One)
```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Platform Support

| Platform | Module | Status |
|----------|--------|--------|
| **Swing Desktop** | `arbercharts-swing-bridge` | ✅ Production |
| **Spring Boot** | `arbercharts-spring-boot-starter` | ✅ Production |
| **Server Headless** | `arbercharts-server-bridge` | ✅ Production |

## Documentation

Public docs shipped with this repository:
- `USER_GUIDE.md`
- `RENDERER_CATALOG.md`
- `LICENSING.md`
- `RELEASE_NOTES_2.0.0.md`
- `MIGRATION_GUIDE.md`

## Demo & Showcase

Run the demo application:
```bash
cd arbercharts-demo
mvn exec:java
```

## System Requirements

- **Java 25** required (baseline for v2.0)
- Any OS with JVM support (Windows, macOS, Linux)

## License

ArberCharts binaries (JAR files) are licensed under the **MIT License**.

This means you can:
- ✅ Use ArberCharts in commercial and non-commercial projects
- ✅ Distribute ArberCharts with your applications
- ✅ Modify and extend through public APIs

**Note**: The source code remains proprietary. See [LICENSE](LICENSE) and [BINARY-LICENSE.md](BINARY-LICENSE.md) for details.

## Support

gashi@pro-business.ch  
https://www.arbergashi.com
