# ChronoFlow

ChronoFlow is a distributed job scheduler platform (Cron-as-a-Service) designed with industry-ready architecture.

## Tech Stack

- Spring Boot 3 + Java 21
- PostgreSQL (source of truth)
- Redis (locks, timer wheels, idempotency)
- Kafka (execution/event backbone)
- Docker Compose (local runtime)
- Kubernetes (next phases)

## Modules (Phase 1)

- `chrono-bom`: centralized dependency versions
- `chrono-common`: shared DTOs and base contracts
- `chrono-job-service`: first Spring Boot service (health + base runtime)
- `chrono-scheduler-service`: consumes job-created events, stores schedule index in Redis, publishes due execution events
- `chrono-executor-service`: consumes execute events, performs webhook calls, pushes retry/DLQ events
- `chrono-api-gateway`: central entrypoint with API key auth, Redis rate limiting, and service routing

## Run Local Infrastructure

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

Observability UIs:

- Jaeger: `http://localhost:16686`
- Grafana: `http://localhost:3000` (admin/admin)

## Build

```bash
mvn clean install
```

## Run Job Service

```bash
mvn -pl chrono-job-service spring-boot:run
```

Health check:

```bash
curl http://localhost:8081/api/v1/health
```

## Run Scheduler Service

```bash
mvn -pl chrono-scheduler-service spring-boot:run
```

Health check:

```bash
curl http://localhost:8082/api/v1/health
```

## Run Executor Service

```bash
mvn -pl chrono-executor-service spring-boot:run
```

Health check:

```bash
curl http://localhost:8083/api/v1/health
```

## Run API Gateway

```bash
mvn -pl chrono-api-gateway spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

## End-to-End Smoke Script

After all services are running locally, execute:

```bash
./scripts/e2e.sh
```

This script creates a tenant, creates an API key, creates a job through the gateway, and lists jobs through the gateway to generate traces for Jaeger.

## Next Phases

- Auth service (tenant/API key/JWT)
- Scheduler service (Redis timer wheel + Kafka publish)
- Executor service (Kafka consumer + webhook delivery + retry/DLQ)
- API Gateway
- Observability + Kubernetes deployment

## Kubernetes Baseline

Base manifests are available under `k8s/base` for:
- namespace, configmap, secret
- job-service, scheduler-service, executor-service, api-gateway
- ingress for `chronoflow.local`
- health probes, resource requests/limits, and HPAs

Apply with:

```bash
kubectl apply -k k8s/base
```

Notes:
- Update container image names/tags before applying in your cluster.
- These manifests assume external Kafka/Redis/Postgres services are reachable in-cluster as `kafka`, `redis`, and `postgres`.

## Database Migrations

`chrono-job-service` now uses Flyway migrations (instead of Hibernate schema auto-update).

- Migration location: `chrono-job-service/src/main/resources/db/migration`
- Baseline migration: `V1__init_chronoflow_schema.sql`
- Hibernate mode: `ddl-auto: validate`

Typical local run:

```bash
mvn -pl chrono-job-service spring-boot:run
```

Flyway runs automatically on startup and validates schema history.

## Helm Chart

A Helm chart is available at `helm/chronoflow`.

Render manifests:

```bash
helm template chronoflow helm/chronoflow
```

Install in namespace:

```bash
helm upgrade --install chronoflow helm/chronoflow --namespace chronoflow --create-namespace
```

## CI/CD

GitHub Actions workflows are included:

- `.github/workflows/ci.yml`
  - Runs on PRs and pushes to `main`
  - Executes `mvn clean verify`
  - Validates k8s manifests with `kubectl kustomize`

- `.github/workflows/release-deploy.yml`
  - On push to `main`: builds and pushes service images to GHCR with Jib
  - On manual dispatch: deploys Helm chart to Kubernetes

Required repo configuration:

- GitHub Packages permissions enabled for workflow token (`packages: write`)
- Repository secret: `KUBE_CONFIG_DATA` (base64 encoded kubeconfig) for deploy job

Recommended branch protection for `main`:

- Require pull request reviews
- Require status checks to pass (`CI / Build and Test`)
- Restrict direct pushes
