# Test Run Report (v1.7.0-LTS)

## Execution Summary
- Command: `mvn -pl arbercharts-core test`
- Result: **SUCCESS**
- Tests: 67 run, 0 failures, 0 errors, 2 skipped (ConcurrentModelTest assumptions)
- Last run: 2026-01-30 17:21:43 +01:00

## Notes
- Visual regression smoke tests executed as part of the default test suite.
- Property-based test executed: `CircularChartModelPropertyTest` (jqwik).
- Warnings observed: incubator vector module notice and SLF4J no-provider (expected for core).

## Zero-Allocation Guidelines
- Command: `mvn -pl arbercharts-core -Pguidelines-check verify`
- Result: **SUCCESS**
- Renderer allocation violations corrected in core renderers; guidelines check now passes.
- Last run: 2026-01-30 17:22:09 +01:00
