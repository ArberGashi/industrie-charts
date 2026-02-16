# ArberCharts Architecture Doctrine

This document is the non-negotiable policy for all code changes in ArberCharts.
Every contribution must conform to these rules. Violations are rejected.

## 1. Layer Isolation

- `domain.*` is pure data. No `java.awt`, `javax.swing`, or renderer/platform types.
- `engine.*` is headless logic only. No UI, no `Graphics2D`, no `Swing`.
- `render.*` draws and formats, but must not depend on `platform.export`.
- `platform.*` is the bridge to Swing/export/integration, and must not depend on `engine.*`.
- The architecture test suite (`ArchitectureDoctrineTest`) is the enforcement gate.

## 2. Headless Standard

- All new features must be testable without a display server.
- Do not introduce dependencies that require `GraphicsEnvironment` in domain/engine.
- Any UI behavior must be isolated to `platform.swing` or `render.*`.

## 3. Allocation Discipline

- Render paths must avoid allocations in paint loops.
- Use preallocated buffers, thread-local caches, and zero-alloc formatting utilities.
- Any allocation in render loops must be justified and documented.

## 4. AI-Assisted Engineering Policy

- AI tools may assist with refactoring, documentation, and boilerplate, but are not a source of authority.
- All AI-assisted changes must be reviewed by a human maintainer before acceptance.
- Core logic changes require test validation and doctrine compliance checks.
- No runtime AI dependencies are embedded into core rendering or domain logic.

## 4. Configuration Discipline (ChartAssets)

- `ChartAssets` is a process-wide configuration registry. Treat it as boot-time only.
- Runtime behavior must flow through `PlotContext`, `ChartTheme`, and renderer contracts.
- Multi-tenant/server environments must not mutate `ChartAssets` per request or per tenant.
- Any new key must be documented with scope and stability expectations.

## 5. Compatibility Window

- The `com.arbergashi.charts.ui` package is removed.
- Do not introduce new `ui.*` references or stubs.
- Use `platform.swing`, `platform.export`, and `render.*` instead.

## 6. Enforcement

- All new code must pass `ArchitectureDoctrineTest`.
- Build failures due to doctrine violations are not waived.
