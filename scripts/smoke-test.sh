#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

check_status() {
  local expected="$1"
  local url="$2"
  local actual
  actual="$(curl --silent --show-error --output /tmp/prestamo-smoke-response --write-out '%{http_code}' "$url")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Smoke test failed: $url returned $actual (expected $expected)" >&2
    cat /tmp/prestamo-smoke-response >&2
    exit 1
  fi
}

check_status 200 "$BASE_URL/actuator/health"
check_status 200 "$BASE_URL/actuator/health/readiness"
check_status 200 "$BASE_URL/api/v1/disponibilidad"
check_status 401 "$BASE_URL/api/v1/prestamos"

if [[ -n "${SMOKE_IDENTIFIER:-}" || -n "${SMOKE_PASSWORD:-}" ]]; then
  : "${SMOKE_IDENTIFIER:?Defina también SMOKE_IDENTIFIER}"
  : "${SMOKE_PASSWORD:?Defina también SMOKE_PASSWORD}"
  login_status="$(curl --silent --show-error --output /tmp/prestamo-smoke-login \
    --write-out '%{http_code}' -H 'Content-Type: application/json' \
    --data "{\"identificador\":\"$SMOKE_IDENTIFIER\",\"contrasena\":\"$SMOKE_PASSWORD\"}" \
    "$BASE_URL/api/v1/auth/login")"
  if [[ "$login_status" != 200 ]]; then
    echo "Smoke login failed with HTTP $login_status" >&2
    cat /tmp/prestamo-smoke-login >&2
    exit 1
  fi
fi

echo "Smoke test passed against $BASE_URL"
