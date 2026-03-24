#!/usr/bin/env bash
set -euo pipefail

IMAGE_TAG="jate-api:smoke"
API_CONTAINER_NAME="jate-api-smoke"
HOST_PORT="${JATE_SMOKE_HOST_PORT:-18000}"
BASE_URL="http://localhost:${HOST_PORT}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

resolve_path() {
  local path="$1"
  if [[ "$path" = /* ]]; then
    printf "%s\n" "$path"
  else
    printf "%s/%s\n" "$(cd "$(dirname "$path")" && pwd)" "$(basename "$path")"
  fi
}

DOCKERFILE_PATH="${1:-${REPO_ROOT}/Dockerfile}"
BUILD_CONTEXT="${2:-${REPO_ROOT}}"
DOCKERFILE_PATH="$(resolve_path "${DOCKERFILE_PATH}")"
BUILD_CONTEXT="$(resolve_path "${BUILD_CONTEXT}")"

if [[ ! -f "${DOCKERFILE_PATH}" ]]; then
  echo "Dockerfile not found: ${DOCKERFILE_PATH}" >&2
  echo "Usage: bash scripts/docker_smoke_test.sh [DOCKERFILE_PATH] [BUILD_CONTEXT]" >&2
  exit 1
fi

if [[ ! -d "${BUILD_CONTEXT}" ]]; then
  echo "Build context not found: ${BUILD_CONTEXT}" >&2
  echo "Usage: bash scripts/docker_smoke_test.sh [DOCKERFILE_PATH] [BUILD_CONTEXT]" >&2
  exit 1
fi

HTTP_BODY_FILE="/tmp/jate_smoke_body.$$"
CLI_OUTPUT_FILE="/tmp/jate_smoke_cli.$$"

cleanup() {
  docker rm -f "${API_CONTAINER_NAME}" >/dev/null 2>&1 || true
  rm -f "${HTTP_BODY_FILE}" "${CLI_OUTPUT_FILE}"
}
trap cleanup EXIT

echo "[1/6] Building Docker image: ${IMAGE_TAG}"
echo "      Dockerfile: ${DOCKERFILE_PATH}"
echo "      Context:    ${BUILD_CONTEXT}"
docker build -f "${DOCKERFILE_PATH}" -t "${IMAGE_TAG}" "${BUILD_CONTEXT}" >/dev/null

echo "[2/6] Running CLI smoke checks"
docker run --rm "${IMAGE_TAG}" \
  jate extract "local post office" --algorithm cvalue --top 20 --output json >"${CLI_OUTPUT_FILE}"

if ! grep -q '"term"' "${CLI_OUTPUT_FILE}"; then
  echo "CLI smoke check failed: expected JSON output with term field" >&2
  cat "${CLI_OUTPUT_FILE}" >&2
  exit 1
fi

echo "[3/6] Starting container in API mode"
docker run --rm -d \
  --name "${API_CONTAINER_NAME}" \
  -p "${HOST_PORT}:8000" \
  -e JATE_API_HOST=0.0.0.0 \
  -e JATE_API_PORT=8000 \
  -e JATE_API_WORKERS=1 \
  -e JATE_API_TIMEOUT_KEEP_ALIVE=5 \
  "${IMAGE_TAG}" \
  jate-api >/dev/null

echo "[4/6] Waiting for service to become live"
for _ in $(seq 1 30); do
  if curl -s "${BASE_URL}/health/live" | grep -q '"status":"ok"'; then
    break
  fi
  sleep 1
done

assert_status() {
  local expected="$1"
  local method="$2"
  local url="$3"
  local data="${4:-}"

  local response
  if [[ -n "$data" ]]; then
    response=$(curl -s -o "${HTTP_BODY_FILE}" -w "%{http_code}" -X "$method" "$url" -H "Content-Type: application/json" -d "$data")
  else
    response=$(curl -s -o "${HTTP_BODY_FILE}" -w "%{http_code}" -X "$method" "$url")
  fi

  if [[ "$response" != "$expected" ]]; then
    echo "Expected status ${expected} for ${method} ${url} but got ${response}" >&2
    echo "Response body:" >&2
    cat "${HTTP_BODY_FILE}" >&2
    exit 1
  fi
}

echo "[5/6] Validating API endpoints and error paths"
assert_status 200 GET "${BASE_URL}/health/live"
assert_status 200 GET "${BASE_URL}/health/ready"
assert_status 503 GET "${BASE_URL}/health/ready?model=does_not_exist"
assert_status 200 GET "${BASE_URL}/jate/api/v1/capabilities"
assert_status 200 POST "${BASE_URL}/jate/api/v1/extract" '{"text":"Hello world from JATE API","algorithm":"cvalue","top":5}'
assert_status 400 POST "${BASE_URL}/jate/api/v1/extract" '{"text":"hello","algorithm":"not_real_algo"}'
assert_status 400 POST "${BASE_URL}/jate/api/v1/extract" '{"text":"hello","extractor":"not_real_extractor"}'
assert_status 422 POST "${BASE_URL}/jate/api/v1/extract" '{"text":""}'

echo "[6/6] Smoke checks passed (CLI and API verified)"
