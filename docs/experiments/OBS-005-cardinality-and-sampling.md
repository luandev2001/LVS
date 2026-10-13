# OBS-005 — Cardinality and Trace Sampling

## Goal
Compare full tracing with a lower sampling rate and demonstrate why identifiers must not become metric labels.

## Procedure
1. Run a fixed k6 workload at `TRACING_SAMPLE_PROBABILITY=1.0`. Record request latency, collector throughput, trace count, and memory.
2. Restart with `TRACING_SAMPLE_PROBABILITY=0.1` and repeat identical workload.
3. Compare trace coverage and telemetry overhead.
4. Review custom metric tags. Never add paymentId, userId, orderReference, raw exception message, or unbounded URI as labels.

## Results
All numerical results: [TO BE MEASURED].

## Interpretation
Sampling reduces trace volume but can hide rare failures. Tail-based sampling can preserve traces with errors/high latency, but requires collector-side policy and more memory/state; it is a follow-up experiment, not claimed as implemented here.
