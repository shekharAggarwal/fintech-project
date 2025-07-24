#!/bin/bash
set -e

# Define infrastructure services
INFRA_SERVICES="fintech_redis grafana prometheus jaeger splunk zookeeper kafka rabbitmq"

# Define database services with read replicas and ShardingSphere-Proxy
DB_SERVICES="postgres-main postgres-auth postgres-scheduler postgres-retry"

# Define application services for ordered startup
CONFIG_SERVICE="config-server"
CORE_SERVICES="auth-service user-service payment-service transaction-service"
SUPPORT_SERVICES="notification-service authorization-service reporting-service scheduler-service retry-service"
GATEWAY_SERVICE="gateway-service"

echo "🐳 Building and starting FinTech microservices with ShardingSphere-Proxy..."

if [ "$1" == "infra" ]; then
  echo "🔧 Starting only infrastructure containers..."
  docker compose --env-file .env -f docker-compose.yml up -d $INFRA_SERVICES $DB_SERVICES
  echo "✅ Infrastructure services started."

else
  # Full startup sequence
  echo "🗄️  Step 1: Starting PostgreSQL databases..."
  docker compose --env-file .env -f docker-compose.yml up -d $DB_SERVICES
  
  echo "⏳ Waiting for master databases to initialize..."
  sleep 30

  
  echo "🔧 Step 4: Starting infrastructure services..."
  docker compose --env-file .env -f docker-compose.yml up -d $INFRA_SERVICES
  
  echo "⏳ Waiting for infrastructure to be ready..."
  sleep 20
  
  echo "⚙️  Step 5: Starting config server..."
  docker compose --env-file .env -f docker-compose.yml up -d $CONFIG_SERVICE
  
  echo "⏳ Waiting for config server to be ready..."
  sleep 30
  
  echo "🚀 Step 6: Starting core microservices..."
  docker compose --env-file .env -f docker-compose.yml up --build -d $CORE_SERVICES
  
  echo "⏳ Waiting for core services to be ready..."
  sleep 20
  
  echo "🛠️  Step 7: Starting support services..."
  docker compose --env-file .env -f docker-compose.yml up --build -d $SUPPORT_SERVICES
  
  echo "⏳ Waiting for support services to be ready..."
  sleep 15
  
  echo "🌐 Step 8: Starting gateway service..."
  docker compose --env-file .env -f docker-compose.yml up --build -d $GATEWAY_SERVICE
  
  echo "✅ All services are starting up."
fi