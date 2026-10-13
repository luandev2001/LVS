# CACHE-002 — Invalidation and Stale Data

## Goal

Prove both the normal invalidation path and the intentionally broken path.

## Normal path

1. Warm the cached inventory page.
2. Reserve one unit with `invalidateCache=true`.
3. Read the cached page again.
4. The new warehouse namespace should cause a miss and reload current PostgreSQL state.

## Failure path

1. Warm the cached inventory page.
2. Reserve one unit with `invalidateCache=false`.
3. Read before TTL expiry.
4. The cached response can still expose the previous `reservedQuantity`.
5. After TTL expiry, the next read reloads PostgreSQL.

## Lesson

TTL limits the maximum stale window but does not provide immediate freshness. Explicit invalidation is needed when the business operation requires a fresh read after mutation.

## Result

[TO BE MEASURED]
