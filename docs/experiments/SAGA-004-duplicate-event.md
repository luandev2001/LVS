# SAGA-004 — Duplicate Event

Redeliver the same `ACCOUNTING_POSTED` event. Inventory's durable `processed_saga_events.event_id` boundary must prevent a second reservation.

Record duplicate deliveries and final reserved quantity.
