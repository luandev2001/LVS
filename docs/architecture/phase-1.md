# Phase 1 — Inventory Baseline

## Goal
Build the first functionally useful service while deliberately retaining database inefficiencies for later experiments.

## Boundary
`inventory-service` owns product and inventory data.

## Current model
Product 1 --- N Inventory

`Inventory.product` is LAZY intentionally. The list mapper accesses product properties, making N+1 observable.

## API
`GET /api/inventory?warehouseCode=WH-1&page=0&size=50`

## Database
PostgreSQL. Flyway owns schema creation and deterministic local seed data.

## Baseline data
- 10,000 products
- 5 inventory rows per product
- 50,000 inventory rows
- 10 warehouse codes

## Explicitly NOT optimized yet
- no warehouse_code index
- no fetch join
- no projection
- offset pagination
- default count query from Page

## Transaction boundary
The read API uses a read-only transaction. The transaction intentionally remains open while lazy relations are resolved so the N+1 behavior is observable.

## Success criteria
The service is functionally correct and the baseline query behavior can be measured reproducibly.
