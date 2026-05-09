#!/usr/bin/env python3

import os
import pathlib
import subprocess
import sys


ROOT_DIR = pathlib.Path(__file__).resolve().parent.parent
LOG_DIR = ROOT_DIR / ".codespaces-logs"
LOG_DIR.mkdir(parents=True, exist_ok=True)


def run(cmd: list[str]) -> None:
    subprocess.run(cmd, cwd=ROOT_DIR, check=True)


def start_service(module: str, log_name: str) -> None:
    log_path = LOG_DIR / log_name
    with log_path.open("ab") as log_file:
        subprocess.Popen(
            ["mvn", "-pl", module, "spring-boot:run"],
            cwd=ROOT_DIR,
            stdout=log_file,
            stderr=log_file,
            start_new_session=True,
        )


def main() -> int:
    print("== ChronoFlow Codespaces bootstrap ==")
    print(f"Root: {ROOT_DIR}")
    print()

    print("[1/4] Starting infra containers...")
    run(
        [
            "docker",
            "compose",
            "-f",
            "infra/docker/docker-compose.yml",
            "up",
            "-d",
            "postgres",
            "redis",
            "zookeeper",
            "kafka",
            "jaeger",
            "otel-collector",
            "grafana",
            "prometheus",
        ]
    )

    print("[2/4] Waiting for infrastructure readiness...")
    run(["docker", "compose", "-f", "infra/docker/docker-compose.yml", "ps"])

    print("[3/4] Starting Spring services...")
    start_service("chrono-job-service", "job-service.log")
    start_service("chrono-auth-service", "auth-service.log")
    start_service("chrono-scheduler-service", "scheduler-service.log")
    start_service("chrono-executor-service", "executor-service.log")
    start_service("chrono-api-gateway", "api-gateway.log")

    print("[4/4] Startup commands launched.")
    print()
    print("Tail logs with:")
    print("  tail -f .codespaces-logs/api-gateway.log")
    print()
    print("Health checks:")
    print("  curl -s http://localhost:8080/actuator/health")
    print("  curl -s http://localhost:8081/api/v1/health")
    print("  curl -s http://localhost:8082/api/v1/health")
    print("  curl -s http://localhost:8083/api/v1/health")
    print("  curl -s http://localhost:8084/actuator/health")
    print()
    print("Run E2E:")
    print("  python3 scripts/e2e.py")
    return 0


if __name__ == "__main__":
    sys.exit(main())
