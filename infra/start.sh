#!/bin/bash
set -e

echo "🐳 Building and starting all services and infra with Docker Compose..."
echo "🔧 Setting up observability infrastructure (Jaeger, Splunk, Grafana, Prometheus)..."

if [ "$1" == "infra" ]; then
  echo "Starting only infra containers (redis, grafana, prometheus, postgres, rabbitmq, kafka, jaeger, splunk)..."
  docker compose --env-file .env -f docker-compose.yml up -d redis grafana prometheus postgres rabbitmq kafka jaeger splunk zookeeper
else
  echo "🚀 Starting all services with enhanced observability..."
  echo "  - Jaeger tracing on http://localhost:16686"
  echo "  - Grafana dashboards on http://localhost:3000 (admin/admin)"
  echo "  - Splunk logging on http://localhost:8000 (admin/admin123)"
  echo "  - Prometheus metrics on http://localhost:9090"
  
  # Start infrastructure first
  docker compose --env-file .env -f docker-compose.yml up -d redis grafana prometheus postgres rabbitmq kafka jaeger splunk zookeeper
  
  # Wait for infrastructure
  echo "⏳ Waiting for infrastructure to be ready..."
  sleep 20
  
  # Start config server
  echo "🔧 Starting config server..."
  docker compose --env-file .env -f docker-compose.yml up -d config-server
  
  # Wait for config server
  echo "⏳ Waiting for config server..."
  sleep 15
  
  # Start all application services
  echo "🚀 Starting all application services..."
  docker compose --env-file .env -f docker-compose.yml up --build -d
  
  echo ""
  echo "✅ All services started with observability features:"
  echo "  📊 Grafana Dashboard: http://localhost:3000 (admin/admin)"
  echo "  🔍 Jaeger Tracing: http://localhost:16686"
  echo "  📝 Splunk Logging: http://localhost:8000 (admin/admin123)"
  echo "  📈 Prometheus Metrics: http://localhost:9090"
  echo "  🌐 Gateway Service: https://localhost:8081"
  echo ""
  echo "🔧 Circuit breakers, distributed tracing, and centralized logging are now active!"
fi
