# Database Scaling Guide

## Current Architecture

The platform runs **4 dedicated PostgreSQL instances** via Docker Compose:

| Container | Database | Purpose |
|-----------|----------|---------|
| `postgres-main` | `fintech_main` | Core transactional data (accounts, payments, ledger) |
| `postgres-auth` | `fintech_auth` | Authentication & user credentials |
| `postgres-scheduler` | `fintech_scheduler` | Scheduled jobs & cron state |
| `postgres-retry` | `fintech_retry` | Retry queue & dead-letter storage |

Each instance has a dedicated Prometheus exporter for monitoring (`postgres-exporter-*`).

---

## When to Introduce Sharding

Consider horizontal sharding when **any** of these thresholds are reached:

| Metric | Threshold | Measurement |
|--------|-----------|-------------|
| Table row count | > 100M rows in a single table | `SELECT reltuples FROM pg_class` |
| Database size | > 500 GB per instance | `SELECT pg_database_size(...)` |
| Write throughput | > 10K TPS sustained on one instance | Prometheus `pg_stat_activity` |
| Query latency p99 | > 200ms after index optimization | APM / slow query log |

**Rule of thumb**: If vertical scaling (bigger instance) can still solve the problem for < 2× cost, prefer it over sharding complexity.

---

## How to Add Shards

### 1. Add Docker containers

Add shard services to `docker-compose.yml`:

```yaml
postgres-shard-1:
  image: postgres:16-alpine
  container_name: fintech_postgres_shard_1
  environment:
    POSTGRES_DB: fintech_shard1
    POSTGRES_USER: ${POSTGRES_USER}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  volumes:
    - postgres-shard-1-data:/var/lib/postgresql/data
  networks:
    - fintech-network
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
    interval: 10s
    timeout: 5s
    retries: 5
```

Repeat for `postgres-shard-2`, `postgres-shard-3`, etc.

### 2. Uncomment volumes

In the `volumes:` section of `docker-compose.yml`, uncomment the corresponding shard volume definitions.

### 3. Uncomment .env URLs

In `infra/.env`, uncomment the `SHARD*_DB_URL` variables.

### 4. Add ShardingSphere Proxy

```yaml
shardingsphere-proxy:
  image: apache/shardingsphere-proxy:5.5.0
  container_name: fintech_shardingsphere_proxy
  ports:
    - "3307:3307"
  volumes:
    - ./shardingsphere/server.yaml:/opt/shardingsphere-proxy/conf/server.yaml
    - ./shardingsphere/config-sharding.yaml:/opt/shardingsphere-proxy/conf/config-sharding.yaml
  depends_on:
    - postgres-main
    - postgres-shard-1
    - postgres-shard-2
    - postgres-shard-3
  networks:
    - fintech-network
```

Uncomment `SHARDINGSPHERE_PROXY_URL` in `.env`.

### 5. Configure shard routing

Create `infra/shardingsphere/config-sharding.yaml` with your sharding key strategy (e.g., `user_id % shard_count`).

---

## How to Add Read Replicas

### 1. Add replica containers

```yaml
postgres-main-replica:
  image: postgres:16-alpine
  container_name: fintech_postgres_main_replica
  environment:
    POSTGRES_USER: ${POSTGRES_USER}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  volumes:
    - postgres-main-replica-data:/var/lib/postgresql/data
    - ./postgres/replica-setup.sh:/docker-entrypoint-initdb.d/replica-setup.sh
  depends_on:
    - postgres-main
  networks:
    - fintech-network
```

### 2. Configure streaming replication

On the **primary** (`postgres-main`), ensure `postgresql.conf` includes:

```
wal_level = replica
max_wal_senders = 5
wal_keep_size = 512MB
```

On the **replica**, configure `recovery.conf` / `standby.signal`:

```
primary_conninfo = 'host=postgres-main port=5432 user=replicator password=...'
```

### 3. Update application routing

Uncomment `MAIN_REPLICA_DB_URL` in `.env` and configure read/write splitting in the application datasource (or via ShardingSphere read-write splitting rules).

---

## Pre-configured .env Variables

The following variables in `infra/.env` are commented out and ready for activation:

```
SHARD1_DB_URL, SHARD2_DB_URL, SHARD3_DB_URL
MAIN_REPLICA_DB_URL, SHARD1_REPLICA_DB_URL, SHARD2_REPLICA_DB_URL, SHARD3_REPLICA_DB_URL
SHARDINGSPHERE_PROXY_HOST, SHARDINGSPHERE_PROXY_PORT, SHARDINGSPHERE_PROXY_URL
```

Corresponding Docker volume definitions are also commented out in `docker-compose.yml`.
