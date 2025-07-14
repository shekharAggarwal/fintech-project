#!/bin/bash
set -e

echo "🐳 Building and starting all services and infra with Docker Compose..."
if [ "$1" == "infra" ]; then
  echo "Starting only infra containers (redis, grafana, postgres, rabbitmq, kafka)..."
  docker compose --env-file .env -f docker-compose.yml up -d redis grafana postgres rabbitmq kafka
else
  docker compose --env-file .env -f docker-compose.yml up --build -d
fi
