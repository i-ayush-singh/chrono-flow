# Resume Bullets (ChronoFlow)

Use these as starting points and replace placeholders with your measured values from `docs/benchmarks/results-template.md`.

- Built **ChronoFlow**, a distributed scheduler platform using Spring Boot microservices, Kafka, Redis, and PostgreSQL, with Kubernetes + Helm deployment and GitHub Actions CI/CD.
- Implemented tenant-aware API gateway controls (DB-backed API key auth, rate limiting, request audit stream), reducing unauthorized traffic exposure and improving multi-tenant isolation.
- Designed durable execution reliability path with idempotent execution IDs, persisted retry state, exponential backoff, and DLQ handling for failure recovery.
- Added production-grade observability stack (OpenTelemetry, Jaeger, Prometheus, Grafana dashboards, and alert rules) and established SLO-driven runbooks for on-call response.
- Hardened runtime security using non-root/read-only containers, dropped Linux capabilities, and namespace network policies.
- Validated system behavior with integration tests (Testcontainers), load tests, and chaos scripts; achieved **<p95_latency_ms_placeholder> ms p95** at **<rps_placeholder> RPS** with **<error_rate_placeholder>%** error rate.

## Short Project Summary (for resume/project section)
ChronoFlow is a multi-tenant job scheduling platform that accepts cron jobs, dispatches executions through Kafka, performs resilient webhook delivery with persisted retries/DLQ, and provides end-to-end observability and production-ready Kubernetes deployment controls.
