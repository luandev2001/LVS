# CONC-002 — Deadlock Experiment Plan

Create a separate test fixture before enabling this experiment in a shared environment.

Transaction A locks inventory row 1 then row 2.
Transaction B locks inventory row 2 then row 1.

Expected outcome: PostgreSQL can detect the cycle and abort one transaction.

Do not treat deadlock reproduction as an application correctness failure by itself; the experiment is to understand lock ordering, transaction scope, retry policy, and observability.

Evidence: PostgreSQL deadlock log/diagnostic, transaction duration, failed transaction count, retry outcome.
