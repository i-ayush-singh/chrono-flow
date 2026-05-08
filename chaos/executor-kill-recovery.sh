#!/usr/bin/env bash

set -euo pipefail

NAMESPACE="${NAMESPACE:-chronoflow}"
SELECTOR="${SELECTOR:-app=chrono-executor-service}"
SLEEP_SECONDS="${SLEEP_SECONDS:-20}"

require_bin() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1"
    exit 1
  fi
}

require_bin kubectl

echo "Finding executor pod in namespace '${NAMESPACE}'..."
POD_NAME="$(kubectl get pods -n "${NAMESPACE}" -l "${SELECTOR}" -o jsonpath='{.items[0].metadata.name}')"

if [ -z "${POD_NAME}" ]; then
  echo "No executor pod found for selector '${SELECTOR}' in namespace '${NAMESPACE}'."
  exit 1
fi

echo "Deleting pod: ${POD_NAME}"
kubectl delete pod "${POD_NAME}" -n "${NAMESPACE}" --grace-period=0 --force

echo "Waiting ${SLEEP_SECONDS}s for replacement pod scheduling..."
sleep "${SLEEP_SECONDS}"

echo "Current executor pods:"
kubectl get pods -n "${NAMESPACE}" -l "${SELECTOR}"

echo
echo "Recovery check guidance:"
echo "1) Confirm new executor pod is Running and Ready."
echo "2) Check DLQ and retry metrics in Grafana/Prometheus."
echo "3) Verify pending retries are consumed after restart."
