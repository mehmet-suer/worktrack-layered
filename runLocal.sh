#!/bin/bash

set -euo pipefail

DATA_DIR="$HOME/observability"

START_OBSERVABILITY=false
USE_LOCAL_CONFIG=false

usage() {
  echo "Usage: $0 [-o]"
  echo "  -o   Start observability stack (Grafana/Prometheus/Tempo/Loki, OTEL on)"
  exit 1
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

  PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
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



APP_PROFILES="local"


echo "🚀 Starting app with profile: $APP_PROFILES"
./gradlew :bootRun --args="--spring.profiles.active=${APP_PROFILES}" \
  -Dspring.output.ansi.enabled=always