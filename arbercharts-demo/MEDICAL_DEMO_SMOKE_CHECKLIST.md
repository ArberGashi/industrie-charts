# Medical Demo Smoke Checklist (First-Class Delivery)

## Scope
- Module: `arbercharts-demo`
- Theme modes: `Modern Dark` and `Modern Light`
- Target renderers:
  - `CalibrationRenderer`
  - `CapnographyRenderer`
  - `ECGRenderer`
  - `ECGRhythmRenderer`
  - `EEGRenderer`
  - `EMGRenderer`
  - `EOGRenderer`
  - `IBPRenderer`
  - `MedicalSweepRenderer`
  - `NIRSRenderer`
  - `PPGRenderer`
  - `SpectrogramMedicalRenderer`
  - `SpirometryRenderer`
  - `SweepEraseEKGRenderer`
  - `UltrasoundMModeRenderer`
  - `VCGRenderer`
  - `VentilatorWaveformRenderer`

## Visual Quality Gates
- No frozen waveform: all medical renderers must show continuous motion.
- No clipping artifacts at sweep edge (especially `SweepEraseEKGRenderer` and `MedicalSweepRenderer`).
- Color contrast must remain readable in both Dark and Light.
- No brown/warm drift in UI shell; renderer colors must match clinical profile presets.
- Text and axis labels remain readable at default window size (1100x680) and compact size (800x500).

## Renderer-Specific Checks
- ECG family (`ECGRenderer`, `ECGRhythmRenderer`, `SweepEraseEKGRenderer`, `VCGRenderer`):
  - Clear P-QRS-T morphology visible.
  - Baseline wander subtle, not dominant.
- Neuro (`EEGRenderer`, `NIRSRenderer`, `EOGRenderer`, `SpectrogramMedicalRenderer`):
  - Smooth low-amplitude traces, no harsh jitter.
  - Multi-channel separation readable.
- Muscle (`EMGRenderer`):
  - Burst behavior visible with high-frequency texture.
- Respiration (`VentilatorWaveformRenderer`, `SpirometryRenderer`, `CapnographyRenderer`):
  - Breath cycles coherent and rhythmically stable.
  - Capnography plateau and downstroke clearly visible.
- Perfusion (`PPGRenderer`, `IBPRenderer`):
  - Pulse peaks clean and periodic.
- Utility (`CalibrationRenderer`, `UltrasoundMModeRenderer`):
  - Calibration pulse is visually crisp.
  - Ultrasound mode has clear contrast gradient.

## Axis and Legend Checks
- Axis ranges are renderer-specific and stable during animation.
- Legends:
  - Hidden for dense clinical traces.
  - Enabled only where multi-channel interpretation benefits (`VentilatorWaveformRenderer`, `SpirometryRenderer`).

## Pass Criteria
- All listed renderers pass visual quality gates in Dark and Light.
- No rendering exceptions in console while cycling all medical renderers.
- No overlap/regression in titlebar controls and demo layout.
