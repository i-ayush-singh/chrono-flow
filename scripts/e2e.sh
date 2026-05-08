#!/usr/bin/env bash

set -euo pipefail

JOB_SERVICE_URL="${JOB_SERVICE_URL:-http://localhost:8081}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TENANT_NAME="${TENANT_NAME:-demo-tenant-$(date +%s)}"
JOB_NAME="${JOB_NAME:-demo-job}"
CRON_EXPR="${CRON_EXPR:-*/2 * * * *}"
TARGET_URL="${TARGET_URL:-https://httpbin.org/post}"

require_bin() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required binary: $1"
    exit 1
  fi
}

require_bin curl
require_bin python3

echo "== ChronoFlow E2E =="
echo "Job service: $JOB_SERVICE_URL"
echo "Gateway:     $GATEWAY_URL"
echo

echo "1) Creating tenant in job-service..."
tenant_response="$(curl -sS -X POST "$JOB_SERVICE_URL/api/v1/tenants" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$TENANT_NAME\"}")"

tenant_id="$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["id"])' <<<"$tenant_response")"
echo "Tenant ID: $tenant_id"

echo "2) Creating API key in job-service..."
key_response="$(curl -sS -X POST "$JOB_SERVICE_URL/api/v1/tenants/$tenant_id/api-keys")"
key_id="$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["keyId"])' <<<"$key_response")"
key_secret="$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["keySecret"])' <<<"$key_response")"
api_credential="${key_id}:${key_secret}"
echo "API key created: $key_id"

echo "3) Creating job via gateway (authenticated + rate limited path)..."
job_payload="$(cat <<EOF
{"tenantId":"$tenant_id","name":"$JOB_NAME","cronExpression":"$CRON_EXPR","targetUrl":"$TARGET_URL"}
EOF
)"

job_response="$(curl -sS -X POST "$GATEWAY_URL/api/v1/jobs" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $api_credential" \
  -d "$job_payload")"
job_id="$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["id"])' <<<"$job_response")"
echo "Job ID: $job_id"

echo "4) Listing jobs via gateway..."
list_response="$(curl -sS -X GET "$GATEWAY_URL/api/v1/jobs?tenantId=$tenant_id" \
  -H "X-API-Key: $api_credential")"
job_count="$(python3 -c 'import json,sys; print(len(json.loads(sys.stdin.read())))' <<<"$list_response")"
echo "Jobs returned: $job_count"

echo
echo "E2E done."
echo "Trace verification:"
echo "- Open Jaeger: http://localhost:16686"
echo "- Search service: chrono-api-gateway"
echo "- You should see traces flowing into chrono-job-service"
echo
echo "Useful exported value for manual calls:"
echo "X-API-Key: $api_credential"
