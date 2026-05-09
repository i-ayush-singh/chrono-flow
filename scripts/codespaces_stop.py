#!/usr/bin/env python3

import pathlib
import subprocess
import sys


ROOT_DIR = pathlib.Path(__file__).resolve().parent.parent


def run_allow_fail(cmd: list[str]) -> None:
    subprocess.run(cmd, cwd=ROOT_DIR, check=False)


def main() -> int:
    print("Stopping Spring Boot processes...")
    run_allow_fail(["pkill", "-f", "chrono-job-service.*spring-boot:run"])
    run_allow_fail(["pkill", "-f", "chrono-auth-service.*spring-boot:run"])
    run_allow_fail(["pkill", "-f", "chrono-scheduler-service.*spring-boot:run"])
    run_allow_fail(["pkill", "-f", "chrono-executor-service.*spring-boot:run"])
    run_allow_fail(["pkill", "-f", "chrono-api-gateway.*spring-boot:run"])

    print("Stopping infra containers...")
    run_allow_fail(["docker", "compose", "-f", "infra/docker/docker-compose.yml", "down"])

    print("ChronoFlow stopped.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
