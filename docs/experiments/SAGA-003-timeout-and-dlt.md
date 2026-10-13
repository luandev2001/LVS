# SAGA-003 — Accounting Failure / Timeout

Use `sagaFailureMode=ACCOUNTING_FAIL`. Accounting throws, Kafka retry/DLT becomes visible, and Payment remains waiting until the Saga timeout scanner marks `TIMED_OUT`.

Record retry count, DLT, timeout latency and outbox backlog. Values `[TO BE MEASURED]`.
