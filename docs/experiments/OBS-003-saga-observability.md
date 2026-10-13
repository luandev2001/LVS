# OBS-003 — Saga Observability

Observe the same Saga across Payment, Accounting and Inventory using trace/correlation fields, service logs and metrics.

Measure:
- end-to-end Saga completion latency `[TO BE MEASURED]`
- state dwell time `[TO BE MEASURED]`
- each local outbox backlog `[TO BE MEASURED]`
- Kafka consumer lag `[TO BE MEASURED]`
- compensation count `[TO BE MEASURED]`
- duplicate event count `[TO BE MEASURED]`

A useful production investigation path is: Payment saga status → Payment outbox → Kafka partition/lag → Accounting outbox/journal → Inventory processed event/reservation → compensation events.
