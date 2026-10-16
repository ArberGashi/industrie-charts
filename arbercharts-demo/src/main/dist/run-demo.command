#!/usr/bin/env sh
set -eu

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
JAR="$DIR/@DEMO_JAR@"

if [ ! -f "$JAR" ]; then
  echo "Could not find @DEMO_JAR@ next to this script."
  exit 1
fi

exec java --add-modules jdk.incubator.vector -jar "$JAR" "$@"

