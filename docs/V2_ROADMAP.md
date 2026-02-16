# ArberCharts v2.0 Roadmap

**Version:** 1.0  
**Date:** 14. Februar 2026  
**Status:** ACTIVE  

---

## Vision Statement

**ArberCharts 2.0 makes Java the best platform for enterprise charting.**

By focusing exclusively on Java 25, Swing, and Spring Boot, we deliver:
- ⚡ Best-in-class performance (Virtual Threads, Vector API)
- 🎨 Modern enterprise UI (FlatLaf 4.x, Accessibility)
- 🍃 Zero-config Spring Boot integration
- 🔒 Production-grade stability and support

---

## Release Timeline

### Q2 2026 (April - Juni)

#### Milestone: Deprecation & Foundation

**Date:** April 15, 2026

**Tasks:**

1. **Documentation** (Week 1-2)
   - [x] V2_STRATEGY.md created
   - [x] V2_MIGRATION_GUIDE.md created
   - [x] AUDIT.md updated with v2.0 analysis
   - [x] **ZERO_GC_POLICY.md created** 🔥
   - [x] **ZERO_GC_IMPLEMENTATION.md created** 🔥
   - [ ] Customer communication email template
   - [ ] Blog post draft: "ArberCharts 2.0: All-In on Java"
   - [ ] FAQ page on website

2. **Deprecation Notices** (Week 2-3)
   - [x] DEPRECATION_NOTICE.md in swift-bridge
   - [x] DEPRECATION_NOTICE.md in qt-bridge
   - [x] DEPRECATION_NOTICE.md in compose-bridge
   - [ ] @Deprecated annotations in all native bridge classes
   - [ ] Build warnings when using native bridges
   - [ ] IDE warning hints (via annotations)

3. **Core Enhancements - Phase 1** (Week 3-8)
   - [ ] Java 25 GA released (expected April 2026)
   - [x] Update maven.compiler.release to 25
   - [ ] Remove incubator flag from Vector API
   - [x] **Zero-GC Pool Implementation (ZeroAllocPool.java)** 🔥
   - [x] **Zero-GC ArchUnit Tests** 🔥
   - [x] Virtual Threads for CircularChartModel concurrent access (skeleton)
   - [ ] Pattern Matching in RendererRegistry
   - [ ] SequencedCollections for deterministic iteration

4. **Swing Bridge Enhancement - Phase 1** (Week 4-8)
   - [ ] FlatLaf 4.0 integration (when released)
   - [ ] Accessibility Layer foundation
     - [ ] Screen reader support (AccessibleContext)
     - [ ] Keyboard navigation framework
     - [ ] Focus indicators
   - [ ] High-DPI detection logic
   - [ ] Look & Feel hot-swapping without restart

5. **Spring Boot Starter - Phase 1** (Week 6-8)
   - [x] ChartsProperties class
   - [x] ChartAutoConfiguration skeleton
   - [ ] YAML schema generation
   - [ ] Spring Boot DevTools support
   - [ ] Basic Actuator endpoint (/actuator/charts/health)

**Deliverables:**
- v1.8.0 Release with deprecation warnings
- Blog post published
- Customer emails sent
- Migration guide available online

---

### Q3 2026 (Juli - September)

#### Milestone: Enhancement & Testing

**Date:** September 15, 2026

**Tasks:**

1. **Core Enhancements - Phase 2** (Week 1-4)
   - [ ] Virtual Thread Executor for Server-Rendering
   - [ ] Lock-free enhancements using VarHandle improvements
   - [ ] Vector API batch processing for spatial renderers
   - [ ] Performance benchmarks vs v1.7.0
   - [ ] Memory profiling and optimization

2. **Swing Bridge Enhancement - Phase 2** (Week 1-8)
   - [ ] Accessibility - WCAG 2.1 AA compliance
     - [ ] ARIA-like role descriptions
     - [ ] Keyboard shortcuts documentation
     - [ ] High contrast themes
   - [ ] High-DPI rendering (150%, 200%, 250%)
     - [ ] Font scaling
     - [ ] Line width scaling
     - [ ] Icon scaling
   - [ ] Touch support for Surface/iPad-Stylus
   - [ ] Performance: <1ms repaint latency
   - [ ] Unit tests for accessibility

3. **Spring Boot Starter - Phase 2** (Week 5-12)
   - [ ] Full Actuator integration
     - [ ] /actuator/charts/metrics
     - [ ] /actuator/charts/renderers
     - [ ] Micrometer timers for render operations
   - [ ] Security integration
     - [ ] Export endpoints with @PreAuthorize
     - [ ] CORS configuration
     - [ ] OAuth2 ResourceServer support
   - [ ] WebFlux support
     - [ ] Reactive chart rendering
     - [ ] Server-Sent Events for streaming
   - [ ] Full integration tests

4. **Testing** (Week 9-12)
   - [ ] Test coverage 12% → 50%
   - [ ] JCStress concurrency tests
   - [ ] Property-based tests (jqwik) expansion
   - [ ] Visual regression tests expansion
   - [ ] Performance regression tests
   - [ ] Accessibility automated tests

5. **Documentation** (Week 10-12)
   - [ ] Complete Javadoc for all public APIs
   - [ ] Package-info.java for all packages
   - [ ] Spring Boot integration guide
   - [ ] Accessibility guide
   - [ ] Performance tuning guide

**Deliverables:**
- v2.0.0-alpha1 (internal testing)
- 50% test coverage achieved
- Complete documentation

---

### Q4 2026 (Oktober - Dezember)

#### Milestone: Beta & Maven Central

**Date:** December 15, 2026

**Tasks:**

1. **Remove Native Bridges** (Week 1)
   - [ ] Delete arbercharts-swift-bridge/
   - [ ] Delete arbercharts-qt-bridge/
   - [ ] Delete arbercharts-compose-bridge/
   - [ ] Update root pom.xml
   - [ ] Update build scripts
   - [ ] Update CI/CD pipelines

2. **Beta Testing** (Week 2-8)
   - [ ] v2.0.0-beta1 release
   - [ ] Beta customer program (10-20 customers)
   - [ ] Feedback collection
   - [ ] Bug fixes
   - [ ] Performance tuning
   - [ ] v2.0.0-beta2 release (if needed)

3. **Maven Central Preparation** (Week 3-6)
   - [ ] Sonatype OSSRH account setup
   - [ ] GPG signing keys
   - [ ] pom.xml metadata completion
   - [ ] Javadoc JAR generation
   - [ ] Sources JAR generation
   - [ ] Test deployment to staging

4. **Examples & Demos** (Week 6-10)
   - [ ] /examples directory with 20+ samples
   - [ ] SimpleLineChart.java "Hello World"
   - [ ] Spring Boot example project
   - [ ] Medical ECG example
   - [ ] Financial candlestick example
   - [ ] Real-time streaming example
   - [ ] Public demo gallery website

5. **Video Content** (Week 8-12)
   - [ ] "ArberCharts in 5 Minutes" tutorial
   - [ ] "Building a Medical ECG Chart" tutorial
   - [ ] "Real-Time Financial Dashboard" tutorial
   - [ ] "Spring Boot Integration" tutorial
   - [ ] "Accessibility Features" showcase
   - [ ] YouTube channel setup
   - [ ] Playlist creation

6. **Final Documentation** (Week 10-12)
   - [ ] Complete USER_GUIDE v2.0
   - [ ] Complete RENDERER_CATALOG v2.0
   - [ ] Spring Boot integration examples
   - [ ] Migration success stories
   - [ ] Performance benchmarks published
   - [ ] Website update with v2.0 content

**Deliverables:**
- v2.0.0-rc1 (Release Candidate)
- Maven Central test deployment
- Public demo gallery live
- 5+ YouTube videos
- Beta customer feedback incorporated

---

### Q1 2027 (Januar - März)

#### Milestone: v2.0.0 Final Release

**Date:** March 15, 2027

**Tasks:**

1. **Release Preparation** (Week 1-2)
   - [ ] Final bug fixes from RC
   - [ ] Performance benchmarks vs competitors
   - [ ] Security audit
   - [ ] License compliance check
   - [ ] Release notes finalization

2. **Maven Central Go-Live** (Week 2)
   - [ ] Production deployment to Maven Central
   - [ ] Verify artifacts availability
   - [ ] Test dependency resolution
   - [ ] Update website download links

3. **Launch Activities** (Week 3-4)
   - [ ] v2.0.0 Final release
   - [ ] Blog post: "ArberCharts 2.0 is Here"
   - [ ] Press release
   - [ ] Social media campaign
   - [ ] Email to all customers
   - [ ] Reddit/HackerNews posts

4. **Community Building** (Week 3-8)
   - [ ] Discord server launch
   - [ ] GitHub Discussions activation
   - [ ] First community office hours
   - [ ] Welcome package for new users
   - [ ] Contributor guidelines

5. **Ecosystem Integration** (Week 4-12)
   - [ ] Spring Initializr pull request
   - [ ] IntelliJ IDEA plugin (optional)
   - [ ] Maven archetypes
   - [ ] Gradle plugin (optional)
   - [ ] Conference talk submissions (Devoxx, JavaOne, etc.)

6. **Customer Success** (Week 1-12)
   - [ ] Migration support for all v1.7 customers
   - [ ] 10+ Reference customer case studies
   - [ ] Success metrics tracking
   - [ ] Customer feedback surveys

**Deliverables:**
- ✅ v2.0.0 Final Release
- ✅ Maven Central availability
- ✅ Discord community live
- ✅ 10+ Reference customers
- ✅ Spring Initializr integration

---

## Success Metrics

### Technical Metrics

| Metric | v1.7.0 Baseline | v2.0 Target | Status |
|--------|-----------------|-------------|--------|
| Test Coverage | 12% | 50% | 🟡 In Progress |
| Build Time | 5 min | 3 min | ⚪ Not Started |
| CI Cost/Month | CHF 100 | CHF 40 | ⚪ Not Started |
| Module Count | 10 | 7 | 🟡 Planned |
| Lines of Code | ~50K | ~45K | ⚪ Not Started |

### Performance Metrics

| Metric | v1.7.0 | v2.0 Target | Status |
|--------|--------|-------------|--------|
| Render Latency (p99) | <5ms | <1ms | ⚪ Not Started |
| Memory Footprint | 50 MB | 40 MB | ⚪ Not Started |
| Startup Time (cold) | 1s | 500ms | ⚪ Not Started |
| Concurrent Renders | 10 | 100+ | ⚪ Not Started |

### Business Metrics

| Metric | Current | Q1 2027 Target | Status |
|--------|---------|----------------|--------|
| ARR | CHF 60-120K | CHF 300-500K | ⚪ Tracking |
| Customers | 5-10 | 30-50 | ⚪ Tracking |
| Maven Downloads/Month | 0 | 10K | ⚪ Not Started |
| GitHub Stars | ~10 | 100+ | ⚪ Tracking |
| Discord Members | 0 | 100+ | ⚪ Not Started |

### Adoption Metrics

| Metric | Current | Q1 2027 Target | Status |
|--------|---------|----------------|--------|
| Blog Posts/Tutorials | 0 | 10+ | ⚪ Not Started |
| Conference Talks | 0 | 5+ | ⚪ Not Started |
| YouTube Views | 0 | 10K+ | ⚪ Not Started |
| Spring Initializr Uses | 0 | 1K+ | ⚪ Not Started |

---

## Resource Requirements

### Development Time

| Phase | Effort | Calendar Time |
|-------|--------|---------------|
| Q2 2026 | 8 weeks | 2 months |
| Q3 2026 | 12 weeks | 3 months |
| Q4 2026 | 12 weeks | 3 months |
| Q1 2027 | 8 weeks | 2 months |
| **Total** | **40 weeks** | **10 months** |

### Budget Estimate

| Category | Cost (CHF) |
|----------|------------|
| Development Time | 0 (internal) |
| Video Production | 3K |
| Demo Gallery Hosting | 1K/year |
| Maven Central Setup | 0 (free) |
| Marketing Materials | 2K |
| Conference Travel | 5K |
| **Total Year 1** | **11K** |

### Opportunity Cost

| What We're NOT Doing | Estimated Value | Justification |
|---------------------|-----------------|---------------|
| Swift Bridge Maintenance | -30K/year | Low ROI, high complexity |
| Qt Bridge Maintenance | -20K/year | Minimal adoption |
| Compose Bridge Maintenance | -10K/year | Ecosystem immature |
| **Total Savings** | **-60K/year** | **Reinvested in core** |

---

## Risk Register

### High-Priority Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Customer churn (native users) | High | Medium | v1.7 LTS support until 2027, free migration support |
| Java 25 delays | Medium | Low | Can use Java 21+ features initially |
| FlatLaf 4.x delays | Medium | Medium | Can use FlatLaf 3.x temporarily |
| Negative community reaction | High | Low | Transparent communication, show v2.0 benefits early |

### Medium-Priority Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Spring Boot breaking changes | Medium | Medium | Pin to specific versions, test thoroughly |
| Performance regressions | Medium | Low | Continuous benchmarking, JMH tests |
| Maven Central delays | Low | Medium | Start process early, have backup plan |

---

## Communication Plan

### Internal
- Weekly status updates
- Monthly roadmap reviews
- Quarterly planning sessions

### Customers
- Monthly newsletter with v2.0 progress
- Q2 2026: Deprecation announcement
- Q4 2026: Beta invitation
- Q1 2027: Launch announcement

### Public
- Q2 2026: Blog post announcing v2.0 strategy
- Q3 2026: Technical deep-dives (Virtual Threads, etc.)
- Q4 2026: Beta program announcement
- Q1 2027: Launch celebration

---

## Next Actions (Immediate)

**Week of Feb 14, 2026:**

- [x] Create V2_STRATEGY.md
- [x] Create V2_MIGRATION_GUIDE.md
- [x] Update AUDIT.md
- [x] Create DEPRECATION_NOTICE.md files
- [x] Update README.md with v2.0 warning
- [x] Create ChartsProperties.java skeleton
- [x] Create ChartAutoConfiguration.java skeleton

**Week of Feb 21, 2026:**

- [ ] Draft customer communication email
- [ ] Draft blog post: "ArberCharts 2.0: All-In on Java"
- [ ] Create FAQ page for website
- [ ] Add @Deprecated annotations to native bridges
- [ ] Test Java 25 Early Access build

**Week of Feb 28, 2026:**

- [ ] Publish blog post
- [ ] Send customer emails
- [ ] Activate GitHub Discussions
- [ ] Create v2.0 milestone in GitHub
- [ ] Start Core Java 25 migration

---

## Conclusion

ArberCharts v2.0 represents a strategic bet on the Java ecosystem. By focusing our efforts, we can deliver:

1. **The best Java charting framework** (not just "good enough")
2. **True enterprise readiness** (Spring Boot, Accessibility, Support)
3. **Sustainable growth** (lower costs, happier customers, better product)

**This is the right decision.**

---

**Status:** ACTIVE  
**Owner:** Arber Gashi  
**Last Updated:** 14. Februar 2026  
**Next Review:** March 15, 2026

