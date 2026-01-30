#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$ROOT_DIR/dist/native"
DEST_DIR="$ROOT_DIR/arbercharts-spring-boot-starter/src/main/resources/native"

if [ ! -d "$SRC_DIR" ]; then
  echo "dist/native not found: $SRC_DIR" >&2
  exit 1
fi

mkdir -p "$DEST_DIR"

copied=0

copy_if_exists() {
  local src="$1"
  local target_dir="$2"
  local target_name="$3"
  if [ -f "$src" ]; then
    mkdir -p "$target_dir"
    cp -f "$src" "$target_dir/$target_name"
    echo "Copied $(basename "$src") -> $target_dir/$target_name"
    copied=1
  fi
}

# macOS dylib
copy_if_exists "$SRC_DIR/libarbercharts-core.dylib" "$DEST_DIR/macos/aarch64" "libarbercharts-core.dylib"
copy_if_exists "$SRC_DIR/arbercharts-core.dylib" "$DEST_DIR/macos/aarch64" "libarbercharts-core.dylib"

# Linux so (x86_64)
copy_if_exists "$SRC_DIR/libarbercharts-core.so" "$DEST_DIR/linux/x86_64" "libarbercharts-core.so"

# Windows dll
copy_if_exists "$SRC_DIR/arbercharts-core.dll" "$DEST_DIR/windows/x86_64" "arbercharts-core.dll"

if [ "$copied" -eq 0 ]; then
  echo "No native binaries found in $SRC_DIR" >&2
  exit 1
fi
