#!/bin/bash

# observability services data mount dir

DATA_DIR="$HOME/observability"


# Parse flags
START_OBSERVABILITY=false
while getopts "o" opt; do
  case ${opt} in
    o )
      START_OBSERVABILITY=true
      ;;
    \? )
      echo "Usage: $0 [-o]"
      exit 1
      ;;
  esac
done


if [ "$START_OBSERVABILITY" = true ]; then
  echo "📊 Starting observability stack..."
  mkdir -p "$DATA_DIR/grafana-data"
  mkdir -p "$DATA_DIR/prometheus-data"
  mkdir -p "$DATA_DIR/tempo-data"
  mkdir -p "$DATA_DIR/loki-data"
  export OBSERVABILITY_DATA="$DATA_DIR"

  PROJECT_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
  cd "$PROJECT_ROOT/observability"

  docker compose up -d --force-recreate

  cd "$PROJECT_ROOT"
else
  echo "✅ Skipping observability stack."
fi

# Gradle bootRun
echo "🚀 Starting app..."
  ./gradlew :bootRun --args='--spring.profiles.active=local'
