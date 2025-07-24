#!/bin/bash
set -e
if [ "$1" == "clean" ]; then
  echo "⛔ Stopping all services and infra..."
  docker compose --env-file .env -f docker-compose.yml down --remove-orphans --volumes
else
  echo "⛔ Stopping all services and infra without delete volume..."
  docker compose --env-file .env -f docker-compose.yml down
fi