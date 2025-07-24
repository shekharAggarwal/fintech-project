#!/bin/bash
set -e

# Service groups for Patroni-based setup (Single ETCD)
ETCD_SERVICES="etcd"
PATRONI_SERVICES="patroni-main-master patroni-main-replica patroni-shard-1-master patroni-shard-1-replica patroni-shard-2-master patroni-shard-2-replica patroni-shard-3-master patroni-shard-3-replica"
INFRA_SERVICES="fintech_redis grafana prometheus jaeger splunk zookeeper kafka rabbitmq dbeaver"
DB_SPECIALIZED="postgres-auth postgres-scheduler postgres-retry"
SHARDING_PROXY="shardingsphere-proxy"
CONFIG_SERVICE="config-server"
CORE_SERVICES="auth-service user-service payment-service transaction-service"
SUPPORT_SERVICES="notification-service authorization-service reporting-service scheduler-service retry-service"
GATEWAY_SERVICE="gateway-service"


if [ "$1" == "infra-only" ]; then
  docker compose --env-file .env -f docker-compose.yml up -d $ETCD_SERVICES $PATRONI_SERVICES $INFRA_SERVICES $DB_SPECIALIZED $SHARDING_PROXY
else
  # Full startup - let Docker Compose handle all dependencies

  docker compose --env-file .env -f docker-compose.yml up --build -d

fi