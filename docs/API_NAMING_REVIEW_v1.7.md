# API Naming Review (v1.7.0-LTS)

## Scope
This review focuses on naming consistency and minor API polish items. Because 1.7.0-LTS prioritizes stability, all changes are non-breaking; any incompatible rename is deferred to a future non-LTS line.

## Findings
1. **BaseRenderer multi-color naming**
   - Current: `setMultiColor(boolean)` + `isMultiColor()` (protected)
   - Observation: the setter implies a public configuration while the getter is protected and the name does not include "Enabled".
   - Decision: add `isMultiColorEnabled()` (public) for LTS without breaking existing code; keep protected `isMultiColor()` for internal use.

2. **CircularChartModel labels flag**
   - Current: `setLabelsEnabled(boolean)` with no public getter.
   - Observation: naming implies a paired `isLabelsEnabled()` but it is not present.
   - Decision: add `isLabelsEnabled()` (public) in 1.7.0-LTS as a non-breaking accessor.

3. **ChartTheme bullish/bearish defaults**
   - Current: `getBullishColor()` / `getBearishColor()` provide defaults.
   - Observation: defaults are convenient but make these methods optional in implementations; auditors may prefer explicit overrides.
   - Decision: keep defaults for compatibility; clarify in docs that overrides are recommended for domain-specific themes.

## Conclusion
No breaking API renames are introduced in 1.7.0-LTS. The above items are tracked for potential cleanup in a future non‑LTS line or a minor release if explicit getter additions are requested by users.
