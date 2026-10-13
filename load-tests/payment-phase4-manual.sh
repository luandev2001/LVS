#!/usr/bin/env bash
set -euo pipefail
BASE=${BASE_URL:-http://localhost:8084}
KEY="manual-$(date +%s)"

echo '--- SUCCESS ---'
curl -sS -X POST "$BASE/api/payments?orderReference=ORDER-SUCCESS&amount=100&currency=VND&mode=SUCCESS" -H "Idempotency-Key: $KEY"
echo

echo '--- TIMEOUT ---'
curl -sS -X POST "$BASE/api/payments?orderReference=ORDER-TIMEOUT&amount=100&currency=VND&mode=TIMEOUT" -H "Idempotency-Key: timeout-$KEY"
echo

echo 'Wait at least 3 seconds, then inspect the payment status.'
