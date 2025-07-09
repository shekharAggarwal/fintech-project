#!/bin/bash
set -e

echo "🐳 Building and starting all services and infra with Docker Compose..."
docker compose --env-file .env -f docker-compose.yml up --build -d
