#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="${1:-$ROOT/reconstructed}"
mkdir -p "$OUT"
for base in COUCHY_C1_C12_FULL PROJECTIVY_G1_G12_FULL PROCESS_FULL; do
  cat "$ROOT/archive/${base}.md.gz.part"* > "$OUT/${base}.md.gz"
  gzip -dc "$OUT/${base}.md.gz" > "$OUT/${base}.md"
done
echo "Reconstructed archives in: $OUT"
sha256sum "$OUT"/*.md
