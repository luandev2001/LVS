# CACHE-004 — Redis Failure / Fail-Open Behavior

## Goal

Verify that cache infrastructure failure does not make the inventory read endpoint unavailable.

## Procedure

1. Warm the cache.
2. Stop Redis.
3. Call the cached endpoint.
4. Observe cache error metrics and PostgreSQL fallback.
5. Restore Redis and verify cache population resumes.

## Result

[TO BE MEASURED]
