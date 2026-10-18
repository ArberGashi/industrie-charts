# Logging policy

ArberCharts **2.2.0** uses limited JDK logging in Core and delegates application
observability to the bridges and host application. Core has no SLF4J or Logback
dependency.

## Existing diagnostics

- Core models use `java.util.logging` for rejected listener dispatch and isolated
  listener failures. These warnings describe error paths that an application
  should investigate rather than silently suppress.
- `ArberBridgeFactory` logs provider selection and discovery failures. `ChartI18N`
  has fine-level missing-resource diagnostics. The predictive shadow renderer
  can emit its configured prediction-delta audit statistics.
- `ServerRenderService` uses `System.Logger` for debug timing output and can
  record Micrometer timers, counters and pool gauges when a registry is supplied.
  The Spring Boot application supplies its own logging configuration.

## Integration rules

Avoid adding per-point or per-frame log messages to rendering loops. Keep
operational request logging, correlation identifiers, sampling and rate limits
at the application boundary. Configure the JDK logger or the application's
logging bridge explicitly when those diagnostics must reach a shared sink.

Logging, listeners and metric recording can allocate and affect frame time.
Measure them as part of the actual workload; this policy does not imply a
zero-allocation or regulated-use guarantee. See the
[verification strategy](VERIFICATION_STRATEGY.md) for performance measurement
requirements.
