#!/bin/bash
echo "🔁 Starting local infra..."
docker compose --env-file .env -f docker-compose.yml up -d
