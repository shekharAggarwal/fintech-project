#!/bin/bash
set -e

# Define infrastructure services
# Note: All postgres instances are listed to support the sharded architecture
INFRA_SERVICES="fintech_redis grafana prometheus jaeger splunk zookeeper kafka rabbitmq"
DB_SERVICES="postgres-main postgres-shard-1 postgres-shard-2 postgres-shard-3 postgres-auth postgres-scheduler postgres-retry"

echo "🐳 Building and starting all services and infra with Docker Compose..."

if [ "$1" == "infra" ]; then
  echo "Starting only infra containers..."
  docker compose --env-file .env -f docker-compose.yml up -d $INFRA_SERVICES $DB_SERVICES
else
  # Start infrastructure first
  echo "🔧 Setting up infrastructure ($INFRA_SERVICES $DB_SERVICES)..."
  docker compose --env-file .env -f docker-compose.yml up -d $INFRA_SERVICES $DB_SERVICES

  # Wait for infrastructure
  echo "⏳ Waiting for infrastructure to be ready..."
  sleep 20

  # Start config server
  echo "🔧 Starting config server..."
  docker compose --env-file .env -f docker-compose.yml up -d config-server

  # Wait for config server
  echo "⏳ Waiting for config server to be ready..."
  sleep 15

  # Start all application services
  echo "🚀 Starting all application services..."
  # The 'up' command will also start any dependencies that aren't running,
  # but we start them explicitly above for clarity and control.
  # The --build flag ensures services are rebuilt if their source code has changed.
  docker compose --env-file .env -f docker-compose.yml up --build -d
fi

echo "✅ All services are starting up."
