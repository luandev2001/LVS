# SAGA-002 — Inventory Failure and Compensation

Use `sagaFailureMode=INVENTORY_FAIL`. Accounting posts settlement. Inventory deliberately fails and emits `INVENTORY_RESERVATION_FAILED`. Accounting posts a reversal. Payment becomes `REFUNDED` and `COMPENSATED`.

Verify reserved quantity did not increase and settlement + reversal net to zero.
