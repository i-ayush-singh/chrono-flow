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

## Next Phases

- Auth service (tenant/API key/JWT)
- Scheduler service (Redis timer wheel + Kafka publish)
- Executor service (Kafka consumer + webhook delivery + retry/DLQ)
- API Gateway
- Observability + Kubernetes deployment
