#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/arbercharts-demo-2.0.0.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Demo JAR nicht gefunden: $JAR"
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java nicht gefunden. Bitte Java 25 installieren."
  exit 1
fi

JAVA_VERSION_RAW="$(java -version 2>&1 | head -n1)"
JAVA_MAJOR="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2}')"

if [[ -z "${JAVA_MAJOR:-}" || "$JAVA_MAJOR" -lt 25 ]]; then
  echo "Gefunden: $JAVA_VERSION_RAW"
  echo "ArberCharts Demo 2.0.0 benötigt Java 25 oder höher."
  exit 1
fi

exec java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -jar "$JAR"
