# ADR-0002 — Keep Baseline and Optimized Query Paths Side by Side

## Decision
Keep the baseline endpoint and expose optimized variants during the learning/benchmark phase.

## Reason
Changing the baseline in place would remove the reproducible before/after comparison required by the simulation lab.

## Consequence
The project temporarily contains intentionally inefficient and optimized paths. This is a lab design choice, not a recommendation for production code.
