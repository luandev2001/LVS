#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8084}"

printf '\n1) Outbox stats before\n'
curl -s "$BASE_URL/api/outbox/stats"; echo

printf '\n2) Create payment\n'
curl -s -X POST \
  "$BASE_URL/api/payments?orderReference=OUTBOX-FAILURE-1&amount=100&currency=VND&mode=SUCCESS" \
  -H 'Idempotency-Key: outbox-failure-1'; echo

printf '\n3) Outbox stats after\n'
curl -s "$BASE_URL/api/outbox/stats"; echo

printf '\n4) Recover stale processing rows manually\n'
curl -s -X POST "$BASE_URL/api/outbox/recover-stale"; echo
