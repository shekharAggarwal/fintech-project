#!/bin/bash
set -e

echo "🐳 Starting FinTech microservices platform..."

if [ "$1" == "infra" ]; then
  echo "🔧 Starting only infrastructure services..."
  docker compose --env-file .env up -d \
    prometheus grafana jaeger splunk \
    zookeeper kafka rabbitmq fintech_redis \
    postgres-main postgres-auth postgres-scheduler postgres-retry \
    postgres-exporter-main postgres-exporter-auth postgres-exporter-scheduler postgres-exporter-retry \
    redis-exporter
  echo "✅ Infrastructure services started successfully."
  
elif [ "$1" == "build" ]; then
  echo "� Building and starting all services..."
  docker compose --env-file .env up --build -d
  echo "✅ All services built and started successfully."
  
else
  echo "� Starting all services (using existing images)..."
  docker compose --env-file .env up -d
  echo "✅ All services started successfully."
fi

echo ""
echo "� Service Status:"
echo "🌐 Gateway Service: http://localhost:8080"
echo "📈 Prometheus: http://localhost:9090"
echo "📊 Grafana: http://localhost:3000"
echo "🔍 Jaeger: http://localhost:16686"
echo "📋 Splunk: http://localhost:8000"
echo "🐰 RabbitMQ Management: http://localhost:15672"
echo ""
echo "💡 Usage:"
echo "  ./start.sh          - Start all services"
echo "  ./start.sh build    - Build and start all services"
echo "  ./start.sh infra    - Start only infrastructure services"
echo ""
echo "🔍 To check service status: docker compose ps"
echo "📋 To view logs: docker compose logs [service-name]"