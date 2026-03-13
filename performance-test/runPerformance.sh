#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

PERF_USERNAME="${PERF_USERNAME:-${1:-performanceUser}}"
PERF_PASSWORD="${PERF_PASSWORD:-${2:-performanceUser}}"
SIMULATION_CLASS="${SIMULATION_CLASS:-${3:-com.worktrack.performance.AuthAndProjectFlowSimulation}}"
BASE_URL="${BASE_URL:-http://localhost:8000}"
P95_LIMIT_MS="${P95_LIMIT_MS:-1000}"
START_USERS_PER_SEC="${START_USERS_PER_SEC:-5}"
USERS_PER_SEC_INCREMENT="${USERS_PER_SEC_INCREMENT:-5}"
LEVELS="${LEVELS:-8}"
LEVEL_DURATION_SEC="${LEVEL_DURATION_SEC:-30}"
RAMP_BETWEEN_LEVELS_SEC="${RAMP_BETWEEN_LEVELS_SEC:-10}"
MAX_FAILED_PERCENT="${MAX_FAILED_PERCENT:-1.0}"
PROJECT_PAGE_SIZE="${PROJECT_PAGE_SIZE:-20}"
BUSINESS_CALLS_PER_LOGIN="${BUSINESS_CALLS_PER_LOGIN:-5}"
MIN_THINK_TIME_MS="${MIN_THINK_TIME_MS:-50}"
MAX_THINK_TIME_MS="${MAX_THINK_TIME_MS:-250}"

"${REPO_ROOT}/gradlew" :performance-test:gatlingRun \
  -PsimulationClass="${SIMULATION_CLASS}" \
  -DbaseUrl="${BASE_URL}" \
  -Dperf.username="${PERF_USERNAME}" \
  -Dperf.password="${PERF_PASSWORD}" \
  -Dp95LimitMs="${P95_LIMIT_MS}" \
  -DstartUsersPerSec="${START_USERS_PER_SEC}" \
  -DusersPerSecIncrement="${USERS_PER_SEC_INCREMENT}" \
  -Dlevels="${LEVELS}" \
  -DlevelDurationSec="${LEVEL_DURATION_SEC}" \
  -DrampBetweenLevelsSec="${RAMP_BETWEEN_LEVELS_SEC}" \
  -DmaxFailedPercent="${MAX_FAILED_PERCENT}" \
  -DprojectPageSize="${PROJECT_PAGE_SIZE}" \
  -DbusinessCallsPerLogin="${BUSINESS_CALLS_PER_LOGIN}" \
  -DminThinkTimeMs="${MIN_THINK_TIME_MS}" \
  -DmaxThinkTimeMs="${MAX_THINK_TIME_MS}"
