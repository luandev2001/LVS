# ASYNC-004 — CompletableFuture Does Not Make JDBC Non-Blocking

## Question

Does returning `CompletableFuture` mean the database operation itself is non-blocking?

## Answer to verify experimentally

No. In this phase each worker executes blocking JDBC through a normal JPA repository. The servlet thread can be released while the worker runs, but the worker still consumes a Java thread and a DB connection during the operation.

## Evidence to collect

- executor active threads
- Hikari active/pending connections
- PostgreSQL sessions
- request thread names
- latency under increasing concurrency

## Result

[TO BE MEASURED]
