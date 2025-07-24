#!/bin/bash
set -e

echo "🚀 Starting Fintech Microservices Platform with Patroni High Availability"
echo "=========================================================================="

# Service groups with single etcd
ETCD_SERVICE="etcd"
PATRONI_SERVICES="patroni-main-master patroni-main-replica patroni-shard-1-master patroni-shard-1-replica patroni-shard-2-master patroni-shard-2-replica patroni-shard-3-master patroni-shard-3-replica"
INFRA_SERVICES="prometheus grafana jaeger splunk zookeeper kafka rabbitmq fintech_redis"
DB_SPECIALIZED="postgres-auth postgres-scheduler postgres-retry"
CONFIG_SERVICE="config-server"
CORE_SERVICES="auth-service authorization-service user-service payment-service transaction-service"
SUPPORT_SERVICES="notification-service reporting-service scheduler-service retry-service"
GATEWAY_SERVICE="gateway-service"

if [ "$1" == "infra-only" ]; then
  echo "🏗️  Starting infrastructure services only..."
  
  echo "📊 Step 1: Starting single ETCD cluster..."
  docker compose --env-file .env -f docker-compose.yml up -d $ETCD_SERVICE
  echo "⏳ Waiting for ETCD to be ready..."
  sleep 15
  
  echo "🗄️  Step 2: Starting Patroni PostgreSQL clusters..."
  docker compose --env-file .env -f docker-compose.yml up -d $PATRONI_SERVICES
  echo "⏳ Waiting for Patroni clusters to form..."
  sleep 60
  
  echo "🔀 Step 3: Starting specialized databases and infrastructure..."
  docker compose --env-file .env -f docker-compose.yml up -d $INFRA_SERVICES $DB_SPECIALIZED shardingsphere-proxy
  echo "✅ Infrastructure services started."
  
else
  # Full startup sequence
  echo "📊 Step 1: Starting single ETCD cluster..."
  docker compose --env-file .env -f docker-compose.yml up -d $ETCD_SERVICE
  echo "⏳ Waiting for ETCD to initialize..."
  sleep 20

  echo "🗄️  Step 2: Starting Patroni PostgreSQL clusters..."
  docker compose --env-file .env -f docker-compose.yml up -d $PATRONI_SERVICES
  echo "⏳ Waiting for Patroni clusters to form and elect leaders..."
  sleep 75

  echo "🔀 Step 3: Starting specialized databases..."
  docker compose --env-file .env -f docker-compose.yml up -d $DB_SPECIALIZED
  echo "⏳ Waiting for specialized DBs..."
  sleep 25

  echo "🔀 Step 4: Starting ShardingSphere-Proxy..."
  docker compose --env-file .env -f docker-compose.yml up -d shardingsphere-proxy
  echo "⏳ Waiting for proxy to be ready..."
  sleep 25

  echo "🔧 Step 5: Starting infrastructure services..."
  docker compose --env-file .env -f docker-compose.yml up -d $INFRA_SERVICES
  echo "⏳ Waiting for infrastructure to be ready..."
  sleep 30

  echo "⚙️  Step 6: Starting config server..."
  docker compose --env-file .env -f docker-compose.yml up -d $CONFIG_SERVICE
  echo "⏳ Waiting for config server to be ready..."
  sleep 35

  echo "🚀 Step 7: Starting core microservices..."
  docker compose --env-file .env -f docker-compose.yml up --build -d $CORE_SERVICES
  echo "⏳ Waiting for core services to be ready..."
  sleep 30

  echo "🛠️  Step 8: Starting support services..."
  docker compose --env-file .env -f docker-compose.yml up --build -d $SUPPORT_SERVICES
  echo "⏳ Waiting for support services to be ready..."
  sleep 20

  echo "🌐 Step 9: Starting gateway service..."
  docker compose --env-file .env -f docker-compose.yml up --build -d $GATEWAY_SERVICE
  
  echo ""
  echo "✅ All services are starting up with Patroni HA!"
  echo ""
  echo "🔍 Key Services:"
  echo "  • ETCD Cluster: http://localhost:2379"
  echo "  • Main DB Master: localhost:5432"
  echo "  • Shard 1 Master: localhost:5433"
  echo "  • Shard 2 Master: localhost:5434"  
  echo "  • Shard 3 Master: localhost:5435"
  echo "  • Main DB Replica: localhost:5442"
  echo "  • ShardingSphere: localhost:3307"
  echo "  • Gateway: http://localhost:${GATEWAY_SERVICE_PORT:-8080}"
  echo "  • Grafana: http://localhost:3000 (admin/admin)"
  echo "  • Prometheus: http://localhost:9090"
  echo "  • Jaeger: http://localhost:16686"
  echo ""
  echo "📋 Patroni Management Endpoints:"
  echo "  • Main Master: http://localhost:8008"
  echo "  • Main Replica: http://localhost:8009"  
  echo "  • Shard 1 Master: http://localhost:8010"
  echo "  • Shard 1 Replica: http://localhost:8011"
  echo "  • Shard 2 Master: http://localhost:8012"
  echo "  • Shard 2 Replica: http://localhost:8013"
  echo "  • Shard 3 Master: http://localhost:8014"
  echo "  • Shard 3 Replica: http://localhost:8015"
fi

echo ""
echo "🎯 Use './patroni-manage.sh status' to check cluster health"
echo "🎯 Use 'docker compose logs -f [service-name]' to view logs"
echo "🎯 Use './stop.sh' to stop all services"
