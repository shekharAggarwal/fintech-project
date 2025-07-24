@echo off
REM FinTech Microservices Startup Script (Windows)
REM Uses Docker Compose dependency management instead of sleep timers

echo 🐳 Starting FinTech microservices with Patroni HA and Docker Compose dependency management...

if "%1"=="infra-only" goto infra_only
if "%1"=="help" goto help
if "%1"=="" goto full_start
goto help

:infra_only
echo 🏗️  Starting infrastructure services only...
echo    Docker Compose will handle all dependencies and health checks automatically
docker compose --env-file .env -f docker-compose.yml up -d etcd patroni-main-master patroni-main-replica patroni-shard-1-master patroni-shard-1-replica patroni-shard-2-master patroni-shard-2-replica patroni-shard-3-master patroni-shard-3-replica fintech_redis grafana prometheus jaeger splunk zookeeper kafka rabbitmq dbeaver postgres-auth postgres-scheduler postgres-retry shardingsphere-proxy
echo ✅ Infrastructure services started. Check logs with: docker compose logs -f
goto end

:full_start
echo 🚀 Starting all FinTech services with intelligent dependency management...
echo.
echo    Docker Compose will automatically:
echo    📊 Start ETCD first and wait for health check
echo    🗄️  Start Patroni clusters after ETCD is healthy
echo    🔀 Start ShardingSphere after all Patroni clusters are healthy
echo    🔧 Start infrastructure services (Redis, Kafka, RabbitMQ, etc.)
echo    ⚙️  Start config server and wait for health check
echo    🚀 Start all microservices based on their dependencies
echo.

docker compose --env-file .env -f docker-compose.yml up --build -d

echo.
echo ✅ All services are starting with proper dependency management!
echo.
echo 🔍 Patroni Cluster Status:
echo   ETCD Endpoint:  http://localhost:2379
echo   Main Cluster:   http://localhost:8008 (master), http://localhost:8009 (replica)
echo   Shard 1:        http://localhost:8010 (master), http://localhost:8011 (replica)
echo   Shard 2:        http://localhost:8012 (master), http://localhost:8013 (replica)
echo   Shard 3:        http://localhost:8014 (master), http://localhost:8015 (replica)
echo.
echo 🌐 Key Services:
echo   Config Server:  http://localhost:8888
echo   Gateway:        http://localhost:8080
echo   Grafana:        http://localhost:3000 (admin/admin)
echo   Prometheus:     http://localhost:9090
echo   Jaeger:         http://localhost:16686
echo   DBeaver:        http://localhost:8978
echo   Splunk:         http://localhost:8000
echo   RabbitMQ:       http://localhost:15672
echo.
echo 🗄️ Database Endpoints:
echo   ShardingSphere: localhost:3307
echo   Patroni Main:   localhost:5432 (master), localhost:5442 (replica)
echo   Patroni Shard1: localhost:5433 (master), localhost:5443 (replica)
echo   Patroni Shard2: localhost:5434 (master), localhost:5444 (replica)
echo   Patroni Shard3: localhost:5435 (master), localhost:5445 (replica)
echo   Auth DB:        localhost:5436
echo   Scheduler DB:   localhost:5437
echo   Retry DB:       localhost:5438
echo   Redis Cache:    localhost:6379
echo.
echo 📊 Management ^& Monitoring:
echo   Status Check:   patroni-manage-single.bat status
echo   Container Logs: docker compose logs -f [service-name]
echo   All Logs:       docker compose logs -f
goto tips

:help
echo FinTech Microservices Startup Script
echo ====================================
echo.
echo Usage: %0 [command]
echo.
echo Commands:
echo   (no args)   - Start all services with dependency management
echo   infra-only  - Start only infrastructure services
echo   help        - Show this help
goto end

:tips
echo.
echo 🎯 Pro Tips:
echo   • No sleep timers needed - Docker Compose handles all timing!
echo   • Health checks ensure services start only when dependencies are ready
echo   • Use 'docker compose ps' to check service status
echo   • Use 'docker compose logs -f' to follow all logs

:end
