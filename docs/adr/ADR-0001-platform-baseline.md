# ADR-0001 — Platform Baseline

## Status
Accepted for Phase 0

## Context

The lab needs a modern Spring Boot baseline while preserving reproducible experiments and avoiding unrelated runtime upgrades during performance comparisons.

## Decision

Use:

- Spring Boot 4.1.1
- Java 21
- Maven multi-module
- Spring MVC/Tomcat
- Spring Boot Actuator
- PostgreSQL as the relational database

## Consequences

Positive:

- Current stable Spring Boot generation.
- Java 21 remains a practical enterprise LTS baseline.
- Service boundaries are explicit.
- Later benchmarks can change one major variable at a time.

Negative:

- Java 25 is the current LTS and will need a separate compatibility/upgrade experiment.
- Spring Boot 4 introduces newer starter naming; existing projects may require migration work.
