# CI Setup (GitHub Actions)

## Overview
The project uses GitHub Actions for continuous integration. Workflows live in `.github/workflows/`.

## Workflows
- `ci-core.yml`: Core + Swing + Server tests on macOS/Windows/Linux.
- `ci-compose.yml`: Compose bridge build on macOS/Linux.
- `ci-qt-swift.yml`: Qt + Swift build on macOS (requires native artifacts; Swift is macOS only).

## Required Secrets (Qt/Swift)
Qt/Swift build needs native headers + dylib:
- `ARBER_NATIVE_PATH` — absolute path to a folder containing:
  - `arbercharts-core.h`
  - `libarbercharts-core.dylib`

If the repository includes `dist/native`, the workflow will auto‑use it and no secret is required.

## Notes
- Workflows run on push and pull_request.
- CI does not publish artifacts; it only validates builds/tests.
- If Qt/Swift native artifacts are not available, the Qt/Swift job will skip with a notice.
- Swift bridge is macOS arm64 only in v1.7.0-LTS (no iOS/iPadOS builds).
