# ADR-0011: Kafka concurrency and explicit resilience experiments

- Status: Accepted for the simulation lab
- Decision: Make Kafka listener concurrency configurable and provide an explicit dependency simulator for bounded retry, circuit breaker, and semaphore bulkhead.
- Why: These mechanisms can be observed independently and correlated with consumer lag, DB pool saturation, and HTTP rejection rates.
- Alternatives: Keep concurrency fixed; rely only on Kafka retry topics; add Resilience4j immediately. Fixed concurrency hides the tuning trade-off. Retry topics do not protect synchronous downstream calls. Resilience4j is the production-oriented library choice, but a minimal explicit implementation is useful for learning internals.
- Consequences: The custom breaker is educational and must not be treated as a hardened production library. Follow up by testing Resilience4j semantics, distributed tracing, timeout propagation, and integration tests.
