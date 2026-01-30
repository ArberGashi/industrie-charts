# Verification Strategy (Core)

## Goals
- Validate deterministic, zero-allocation render paths.
- Ensure thread-safe behavior under concurrent access.
- Provide audit-ready traceability for high-risk components.

## Pillars
1. **Property-based testing** for model correctness and invariants.
2. **Visual regression testing** for renderer output stability.
3. **Zero-allocation checks** using guidelines and static analysis.

## Compliance Matrix
| Component | Property Tests | Concurrency Tests | Visual Regression | Zero-Alloc |
| --- | --- | --- | --- | --- |
| CircularChartModel | ✓ (jqwik) | - | - | ✓ |
| Renderers | - | - | ✓ (Approval) | ✓ |
| Theme Engine | - | - | ✓ | ✓ |
| Playback/Forensics | ✓ | - | ✓ | ✓ |

## Recommended Tooling
- **jqwik** for property-based testing of data models.
- **Approval Tests** or image diff tooling for render regression.
- **RendererGuidelinesChecker** for zero-allocation enforcement.

## Execution Notes
- Property tests run via `mvn -pl arbercharts-core test`.
- Visual regression smoke tests are included in the core test suite and run with the default `test` phase.
- Tests must target the hottest paths: model ingestion, renderer draw cycles, and buffer reuse.
- Visual regression tests should run in a stable headless environment.
- Concurrency tests should include multi-reader/multi-writer contention.

## CI Matrix (Suggested)
| Job | Command | Purpose |
| --- | --- | --- |
| core-tests | mvn -pl arbercharts-core test | Unit + jqwik + visual smoke |
| guidelines-check | mvn -pl arbercharts-core -Pguidelines-check verify | Zero-alloc enforcement |

## Scope
This strategy documents verification expectations and is paired with targeted zero-allocation remediation in core renderers as needed.
