#!/usr/bin/env python3
import importlib.util
import subprocess
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
REF_SCRIPT = ROOT / "tools" / "refactor_platform_move.py"

spec = importlib.util.spec_from_file_location("refactor", REF_SCRIPT)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
MOVE_MAP = mod.MOVE_MAP

SRC_BASE = ROOT / "arbercharts-core" / "src" / "main" / "java"

package_re = re.compile(r"^package\s+[\w\.]+;", re.MULTILINE)


def git_show(path):
    rel = path.relative_to(ROOT).as_posix()
    return subprocess.check_output(["git", "show", f"HEAD:{rel}"])


def materialize():
    for src_fq, tgt_fq in MOVE_MAP.items():
        src_path = SRC_BASE / Path(*src_fq.split(".")).with_suffix(".java")
        tgt_path = SRC_BASE / Path(*tgt_fq.split(".")).with_suffix(".java")
        tgt_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            content = git_show(src_path)
        except subprocess.CalledProcessError:
            # Skip if file did not exist at HEAD
            continue
        text = content.decode("utf-8")
        tgt_pkg = ".".join(tgt_fq.split(".")[:-1])
        text = package_re.sub(f"package {tgt_pkg};", text, count=1)
        tgt_path.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    materialize()
    print("Materialized files to new packages from HEAD.")
