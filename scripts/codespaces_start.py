#!/usr/bin/env python3

import pathlib
import subprocess
import sys
import time
import urllib.error
import urllib.request


ROOT_DIR = pathlib.Path(__file__).resolve().parent.parent
LOG_DIR = ROOT_DIR / ".codespaces-logs"
LOG_DIR.mkdir(parents=True, exist_ok=True)
COMPOSE_FILE = "infra/docker/docker-compose.yml"


def run(cmd: list[str], check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=ROOT_DIR, check=check, text=True, capture_output=False)


def run_allow_fail(cmd: list[str]) -> None:
    run(cmd, check=False)


def stop_existing_java_processes() -> None:
    run_allow_fail(["pkill", "-f", "spring-boot:run"])
    run_allow_fail(["pkill", "-f", "Chrono.*Application"])


def start_service(module: str, log_name: str, run_args: str | None = None) -> None:
    log_path = LOG_DIR / log_name
    cmd = ["mvn", "-pl", module, "spring-boot:run"]
    if run_args:
        cmd.append(f"-Dspring-boot.run.arguments={run_args}")
    with log_path.open("ab") as log_file:
        subprocess.Popen(
            cmd,
            cwd=ROOT_DIR,
            stdout=log_file,
            stderr=log_file,
            start_new_session=True,
        )


def wait_for_container_health(service: str, timeout_seconds: int = 120) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        result = subprocess.run(
            ["docker", "compose", "-f", COMPOSE_FILE, "ps", service],
            cwd=ROOT_DIR,
            capture_output=True,
            text=True,
            check=False,
        )
        output = result.stdout
        if "(healthy)" in output or ("Up" in output and "health:" not in output):
            return
        time.sleep(3)
    raise RuntimeError(f"Timed out waiting for service health: {service}")


def wait_for_http_ok(url: str, timeout_seconds: int = 150) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=3) as response:
                if 200 <= response.status < 300:
                    return
        except (urllib.error.URLError, TimeoutError):
            pass
        time.sleep(3)
    raise RuntimeError(f"Timed out waiting for HTTP health endpoint: {url}")


def main() -> int:
    print("== ChronoFlow Codespaces bootstrap ==")
    print(f"Root: {ROOT_DIR}")
    print()

    print("[1/6] Stopping stale Spring Boot processes...")
    stop_existing_java_processes()

    print("[2/6] Starting infra containers...")
    run(
        [
            "docker",
            "compose",
            "-f",
            COMPOSE_FILE,
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

    print("[3/6] Waiting for infrastructure readiness...")
    wait_for_container_health("postgres")
    wait_for_container_health("redis")
    wait_for_container_health("kafka")
    run(["docker", "compose", "-f", COMPOSE_FILE, "ps"])

    print("[4/6] Installing shared Maven modules...")
    run(["mvn", "-DskipTests", "install", "-pl", "chrono-common,chrono-bom", "-am"])

    print("[5/6] Starting Spring services...")
    start_service("chrono-job-service", "job-service.log")
    start_service("chrono-auth-service", "auth-service.log")
    start_service("chrono-scheduler-service", "scheduler-service.log")
    start_service("chrono-executor-service", "executor-service.log")
    start_service("chrono-api-gateway", "api-gateway.log")

    print("[6/6] Waiting for service health endpoints...")
    wait_for_http_ok("http://localhost:8081/api/v1/health")
    wait_for_http_ok("http://localhost:8084/actuator/health")
    wait_for_http_ok("http://localhost:8082/api/v1/health")
    wait_for_http_ok("http://localhost:8083/api/v1/health")
    wait_for_http_ok("http://localhost:8080/actuator/health")

    print("ChronoFlow startup complete.")
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
