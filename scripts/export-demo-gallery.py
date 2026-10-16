#!/usr/bin/env python3
"""Export the Charts documentation gallery from a running Spring Boot demo."""

import argparse
import hashlib
import json
from pathlib import Path
import struct
from urllib.parse import urlencode
from urllib.request import urlopen


EXAMPLES = {
    "candlestick": (96, "CandlestickRenderer"),
    "heatmap": (1024, "HeatmapRenderer"),
    "donut": (256, "DonutRenderer"),
    "radar": (96, "RadarRenderer"),
    "boxplot": (96, "BoxPlotRenderer"),
    "kde": (256, "KDERenderer"),
}
WIDTH, HEIGHT, SCALE = 1600, 900, 2
ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8081")
    parser.add_argument("--artifact", type=Path, required=True,
                        help="The fresh executable JAR used to start this demo.")
    parser.add_argument("--output", type=Path, default=ROOT / "arbercharts-spring-boot-demo"
                        / "docs/screenshots/renderers")
    args = parser.parse_args()
    artifact_hash = hashlib.sha256(args.artifact.read_bytes()).hexdigest()
    base_url = args.base_url.rstrip("/")
    with urlopen(base_url + "/api/examples", timeout=30) as response:
        catalog = json.load(response)
    available = {item["id"]: item["renderer"] for item in catalog["examples"]}
    images = {}
    records = []
    for example, (samples, renderer) in EXAMPLES.items():
        if available.get(example) != renderer:
            raise RuntimeError(f"The running demo does not expose {example}: {renderer}")
        for theme in ("light", "dark"):
            query = urlencode(dict(width=WIDTH, height=HEIGHT, scale=SCALE,
                                   samples=samples, theme=theme))
            endpoint = f"/api/charts/{example}.png?{query}"
            with urlopen(base_url + endpoint, timeout=60) as response:
                if response.headers.get_content_type() != "image/png":
                    raise RuntimeError(f"Expected image/png for {endpoint}")
                data = response.read()
            if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
                raise RuntimeError(f"Invalid PNG header for {endpoint}")
            if struct.unpack(">II", data[16:24]) != (WIDTH, HEIGHT):
                raise RuntimeError(f"Unexpected PNG dimensions for {endpoint}")
            name = f"{example}-{theme}.png"
            images[name] = data
            records.append(dict(file=name, renderer=renderer, request=endpoint,
                                sha256=hashlib.sha256(data).hexdigest()))
    args.output.mkdir(parents=True, exist_ok=True)
    for name, data in images.items():
        (args.output / name).write_bytes(data)
    manifest = dict(artifact=args.artifact.name, artifactSha256=artifact_hash,
                    springBoot=catalog["springBoot"], java=catalog["java"],
                    runtime=catalog["runtime"], dataSource=catalog["dataSource"],
                    width=WIDTH, height=HEIGHT, scale=SCALE, images=records)
    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Exported {len(images)} verified PNG files to {args.output}")


if __name__ == "__main__":
    main()
