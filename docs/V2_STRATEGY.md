# ArberCharts 2.0 - Strategic Focus Shift

**Document Version:** 1.0  
**Date:** 14. Februar 2026  
**Status:** APPROVED  
**Author:** Arber Gashi

---

## Executive Decision

ArberCharts 2.0 eliminiert alle experimentellen Plattform-Bridges (Swift, Qt, Compose) und fokussiert sich vollständig auf **Java 25, Swing Bridge (Enterprise-Grade) und Spring Boot Integration**.

---

## Rationale

### Warum diese Entscheidung?

#### 1. Market Reality Check
- **Swift Bridge Adoption:** <5 Kunden (geschätzt)
- **Qt Bridge Adoption:** <3 Kunden (geschätzt)
- **Compose Bridge Adoption:** <2 Kunden (geschätzt)
- **Swing Bridge Adoption:** 80%+ aller Kunden

#### 2. Technical Debt
- Native Bridges erfordern Platform-Specific Builds (macOS arm64, Linux x64, Windows x64)
- CI/CD-Komplexität: 10x höher als Pure-JVM
- Cross-Platform-Bugs: 60% der Support-Tickets

#### 3. Opportunity Cost
- Zeit für Native-Bridges: ~40% des Development-Budgets
- ROI: Negativ (mehr Kosten als Revenue)
- Alternative: Invest in Core-Features & Enterprise-Tooling

---

## v2.0 Module Architecture

### Behalten (Enhanced)

#### 1. arbercharts-core
**Status:** Enhanced  
**Java Version:** 25 (Baseline)  
**Changes:**
- Virtual Threads für Concurrent Rendering
- Vector API (final) statt Incubator
- Pattern Matching für Renderer-Dispatch
- Zero External Dependencies (bleibt)

#### 2. arbercharts-swing-bridge
**Status:** Major Enhancement → Enterprise-Grade  
**New Features:**
- FlatLaf 4.x Integration (2026 Release)
- WCAG 2.1 AA Accessibility
- High-DPI Rendering (150%, 200%, 250%)
- Dark/Light Mode Hot-Swapping
- Keyboard Navigation (Full)
- Screen Reader Support
- Touch-Friendly (for Surface/iPad-Stylus via Java AWT)

**Performance Targets:**
- Repaint Latency: <1ms (99th percentile)
- Memory Footprint: <50 MB for 10 charts
- Startup Time: <500ms cold, <100ms warm

#### 3. arbercharts-spring-boot-starter
**Status:** Major Enhancement → Production-Ready  
**New Features:**

**Auto-Configuration:**
```yaml
arbercharts:
  theme: dark
  export:
    enabled: true
    formats: [png, svg, pdf]
    directory: /tmp/charts
  performance:
    virtual-threads: true
    max-concurrent-renders: 10
```

**Actuator Integration:**
- `/actuator/charts/health` - Health Check
- `/actuator/charts/metrics` - Render Times, Memory
- `/actuator/charts/renderers` - Available Renderers

**Security Integration:**
- Export-Endpoints mit Spring Security schützbar
- CORS-Konfiguration für Chart-APIs
- OAuth2 Support für Enterprise-SSO

**WebFlux Support:**
```java
@RestController
class ChartController {
    @GetMapping("/chart/stream")
    Flux<ServerSentEvent<byte[]>> streamChart() {
        return chartService.renderStream()
            .map(png -> ServerSentEvent.of(png));
    }
}
```

#### 4. arbercharts-server-bridge
**Status:** Enhanced with Virtual Threads  
**Changes:**
- Loom Virtual Threads für Parallel-Rendering
- PNG/SVG/PDF ohne AWT-Headless-Hacks
- REST API Template (optional)

#### 5. arbercharts-demo
**Status:** Enhanced → Public Gallery  
**Changes:**
- Web-UI mit allen 158 Renderern
- Live-Code-Editor (Monaco-Editor)
- Export zu PNG/SVG/PDF
- Permalink-Sharing

---

### Entfernen (Deprecated → Removed)

#### 1. arbercharts-swift-bridge
**Status:** REMOVED in v2.0  
**Timeline:**
- v1.7.0-LTS: Supported bis Ende 2027
- v1.8.0: Marked `@Deprecated`
- v2.0.0: Removed from codebase

**Migration Path:**
```
Option 1: Stay on v1.7.0-LTS (Maintenance-Support bis 12/2027)
Option 2: Switch to Swing with FlatLaf (Look ähnlich wie macOS native)
Option 3: Custom Bridge Development (ab CHF 50K, separate Lizenz)
```

#### 2. arbercharts-qt-bridge
**Status:** REMOVED in v2.0  
**Reason:** Cross-platform build complexity, minimal adoption

**Migration Path:**
- Same as Swift Bridge

#### 3. arbercharts-compose-bridge
**Status:** REMOVED in v2.0  
**Reason:** Compose Desktop unreif, kleine Community

**Migration Path:**
```
Option 1: Switch to Swing Bridge (empfohlen)
Option 2: Direct Core Access via ArberCanvas (fortgeschritten)
```

---

## Technical Implementation Plan

### Phase 1: Deprecation (Q2 2026)

#### Code Changes

**1. Mark Bridges as Deprecated**

File: `arbercharts-swift-bridge/pom.xml`
```xml
<!-- DEPRECATED: Will be removed in v2.0 -->
<!-- Use arbercharts-swing-bridge instead -->
<!-- See docs/V2_MIGRATION_GUIDE.md -->
```

File: `arbercharts-swift-bridge/README.md`
```markdown
# ⚠️ DEPRECATED

This bridge is deprecated and will be removed in v2.0.

**Migration Options:**
1. Stay on v1.7.0-LTS (supported until December 2027)
2. Switch to Swing Bridge with FlatLaf
3. Contact gashi@pro-business.ch for custom bridge development

See: [V2_MIGRATION_GUIDE.md](../docs/V2_MIGRATION_GUIDE.md)
```

**2. Update Root README.md**

Add prominent warning:
```markdown
## ⚠️ v2.0 Breaking Changes

ArberCharts 2.0 focuses exclusively on Java/JVM platforms.

**Removed in v2.0:**
- Swift Bridge (macOS/iOS)
- Qt Bridge (native C++)
- Compose Bridge (Kotlin)

**Enhanced in v2.0:**
- Swing Bridge (Enterprise-Grade)
- Spring Boot Starter (Production-Ready)
- Java 25 Baseline (Virtual Threads, Vector API)

See [V2_STRATEGY.md](docs/V2_STRATEGY.md) for details.
```

### Phase 2: Enhancement (Q2-Q3 2026)

#### Swing Bridge Enhancement

**File:** `arbercharts-swing-bridge/pom.xml`

Add FlatLaf 4.x:
```xml
<dependency>
    <groupId>com.formdev</groupId>
    <artifactId>flatlaf</artifactId>
    <version>4.0.0</version> <!-- Update when released -->
</dependency>
<dependency>
    <groupId>com.formdev</groupId>
    <artifactId>flatlaf-extras</artifactId>
    <version>4.0.0</version>
</dependency>
```

**New Class:** `AccessibilitySupport.java`
```java
package com.arbergashi.charts.platform.swing.a11y;

/**
 * WCAG 2.1 AA Accessibility Support for ArberCharts.
 * 
 * Features:
 * - Screen Reader Support (JAWS, NVDA, VoiceOver)
 * - Keyboard Navigation (Tab, Arrow Keys, Enter)
 * - High Contrast Themes
 * - Focus Indicators
 * 
 * @since 2.0.0
 */
public final class AccessibilitySupport {
    // Implementation
}
```

**New Class:** `HighDpiRenderer.java`
```java
package com.arbergashi.charts.platform.swing.render;

/**
 * High-DPI Rendering Support (150%, 200%, 250%)
 * 
 * Automatically detects display scaling and adjusts:
 * - Font sizes
 * - Line widths
 * - Icon sizes
 * - Grid spacing
 * 
 * @since 2.0.0
 */
public final class HighDpiRenderer {
    // Implementation
}
```

#### Spring Boot Starter Enhancement

**File:** `arbercharts-spring-boot-starter/src/main/java/.../ChartAutoConfiguration.java`

```java
package com.arbergashi.charts.spring.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-Configuration for ArberCharts in Spring Boot applications.
 * 
 * Provides:
 * - Theme configuration from application.yml
 * - Export service with file management
 * - Actuator endpoints for monitoring
 * - Security integration for protected exports
 * 
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ChartModel.class)
@EnableConfigurationProperties(ChartsProperties.class)
public class ChartAutoConfiguration {
    
    @Bean
    public ChartService chartService(ChartsProperties properties) {
        return new ChartService(properties);
    }
    
    @Bean
    public ChartExportService chartExportService(ChartsProperties properties) {
        return new ChartExportService(properties);
    }
}
```

**New File:** `ChartsProperties.java`
```java
package com.arbergashi.charts.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ArberCharts.
 * 
 * Example application.yml:
 * <pre>
 * arbercharts:
 *   theme: dark
 *   export:
 *     enabled: true
 *     formats: [png, svg, pdf]
 *     directory: /tmp/charts
 *   performance:
 *     virtual-threads: true
 *     max-concurrent-renders: 10
 * </pre>
 * 
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "arbercharts")
public class ChartsProperties {
    private String theme = "dark";
    private ExportProperties export = new ExportProperties();
    private PerformanceProperties performance = new PerformanceProperties();
    
    // Getters/Setters
    
    public static class ExportProperties {
        private boolean enabled = true;
        private List<String> formats = List.of("png", "svg", "pdf");
        private String directory = System.getProperty("java.io.tmpdir");
        // Getters/Setters
    }
    
    public static class PerformanceProperties {
        private boolean virtualThreads = true;
        private int maxConcurrentRenders = 10;
        // Getters/Setters
    }
}
```

**New File:** `ChartActuatorEndpoint.java`
```java
package com.arbergashi.charts.spring.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Actuator endpoint for ArberCharts monitoring.
 * 
 * Available at: /actuator/charts
 * 
 * Provides:
 * - Health status
 * - Render metrics (avg time, p99, p999)
 * - Memory usage
 * - Active renderers
 * 
 * @since 2.0.0
 */
@Endpoint(id = "charts")
public class ChartActuatorEndpoint {
    
    @ReadOperation
    public ChartHealth health() {
        return new ChartHealth(
            renderCount,
            avgRenderTime,
            memoryUsage
        );
    }
}
```

#### Core Enhancement (Java 25)

**File:** `arbercharts-core/pom.xml`

Update compiler:
```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

**New Class:** `VirtualThreadRenderer.java`
```java
package com.arbergashi.charts.engine.concurrent;

import java.util.concurrent.Executors;

/**
 * Virtual Thread based concurrent rendering.
 * 
 * Uses Project Loom Virtual Threads for:
 * - Parallel chart rendering (multiple charts)
 * - Parallel renderer execution (within one chart)
 * - Non-blocking server-side rendering
 * 
 * Performance: 100+ concurrent renders without thread pool exhaustion
 * 
 * @since 2.0.0
 */
public final class VirtualThreadRenderer {
    private final ExecutorService executor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public CompletableFuture<byte[]> renderAsync(ChartModel model) {
        return CompletableFuture.supplyAsync(
            () -> render(model),
            executor
        );
    }
}
```

### Phase 3: Removal (Q4 2026)

**Delete Modules:**
```bash
# Q4 2026 - After v2.0.0-beta testing
rm -rf arbercharts-swift-bridge
rm -rf arbercharts-qt-bridge
rm -rf arbercharts-compose-bridge
```

**Update Root pom.xml:**
```xml
<modules>
    <module>arbercharts-core</module>
    <module>arbercharts-swing-bridge</module>
    <module>arbercharts-spring-boot-starter</module>
    <module>arbercharts-server-bridge</module>
    <module>arbercharts-starter</module>
    <module>arbercharts-visual-verifier</module>
    <module>arbercharts-demo</module>
    <!-- REMOVED in v2.0:
    <module>arbercharts-swift-bridge</module>
    <module>arbercharts-qt-bridge</module>
    <module>arbercharts-compose-bridge</module>
    -->
</modules>
```

---

## Communication Plan

### Internal
- [x] Strategic decision documented (this file)
- [ ] Team alignment meeting (if applicable)
- [ ] Update roadmap in all docs

### External (Customers)

#### Email Template
```
Subject: ArberCharts 2.0 - Strategic Focus on Java/Spring

Dear [Customer],

We're excited to announce ArberCharts 2.0, which focuses exclusively 
on Java 25, Enterprise-Swing, and Spring Boot integration.

**What's changing:**
- Swift, Qt, and Compose bridges will be removed in v2.0
- v1.7.0-LTS continues to be supported until December 2027
- Enhanced Swing Bridge with FlatLaf 4.x and Accessibility
- Production-ready Spring Boot Starter with Auto-Configuration

**Why this change:**
This allows us to deliver the absolute best JVM charting experience
instead of maintaining 4 different platforms.

**What you need to do:**
- If you use Swift/Qt/Compose: See migration guide [link]
- If you use Swing/Spring: You'll love the enhancements!

Questions? Reply to this email or schedule a call: [calendly]

Best regards,
Arber Gashi
```

### Public Communication

#### Blog Post Title
"ArberCharts 2.0: All-In on Java 25 & Spring Boot"

#### Key Messages
1. **Focus = Quality:** One excellent platform > four mediocre ones
2. **Enterprise-First:** Swing + Spring is the heart of Java enterprise
3. **Modern Java:** Virtual Threads, Vector API, Pattern Matching
4. **Migration Support:** We help every customer transition smoothly

---

## Success Metrics (v2.0)

### Technical
- [ ] Test Coverage: 12% → 50%
- [ ] Build Time: -40% (no native builds)
- [ ] CI Cost: -60% (GitHub Actions only)
- [ ] Support Tickets: -50% (simpler architecture)

### Business
- [ ] Maven Central Downloads: 10K/month (von 0)
- [ ] Spring Initializr Integration: Live
- [ ] ARR Growth: +150% YoY
- [ ] Churn Rate: <10%

### Adoption
- [ ] 50+ GitHub Stars (von ~10)
- [ ] 10+ Blog Posts/Tutorials (von 0)
- [ ] 5+ Conference Talks (JUG, Devoxx, etc.)
- [ ] 100+ Discord Members

---

## Risk Mitigation

### Risk 1: Customer Churn (Native Bridge Users)
**Mitigation:**
- v1.7.0-LTS Support bis Ende 2027 (22 Monate)
- Free Custom Bridge Development für Top-Kunden
- Aggressive Migration-Support (1:1 Calls)

### Risk 2: Negative Community Reaction
**Mitigation:**
- Transparente Kommunikation (Blog, Email)
- Technical reasoning klar darstellen
- Show v2.0 Benefits früh (Beta Previews)

### Risk 3: Competitor Takes Native-Market
**Mitigation:**
- Native-Market ist <5% des JVM-Markets
- Fokus auf 95% statt 5% ist strategisch richtig
- Custom Bridges weiterhin möglich (kostenpflichtig)

---

## Conclusion

ArberCharts 2.0 ist die richtige Entscheidung für:
- **Technische Exzellenz:** 1 exzellente Bridge > 4 mittelmäßige
- **Business-Fokus:** 95% des Markets ist Pure-JVM
- **Nachhaltigkeit:** 40% weniger Maintenance-Burden

**Next Steps:**
1. Deprecation Warnings in v1.8.0 (Q2 2026)
2. Enhancement Development (Q2-Q3 2026)
3. v2.0.0-beta Release (Q4 2026)
4. v2.0.0 Final (Q1 2027)

---

**Approved by:** Arber Gashi  
**Date:** 14. Februar 2026  
**Status:** ACTIVE

