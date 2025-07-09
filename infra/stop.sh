#!/bin/bash
echo "⛔ Stopping all services and infra..."
docker compose --env-file .env -f docker-compose.yml down --remove-orphans --volumes
