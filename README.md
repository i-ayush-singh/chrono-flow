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
- `chrono-auth-service`: dedicated API key validation and key lifecycle management service
- `chrono-scheduler-service`: consumes job-created events, stores schedule index in Redis, publishes due execution events
- `chrono-executor-service`: consumes execute events, performs webhook calls, pushes retry/DLQ events
- `chrono-api-gateway`: central entrypoint with API key auth, Redis rate limiting, and service routing

## Run Local Infrastructure

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

## On-demand Web Demo with GitHub Codespaces

Use this when you want a public link only during interviews and shut it down afterwards.

### 1) Start a Codespace

- Open the repository on GitHub.
- Click `Code` -> `Codespaces` -> `Create codespace on main`.
- Wait for container setup to finish.

### 2) Boot full stack inside Codespace

```bash
python3 scripts/codespaces_start.py
```

Then verify:

```bash
curl -s http://localhost:8080/actuator/health
python3 scripts/e2e.py
```

### 3) Create a public resume link

- In Codespaces, open the `Ports` tab.
- For port `8080`, set visibility to `Public`.
- Copy the forwarded URL (example: `https://<name>-8080.app.github.dev`).
- Use this as your demo/deployment link (for example `.../actuator/health`).

### 4) Optional public observability links

- Port `16686` (Jaeger)
- Port `3000` (Grafana)

You can temporarily set these to `Public` during demos, then set back to `Private`.

### 5) Shut down after interview

```bash
python3 scripts/codespaces_stop.py
```

Then stop or delete the Codespace in GitHub UI to avoid usage charges beyond free quota.

Observability UIs:

- Jaeger: `http://localhost:16686`
- Grafana: `http://localhost:3000` (admin/admin)
- Prometheus: `http://localhost:9090`

Provisioned observability assets:

- Grafana datasources: Prometheus + Jaeger (auto-configured)
- Grafana dashboard: `ChronoFlow Overview`
- Prometheus alert rules: gateway 5xx ratio and executor 5xx ratio

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

## Run Auth Service

```bash
mvn -pl chrono-auth-service spring-boot:run
```

Health check:

```bash
curl http://localhost:8084/actuator/health
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

Demo UI (via gateway):

```bash
open http://localhost:8080/demo/index.html
```

This UI can:
- create tenant
- create API key
- create job via gateway auth path
- list jobs via gateway
- jump to Jaeger/Grafana/Prometheus links

## End-to-End Smoke Script

After all services are running locally, execute:

```bash
python3 scripts/e2e.py
```

This script creates a tenant, creates an API key, creates a job through the gateway, and lists jobs through the gateway to generate traces for Jaeger.

## How to Evaluate This Project

Use this quick checklist to evaluate ChronoFlow in 10-15 minutes.

### 1) Start infrastructure

```bash
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml ps
```

Expected evidence:
- Postgres, Redis, Kafka, OTel collector, Jaeger, Grafana, Prometheus are up.

### 2) Start services (separate terminals)

```bash
mvn -pl chrono-job-service spring-boot:run
mvn -pl chrono-auth-service spring-boot:run
mvn -pl chrono-scheduler-service spring-boot:run
mvn -pl chrono-executor-service spring-boot:run
mvn -pl chrono-api-gateway spring-boot:run
```

Expected evidence:
- Health endpoints return 200 for ports 8080-8084.

### 3) Run end-to-end flow

```bash
python3 scripts/e2e.py
```

Expected evidence:
- Tenant created
- API key created
- Job created via gateway
- Job list returned successfully

### 4) Verify observability

- Open Jaeger: `http://localhost:16686`
  - Search for `chrono-api-gateway` traces
- Open Grafana: `http://localhost:3000`
  - Dashboard: `ChronoFlow Overview`
- Open Prometheus: `http://localhost:9090`
  - Confirm scrape targets for app services

Expected evidence:
- Trace spans across gateway and downstream service
- Throughput/5xx panels populated

### 5) Validate reliability behavior

```bash
NAMESPACE=chronoflow python3 chaos/executor_kill_recovery.py
```

Expected evidence:
- Executor pod restarts
- Service recovers
- Retry/DLQ path remains operational

### 6) Validate deployment assets

```bash
kubectl kustomize k8s/base >/dev/null && echo "kustomize-ok"
helm template chronoflow helm/chronoflow >/dev/null && echo "helm-ok"
```

Expected evidence:
- Kubernetes baseline renders
- Helm chart renders cleanly

### Reviewer Evidence Checklist

- [ ] E2E script succeeds
- [ ] Gateway auth/rate-limit path exercised
- [ ] Jaeger trace visible
- [ ] Grafana metrics dashboard visible
- [ ] Chaos recovery script works
- [ ] k8s and Helm manifests render

## Next Phases

- Auth service (tenant/API key/JWT)
- Scheduler service (Redis timer wheel + Kafka publish)
- Executor service (Kafka consumer + webhook delivery + retry/DLQ)
- API Gateway
- Observability + Kubernetes deployment

## Kubernetes Baseline

Base manifests are available under `k8s/base` for:
- namespace, configmap, secret
- job-service, auth-service, scheduler-service, executor-service, api-gateway
- ingress for `chronoflow.local`
- health probes, resource requests/limits, and HPAs
- network policies + non-root/read-only security context hardening

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

`chrono-executor-service` also uses Flyway and persists execution records for idempotency and scheduled retries with backoff.

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

Security defaults in Helm:

- Pod/container runs as non-root (`runAsNonRoot`, explicit UID/GID)
- `allowPrivilegeEscalation: false`
- `readOnlyRootFilesystem: true`
- dropped Linux capabilities (`ALL`)
- namespace ingress hardening via NetworkPolicy templates

## Performance and Chaos Testing

Load scripts:

- `perf/k6/smoke-flow.js` (full API smoke flow under load)
- `perf/k6/gateway-soak.js` (sustained gateway read traffic)

Examples:

```bash
k6 run perf/k6/smoke-flow.js
```

```bash
TENANT_ID=<tenant-id> API_KEY=<keyId:keySecret> k6 run perf/k6/gateway-soak.js
```

Chaos script:

- `chaos/executor_kill_recovery.py` (kills executor pod and validates recovery path)

Benchmark results template:

- `docs/benchmarks/results-template.md`

## Operations Docs

- SLO definitions: `docs/operations/slo.md`
- Incident/ops runbook: `docs/operations/runbook.md`
- Incident review template: `docs/operations/incident-template.md`

## Resume Pack

- Architecture one-pager: `docs/resume-pack/architecture-one-pager.md`
- Resume bullet templates: `docs/resume-pack/resume-bullets.md`
- Demo script (2 minutes): `docs/resume-pack/demo-script-2min.md`

## Release Governance

- Versioning strategy: `docs/release/versioning-strategy.md`
- Changelog template: `docs/release/changelog-template.md`
- Release runbook: `docs/release/release-runbook.md`

## Final Readiness

- Production-readiness closeout checklist: `docs/final-readiness-checklist.md`

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
