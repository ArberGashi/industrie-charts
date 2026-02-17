# ArberCharts v2.0.0 - MIT License Release

**Release Date:** February 16, 2026

## 🎉 Major Licensing Change

ArberCharts v2.0.0 is now available under the **MIT License** for all binary artifacts (JAR files).

### What This Means:
- ✅ **Free to use** in commercial and non-commercial projects
- ✅ **No licensing fees** required
- ✅ **Distribution allowed** with your applications
- ✅ **Modification allowed** through public APIs

### Source Code:
The source code remains **proprietary**. Enterprise source code licensing is available for customers who need custom modifications or deeper integration.

See [LICENSE](LICENSE) and [BINARY-LICENSE.md](BINARY-LICENSE.md) for full details.

---

## 🚀 What's New in v2.0.0

### Java 25 Baseline
- **Virtual Threads** - Leveraged for background tasks
- **Vector API** - SIMD-accelerated rendering paths
- **Pattern Matching** - Cleaner, safer code
- **Structured Concurrency** - Better resource management

### Streamlined Architecture
- **Focus on Swing + Spring Boot** - Removed Compose, Qt, and Swift bridges
- **157 Production Renderers** - All verified and documented
- **Demo Application** - Professional renderer showcase for customer delivery
- **Zero-GC Guarantee** - No allocations in hot render paths

### Module Structure
```
arbercharts-core                  // Core rendering engine
arbercharts-swing-bridge          // Swing/Desktop integration
arbercharts-server-bridge         // Server-side rendering
arbercharts-spring-boot-starter   // Spring Boot auto-configuration
arbercharts-demo                  // Interactive demo application
```

---

## 📦 Maven Coordinates

```xml
<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-core</artifactId>
    <version>2.0.0</version>
</dependency>

<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-swing-bridge</artifactId>
    <version>2.0.0</version>
</dependency>

<dependency>
    <groupId>com.arbergashi</groupId>
    <artifactId>arbercharts-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## 🎨 Visual Improvements

### Demo Application
- Modern split-pane UI with smooth divider
- Updated FlatLaf Dark/Light themes
- Optimized axis label sizing (font size: 7pt)
- Refined crosshair indicators (smaller, cleaner)
- Better chart panel margins

### Typography
- Inter font family for all UI text
- Consistent sizing across all elements
- High-DPI/Retina display optimized

---

## 🔧 Platform Scope in v2.0.0

v2.0.0 focuses on the actively maintained platform modules:

- `arbercharts-core`
- `arbercharts-swing-bridge`
- `arbercharts-server-bridge`
- `arbercharts-spring-boot-starter`
- `arbercharts-demo`

---

## 📊 Performance

- **Zero-GC rendering** - Fully implemented and verified
- **157 renderers** - All production-ready
- **60 FPS real-time** - Smooth data visualization
- **SIMD acceleration** - Vector API for hot paths
- **Efficient memory** - Pooled resources, no allocations

---

## 🛠️ System Requirements

- **Java 25** (required)
- Windows 11, macOS 15+, or Ubuntu 24.04 LTS
- Modern JVM with Vector API support

---

## 📝 License

**MIT License** for binary artifacts (JAR files).

Source code remains proprietary. Enterprise source code licensing available.

See [LICENSE](LICENSE) for full license text.

---

## 🆘 Support

### Community Support (Free)
- Email: gashi@pro-business.ch
- Documentation: https://www.arbergashi.com/javadoc

### Enterprise Support (Paid)
- Priority support (24h response)
- Source code access
- Custom development
- SLA agreements

Contact: gashi@pro-business.ch

---

## 🔗 Links

- **Website:** https://www.arbergashi.com
- **Downloads:** https://www.arbergashi.com/downloads
- **JavaDoc:** https://www.arbergashi.com/javadoc
- **Demo:** bundled in `arbercharts-demo` release artifact

---

**Copyright (c) 2024-2026 Arber Gashi. All rights reserved.**

MIT License for binaries. Source code proprietary.
