# Phase 13 — End-to-end Observability

## Goal
Connect metrics, traces, and structured logs across the payment → Kafka → accounting/inventory flow, and make the Phase 1–12 failure modes diagnosable.

## Implemented
- Prometheus scrapes inventory (8081), payment (8084), and accounting (8085) actuator endpoints.
- HTTP histograms enabled for server/client requests and Kafka listener observations.
- Micrometer tracing bridge + OTLP exporter configuration on inventory, payment, and accounting.
- Kafka producer/listener observation enabled to allow trace context propagation through supported Spring Kafka instrumentation.
- OpenTelemetry Collector receives OTLP over gRPC/HTTP and exports traces to Tempo.
- Grafana provisions Prometheus and Tempo datasources; dashboard includes request latency, JVM, Hikari, cache, async, Kafka, and outbox panels where metrics are available.
- ECS structured console logging on the main business services; trace/span correlation should be verified in actual runtime logs.
- Representative inventory API and existing saga/accounting operations have observation annotations.

## Local URLs
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (default local Grafana credentials depend on image defaults; change for shared environments)
- Tempo query API: http://localhost:3200
- Inventory metrics: http://localhost:8081/actuator/prometheus
- Payment metrics: http://localhost:8084/actuator/prometheus
- Accounting metrics: http://localhost:8085/actuator/prometheus

## Run
1. Start dependencies with `docker compose up -d`.
2. Run services from the host using the ports above.
3. Generate traffic with `k6 run load-tests/observability-phase13.js`.
4. Inspect Prometheus targets, Grafana dashboard, and Tempo traces.

## Diagnose by signal
- Metrics tell you **what/when**: latency, errors, saturation, cache hit/miss, pool usage, Kafka listener rates.
- Traces tell you **where**: HTTP → service method → Kafka publish/consume boundaries where instrumentation supports propagation.
- Structured logs tell you **why**: business IDs, error context, and trace/span correlation. Do not put raw PII, credentials, idempotency keys, or full payment payloads into logs.

## Important limits
- Trace sampling defaults to 1.0 for a lab only; production should use a cost-aware sampling policy.
- Prometheus histogram buckets and labels must be controlled; never add user IDs, payment IDs, order references, or raw URLs as metric tags.
- Tempo local storage is for the lab and has 24-hour retention, not production durability.
- Kafka trace propagation depends on producer/listener observation instrumentation and runtime-compatible Spring Boot/Spring Kafka versions; verify a trace across the actual broker rather than assuming every custom listener carries context.
- Metrics names vary by Micrometer/Spring Boot version; use `/actuator/metrics` and Prometheus series discovery to confirm exact names before editing dashboard queries.
- Benchmarks remain `[TO BE MEASURED]`.
