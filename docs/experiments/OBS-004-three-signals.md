# OBS-004 — Correlating Metrics, Traces, and Logs

## Hypothesis
Combining RED metrics (Rate, Errors, Duration), saturation metrics, distributed traces, and structured logs reduces diagnosis time for a deliberately slow/failing request.

## Procedure
1. Start stack and services.
2. Confirm all Prometheus targets are UP.
3. Call inventory list and cached endpoints, then create a payment that traverses Kafka.
4. Open Grafana and identify request-rate/error/latency changes.
5. Open Tempo and find the trace by service/name/time.
6. Search service logs using the trace ID and correlate the slow span to DB/Kafka/cache behavior.
7. Repeat with `INVENTORY_ASYNC_SIMULATED_DB_DELAY_MS=100` and/or `INVENTORY_CACHE_SIMULATED_DB_DELAY_MS=100`.

## Record
- Request rate: [TO BE MEASURED]
- Error rate: [TO BE MEASURED]
- p50/p95/p99: [TO BE MEASURED]
- Trace completeness across Kafka: [TO BE VERIFIED]
- Diagnosis time: [TO BE MEASURED]

## Acceptance criteria
- Prometheus scrapes each configured target.
- HTTP request histograms are available after traffic.
- At least one inventory trace appears in Tempo.
- Trace/span identifiers can be correlated with structured logs.
- Kafka cross-service propagation is explicitly verified, not inferred from local spans.
