# Phase 3 — Concurrency

Phase 3 introduces reproducible inventory reservation strategies:

- no locking
- optimistic locking
- pessimistic locking
- atomic SQL update

The goal is to observe correctness and contention rather than declare one universal winner. The appropriate strategy depends on contention, transaction scope, failure handling, and database workload.
