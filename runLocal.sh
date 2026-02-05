#!/bin/bash

set -euo pipefail

DATA_DIR="$HOME/observability"

START_OBSERVABILITY=false
PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MYSQL_COMPOSE_FILE="$PROJECT_ROOT/docker-compose-local.yml"

usage() {
  echo "Usage: $0 [-o]"
  echo "  -o   Start observability stack (Grafana/Prometheus/Tempo/Loki, OTEL on)"
  exit 1
}

start_mysql_if_needed() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker not found; skipping MySQL startup."
    return
  fi

  if [ ! -f "$MYSQL_COMPOSE_FILE" ]; then
    echo "No MySQL compose file found; skipping MySQL startup."
    return
  fi

  if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon not running; skipping MySQL startup."
    return
  fi

  local running
  running="$(docker compose -f "$MYSQL_COMPOSE_FILE" ps --status running -q mysql 2>/dev/null || true)"
  if [ -z "$running" ]; then
    echo "Starting MySQL via docker compose..."
    docker compose -f "$MYSQL_COMPOSE_FILE" up -d mysql
  else
    echo "MySQL already running."
  fi
}


while getopts "oc" opt; do
  case ${opt} in
    o ) START_OBSERVABILITY=true ;;
    * ) usage ;;
  esac
done


if [ "$START_OBSERVABILITY" = true ]; then
  echo "Starting observability stack..."
  mkdir -p "$DATA_DIR"/{grafana-data,prometheus-data,tempo-data,loki-data}
  export OBSERVABILITY_DATA="$DATA_DIR"

  cd "$PROJECT_ROOT/observability"
  docker compose up -d --force-recreate
  cd "$PROJECT_ROOT"


  export OTEL_SDK_DISABLED=false
  export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"
  export OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
  export OTEL_TRACES_EXPORTER=otlp
  export OTEL_LOGS_EXPORTER=none
  export OTEL_METRICS_EXPORTER=none
else
  echo " Skipping observability stack."
  export OTEL_SDK_DISABLED=true
fi

start_mysql_if_needed


APP_PROFILES="local"


echo "🚀 Starting app with profile: $APP_PROFILES"
./gradlew :bootRun --args="--spring.profiles.active=${APP_PROFILES}" \
  -Dspring.output.ansi.enabled=always
