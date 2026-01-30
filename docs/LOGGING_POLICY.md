# Logging Policy (Core vs. Bridges)

## Purpose
ArberCharts Core is optimized for deterministic, low-latency rendering. Logging within the core is intentionally minimal to avoid runtime overhead, allocation spikes, and timing variability.

## Policy
- **Core module:** logging-light by design. Only essential errors may be surfaced via exceptions.
- **Bridge modules and applications:** responsible for operational logging (SLF4J/Logback or platform-native logging).
- **Performance-sensitive paths:** avoid logging inside render loops, model hot paths, and allocation-sensitive sections.

## Rationale
In regulated environments, determinism and zero-allocation guarantees outweigh verbose logging in the core. Observability is provided at the bridge/application layer where it can be controlled and rate-limited.

## Guidance for Integrators
- Enable logging at the application boundary where data ingress, rendering requests, and output delivery are orchestrated.
- Use structured logging and sampling to avoid frame-time spikes.
