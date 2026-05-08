# ChronoFlow Architecture One-Pager

## Problem
ChronoFlow provides cron-based job scheduling as a platform service with multi-tenant isolation, reliable execution, and observability.

## Core Design
- **Gateway**: API key auth, tenant-aware rate limiting, audit stream
- **Job service**: tenant/job APIs + event emission
- **Auth service**: key validation and lifecycle operations (list/revoke/rotate)
- **Scheduler service**: Redis schedule index + due-event publishing
- **Executor service**: webhook delivery, retries, DLQ, idempotency records
- **Infra**: Kafka + Redis + PostgreSQL + OTel + Jaeger + Prometheus + Grafana

## Reliability Patterns
- Execution idempotency via `executionId` and persisted execution records
- Exponential backoff retries with durable retry state
- DLQ publishing for terminal failures
- Flyway migrations and schema validation on startup

## Security and Platform Controls
- Non-root/read-only containers and dropped capabilities
- Namespace network policies with gateway ingress exception
- Kubernetes probes, resource limits, and HPA scaling

## Deployment Model
- Docker Compose for local full-stack runtime
- Kubernetes manifests (`k8s/base`) for baseline deployment
- Helm chart (`helm/chronoflow`) for environment-specific releases
- CI/CD workflows for build/test, image publish, and manual deploy
