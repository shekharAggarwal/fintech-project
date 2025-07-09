#!/bin/bash
echo "⛔ Stopping local infra..."
docker compose --env-file .env -f docker-compose.yml down
