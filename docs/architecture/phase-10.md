# Phase 10 — Redis Cache-Aside, Invalidation, Stale Data & Cache Stampede

## Goal

Add Redis to the existing inventory read path and use the lab to demonstrate four production concerns:

1. cache-aside hit/miss
2. TTL and explicit invalidation after a successful DB mutation
3. stale data when invalidation is intentionally skipped
4. cache stampede when a hot key expires, with a Redis single-flight lock as the mitigation

The experiment remains deliberately measurable. No latency/QPS improvement is claimed until the services are actually run under load.

## Architecture

```text
GET /api/inventory/cached
        |
        v
   Redis GET
   /       \
 hit        miss
 |            |
response   protection?
             |
       +-----+------+
       |            |
     NAIVE        LOCKED
       |            |
       |      SET NX + TTL lock
       |            |
       |       one request loads DB
       |       others wait for cache
       |            |
       +-----+------+
             |
        PostgreSQL
             |
        Redis SET TTL
             |
          response

DB mutation
    |
 PostgreSQL commit
    |
 afterCommit()
    +--> delete item cache key
    +--> increment warehouse cache version
```

## Why versioned invalidation?

A warehouse list has many possible page/size keys. Deleting all matching keys with `KEYS` or repeated `SCAN` would turn invalidation into a potentially expensive Redis operation.

The lab instead stores a small namespace version:

`inventory:cache:version:{warehouse}`

A data key contains the current version:

`inventory:cache:data:{warehouse}:v{version}:page={page}:size={size}`

Invalidation increments the version. Old entries may remain in Redis until TTL, but they are unreachable by new readers. This is lazy invalidation rather than physical deletion of every list key.

## Important transaction detail

Invalidation is registered with `TransactionSynchronizationManager.afterCommit()` rather than executed before commit. Otherwise a transaction could invalidate the cache, load an old value concurrently, then roll back/commit in an order that leaves the newly cached value inconsistent with the database.

## Endpoints

### Baseline

- `GET /api/inventory?warehouseCode=WH-1&page=0&size=50`
- `GET /api/inventory/optimized?warehouseCode=WH-1&page=0&size=50`

### Cache-aside

- `GET /api/inventory/cached?warehouseCode=WH-1&page=0&size=50&protection=NAIVE`
- `GET /api/inventory/cached?warehouseCode=WH-1&page=0&size=50&protection=LOCKED`
- `GET /api/inventory/cached/{inventoryId}`

### Mutation + invalidation experiment

`POST /api/inventory/{inventoryId}/reserve/atomic?amount=1&invalidateCache=true`

The default is `true`.

To intentionally reproduce stale data:

`POST /api/inventory/{inventoryId}/reserve/atomic?amount=1&invalidateCache=false`

The DB changes, but the cached representation is intentionally left untouched until TTL expiry.

Explicit experiment invalidation:

`POST /api/inventory/cache/invalidate?warehouseCode=WH-1`

## Configuration

```yaml
cache:
  inventory:
    enabled: true
    invalidation-enabled: true
    ttl-seconds: 30
    lock-ttl-ms: 5000
    lock-wait-ms: 2000
    simulated-db-delay-ms: 0
```

Environment variables can override these values. For stampede experiments, a controlled DB delay such as `INVENTORY_CACHE_SIMULATED_DB_DELAY_MS=1000` makes the amplification visible. It is an experiment knob, not a production recommendation.

## Failure scenarios

### Stale data

1. warm `/api/inventory/cached`.
2. reserve stock with `invalidateCache=false`.
3. call the cached endpoint again before TTL expiry.
4. observe the old `reservedQuantity`.
5. wait for TTL or explicitly invalidate.
6. observe the new value.

This demonstrates why TTL alone is not sufficient when freshness matters.

### Cache stampede

A hot key expires and many callers request the same key at approximately the same time.

`NAIVE` mode lets every caller execute the PostgreSQL query on a miss.

`LOCKED` mode uses Redis `SET NX PX` semantics. One caller becomes the loader. Other callers wait for the cache value. The lock has a TTL so a crashed loader does not hold it forever. Lock release uses a token-checked Lua script so one request cannot delete another request's lock after lock expiry/reacquisition.

## Redis failure behavior

The read/write cache operations are intentionally fail-open: Redis errors increment metrics and the request falls back to PostgreSQL. This protects correctness/availability at the cost of losing cache acceleration. The stampede lock path also falls back rather than making Redis availability a hard dependency for the API.

## Metrics

Examples exposed through Actuator:

- `inventory.cache.hit`
- `inventory.cache.miss`
- `inventory.cache.write`
- `inventory.cache.error`
- `inventory.cache.invalidation`
- `inventory.cache.invalidation.skipped`
- `inventory.cache.stampede.lock.acquired`
- `inventory.cache.stampede.lock.contended`
- `inventory.cache.stampede.wait.success`
- `inventory.cache.stampede.wait.timeout`
- `inventory.item-cache.hit`
- `inventory.item-cache.miss`

For load testing also inspect PostgreSQL active connections, Hikari pool utilization, request latency, Redis command rate and Redis memory.

## Alternatives considered

| Approach | Why not the primary Phase 10 approach |
|---|---|
| Caffeine/local cache | Excellent single-instance latency, but entries are local to each service instance and invalidation across instances is harder |
| Spring Cache abstraction | Useful abstraction, but the lab needs explicit Redis keys, versioned invalidation and stampede mechanics to make the failure modes visible |
| Redis cache-aside | Chosen because the lab can demonstrate distributed shared cache, TTL, explicit invalidation and distributed locking |
| Write-through | Not chosen because this phase is focused on read acceleration and explicit consistency behavior |

## Measurements

Use the same dataset and workload before/after caching. Record:

- p50/p95/p99 latency
- request throughput
- PostgreSQL query count/QPS
- Hikari active/pending/idle connections
- Redis hit/miss ratio
- Redis memory
- cache rebuild count during expiry
- lock contention and wait timeout count

Use `[TO BE MEASURED]` until actual runs are performed.
