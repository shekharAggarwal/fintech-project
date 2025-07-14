# Fintech Microservices Project

## Overview
This project is a Spring Boot microservices architecture for a payment gateway system, using Docker Compose for local development and ready for Kubernetes in production.

## Project Structure
- `services/` — All Java microservices (Spring Boot)
- `infra/` — Docker Compose, scripts, and infra config
- `config-repo/` — Centralized configuration for all services
- `certs/` — SSL certificates

## Prerequisites
- Docker & Docker Compose
- Bash (for `start.sh`/`stop.sh` scripts)

## How to Start All Services
From the `infra` directory:
```bash
./start.sh
```
This will build and start all services and infrastructure using Docker Compose. Multi-stage Dockerfiles will build the Java apps inside the containers.

## How to Stop All Services
From the `infra` directory:
```bash
./stop.sh
```
This will stop and remove all containers, orphans, and volumes.

## How to Update Only One Service
From the `infra` directory:
```bash
# Replace <service-name> with the actual service, e.g., gateway-service

docker compose build <service-name>
docker compose up -d <service-name>
```

## How to View Logs
- **All services (live, combined):**
  ```bash
  docker compose logs -f
  ```
- **Single service (e.g., gateway-service):**
  ```bash
  docker compose logs -f gateway-service
  ```
- **Multiple services:**
  ```bash
  docker compose logs -f gateway-service auth-service
  ```
- **Last N lines:**
  ```bash
  docker compose logs --tail=100 gateway-service
  ```

Press `Ctrl+C` to stop following logs (containers keep running).