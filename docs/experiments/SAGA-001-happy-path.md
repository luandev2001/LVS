# SAGA-001 — Happy Path

Create a Payment with `inventoryId=1`, `quantity=1`, gateway `SUCCESS`, `sagaFailureMode=NONE`.
Expected: PAYMENT_SUCCEEDED → ACCOUNTING_POSTED → INVENTORY_RESERVED → Payment `COMPLETED`.

Verify: one settlement journal, one reservation, balanced journal, all outbox rows eventually `PUBLISHED`. Metrics `[TO BE MEASURED]`.
