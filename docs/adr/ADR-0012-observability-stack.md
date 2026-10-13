# ADR-0012 — Metrics + Traces + Structured Logs

## Status
Accepted for the simulation lab.

## Context
The earlier phases introduce DB query bottlenecks, cache failures, async saturation, Kafka lag/retries, outbox recovery, and Saga compensation. A single signal cannot distinguish all of these failure modes.

## Decision
- Micrometer + Prometheus for low-cost, queryable metrics.
- Micrometer Tracing / OpenTelemetry OTLP for distributed traces.
- OpenTelemetry Collector as a vendor-neutral ingestion boundary.
- Grafana Tempo as trace backend and Grafana as the UI.
- ECS structured console logs with trace correlation.

## Alternatives
- Zipkin: simpler tracing backend; Tempo aligns with the existing Grafana stack and object/block trace model.
- Jaeger: mature tracing UI/backend; also valid if team standardizes on Jaeger.
- Vendor APM: easier managed correlation and alerting, but cost/vendor coupling.
- Logs-only: insufficient for latency breakdown and cross-service causality.
- Metrics-only: shows aggregate symptoms but usually cannot identify the slow span for one request.

## Trade-offs
- More components and operational configuration than metrics alone.
- Trace sampling and retention are mandatory cost controls.
- Exemplars and trace-to-metrics navigation require compatible instrumentation/configuration and runtime verification.
- High-cardinality labels can overload Prometheus; IDs belong in logs/traces, not metric labels.
