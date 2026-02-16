#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAR="arbercharts-demo/target/arbercharts-demo-1.7.0-LTS.jar"
if [[ ! -f "$JAR" ]]; then
  mvn -pl arbercharts-demo -am package
fi

java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -Ddemo.theme=dark \
  -jar "$JAR"
