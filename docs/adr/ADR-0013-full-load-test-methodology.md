# ADR-0013: Full load-test methodology

- Status: Accepted
- Context: Independent endpoint tests do not show cross-service bottleneck shifts, queueing, or downstream effects.
- Decision: Use k6 smoke, component, mixed, stress and soak profiles; correlate results with Prometheus, traces, structured logs, PostgreSQL, Kafka and outbox signals. Save raw summaries and environment metadata.
- Consequences: Runs are repeatable and bottleneck attribution is more defensible. Tests consume local resources and must be ramped carefully. Thresholds in k6 are initial lab guardrails, not production SLOs.
- Alternatives considered: one large undifferentiated stress test (poor attribution), manual curl only (not repeatable), average latency only (hides tail latency).
