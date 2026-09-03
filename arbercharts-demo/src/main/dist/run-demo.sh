#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
JAR="$(ls "$DIR"/arbercharts-demo-*.jar 2>/dev/null | head -n 1 || true)"

if [ -z "${JAR:-}" ]; then
  echo "Could not find arbercharts-demo-*.jar next to this script."
  exit 1
fi

exec java --add-modules jdk.incubator.vector -jar "$JAR" "$@"

