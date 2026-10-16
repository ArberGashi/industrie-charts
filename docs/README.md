# ArberCharts documentation

The current source tree and Maven installation examples target **2.2.0**. Documents named for older versions remain historical references; they do not establish the current implementation or verification status.

## Start here

- [Project README and installation](../README.md)
- [Quick Start](QUICK_START.md)
- [User Guide](USER_GUIDE.md)
- [Renderer Catalog](RENDERER_CATALOG.md)
- [Release notes index](../RELEASE_NOTES.md)
- [Licensing](LICENSING.md)

## Architecture and verification

- [Rendering architecture and 2.2.0 migration](RENDERING_ARCHITECTURE.md)
- [Verification strategy and test limits](VERIFICATION_STRATEGY.md)
- [Independent renderer, axes and UI audit — 16 October 2026](audits/2026-10-16-rendering/README.md)
- [Dependency updates](DEPENDENCY_UPDATES.md)
- [GraalVM metadata ownership and validation limits](GRAALVM_AUDIT.md)
- [Concurrency model](CONCURRENCY_MODEL.md)
- [Logging policy](LOGGING_POLICY.md)
- [Design policies](DOCTRINE_POLICY.md)
- [Recorded performance report](PERFORMANCE_REPORT.md)

The independent audit separates executed reproductions, source review and inventory coverage. Its remaining visual and numerical findings are follow-up work; primitive backend tests and catalog smoke tests do not constitute full visual approval.

## Applications and delivery

- [Swing demo module](../arbercharts-demo/)
- [Spring Boot demo](../arbercharts-spring-boot-demo/README.md)
- [Release preparation and publishing](../RELEASING.md)
- [Commercial support](PRICING.md)
- [Service level agreement](SLA.md) / [German version](SLA_DE.md)
