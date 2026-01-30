#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CORE_JAR="${ROOT_DIR}/arbercharts-core/target/arbercharts-core-1.7.0-LTS.jar"
CONFIG_DIR="${ROOT_DIR}/arbercharts-core/src/main/resources/META-INF/native-image/com.arbergashi/charts-core"
OUT_DIR="${ROOT_DIR}/dist/native"
GRAAL_BIN="${GRAALVM_HOME:-}/bin/native-image"

mkdir -p "${OUT_DIR}"

if [[ ! -f "${CORE_JAR}" ]]; then
  echo "Core JAR not found: ${CORE_JAR}"
  echo "Build it first: mvn -pl arbercharts-core -am -DskipTests package"
  exit 1
fi

if command -v native-image >/dev/null 2>&1; then
  NATIVE_IMAGE="native-image"
elif [[ -n "${GRAALVM_HOME:-}" && -x "${GRAAL_BIN}" ]]; then
  NATIVE_IMAGE="${GRAAL_BIN}"
else
  echo "native-image not found on PATH."
  echo "Install GraalVM 24.1.0 and ensure native-image is available."
  echo "If GraalVM is installed, set GRAALVM_HOME and retry."
  exit 1
fi

"${NATIVE_IMAGE}" \
  --shared \
  --no-fallback \
  -H:Name=arbercharts-core \
  -H:ConfigurationFileDirectories="${CONFIG_DIR}" \
  -H:Path="${OUT_DIR}" \
  -cp "${CORE_JAR}"

echo "Native artifacts written to: ${OUT_DIR}"
