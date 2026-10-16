#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mvn -pl arbercharts-demo -am package
JAR="arbercharts-demo/target/$(cat arbercharts-demo/target/demo-jar.txt)"

java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -Ddemo.theme=dark \
  -jar "$JAR" "$@"
