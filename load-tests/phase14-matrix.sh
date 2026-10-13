#!/usr/bin/env bash
set -euo pipefail

# Run from repository root after services and observability stack are healthy.
mkdir -p results/phase14
STAMP="$(date +%Y%m%d-%H%M%S)"
run() {
  local name="$1"; shift
  echo "== Phase 14: ${name} =="
  k6 run --summary-export "results/phase14/${STAMP}-${name}.json" "$@"
}

run smoke load-tests/phase14-smoke.js
MODE=INVENTORY TARGET_VUS=10 STEADY=60s run inventory-baseline load-tests/full-system-phase14.js
MODE=INVENTORY TARGET_VUS=25 STEADY=2m run inventory-load load-tests/full-system-phase14.js
MODE=MIXED TARGET_VUS=25 STEADY=3m run mixed-load load-tests/full-system-phase14.js
MODE=PAYMENT TARGET_VUS=10 STEADY=2m run payment-load load-tests/full-system-phase14.js
MODE=MIXED TARGET_VUS=50 STEADY=2m run stress-load load-tests/full-system-phase14.js
