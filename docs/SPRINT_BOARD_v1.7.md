# Documentation & Verification Sprint Board (v1.7.0-LTS)

## Track A — Package Documentation
- `arbercharts-core/src/main/java/com/arbergashi/charts/api/package-info.java` — API purpose + thread-safety note
- `arbercharts-core/src/main/java/com/arbergashi/charts/model/package-info.java` — model semantics + concurrency note
- `arbercharts-core/src/main/java/com/arbergashi/charts/render/package-info.java` — renderer contracts + zero‑alloc rules
- `arbercharts-core/src/main/java/com/arbergashi/charts/engine/package-info.java` — engine concurrency semantics
- `arbercharts-core/src/main/java/com/arbergashi/charts/core/rendering/package-info.java` — ArberCanvas contract
- `arbercharts-core/src/main/java/com/arbergashi/charts/util/package-info.java` — helper expectations
- `arbercharts-core/src/main/java/com/arbergashi/charts/domain/package-info.java` — domain types
- `arbercharts-core/src/main/java/com/arbergashi/charts/tools/package-info.java` — tooling use only
- `arbercharts-core/src/main/java/com/arbergashi/charts/platform/package-info.java` — platform hooks
- `arbercharts-core/src/main/java/com/arbergashi/charts/internal/package-info.java` — internal warning

## Track B — Concurrency Documentation
- `docs/CONCURRENCY_MODEL.md`
  - Happens‑before chain (VarHandle acquire/release)
  - Reader/Writer interleaving
  - Known limitations (Long wrap, high contention)
  - Formal verification hints (jqwik + stress patterns)

## Track C — Verification Strategy
- `docs/VERIFICATION_STRATEGY.md`
  - Compliance matrix
  - Property tests (jqwik)
  - Visual regression checks
  - Zero‑allocation enforcement (Guidelines Checker)

## Track D — Logging Policy
- `docs/LOGGING_POLICY.md`
  - Core logging‑light rationale
  - Bridge/app logging responsibilities

## Track E — Executive Summary
- `docs/AUDIT_RESPONSE_v1.7.md`
- `docs/SUMMARY_ONEPAGER_v1.7.md`

## Track F — API Naming Review
- `docs/API_NAMING_REVIEW_v1.7.md`
